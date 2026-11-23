package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.Ordine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di {@link OrdineDAO} su un database H2 in memoria con un ordine del cliente con id 1;
 * gli errori del database sono simulati con Mockito.
 */
class OrdineDAOTest {

    private final OrdineDAO dao = new OrdineDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
        // IdOrdine 1
        DatabaseDiTest.esegui("INSERT INTO ordine (totale, IdUtente, cap, città, numerocivico, nome, cognome, via, "
                + "telefono, nomemetodospedizione, dataordine) VALUES (1004.99, 1, '84084', 'Fisciano', '1', "
                + "'Mario', 'Rossi', 'Via Giovanni Paolo II', '+393331234567', 'Standard', '2026-01-15')");
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    private static Ordine nuovoOrdine() {
        Ordine o = new Ordine();
        o.setTotale(209.90);
        o.setIdUtente(1);
        o.setCap("00195");
        o.setCittà("Roma");
        o.setNumeroCivico("22 bis");
        o.setNome("Mario");
        o.setCognome("Rossi");
        o.setVia("Via Antonio Cantore");
        o.setTelefono("+393331234567");
        o.setNomeMetodoSpedizione("Express");
        o.setDataOrdine(Date.valueOf("2026-02-01"));
        return o;
    }

    /** Tutti i campi dell'ordine vengono letti dal database. */
    @Test
    void doRetrieveByIdOrder_esistente_tuttiICampi() {
        Ordine o = dao.doRetrieveByIdOrder(1);

        assertEquals(1, o.getIdOrdine());
        assertEquals(1004.99, o.getTotale(), 0.001);
        assertEquals(1, o.getIdUtente());
        assertEquals("84084", o.getCap());
        assertEquals("Fisciano", o.getCittà());
        assertEquals("1", o.getNumeroCivico());
        assertEquals("Mario", o.getNome());
        assertEquals("Rossi", o.getCognome());
        assertEquals("Via Giovanni Paolo II", o.getVia());
        assertEquals("+393331234567", o.getTelefono());
        assertEquals("Standard", o.getNomeMetodoSpedizione());
        assertEquals(Date.valueOf("2026-01-15"), o.getDataOrdine());
    }

    @Test
    void doRetrieveByIdOrder_inesistente_null() {
        assertNull(dao.doRetrieveByIdOrder(99));
    }

    /** Percorso critico: l'ordine salvato riceve un id e si ritrova tra gli ordini del cliente. */
    @Test
    void doSave_restituisceIdESiRitrova() {
        int id = dao.doSave(nuovoOrdine());

        Ordine salvato = dao.doRetrieveByIdOrder(id);
        assertEquals(2, id);
        assertEquals("Express", salvato.getNomeMetodoSpedizione());
        assertEquals(209.90, salvato.getTotale(), 0.001);
        assertEquals(2, dao.doRetrieveByCustomer(1).size());
    }

    @Test
    void doRetrieveAll_tuttiGliOrdini() {
        dao.doSave(nuovoOrdine());

        assertEquals(2, dao.doRetrieveAll().size());
    }

    @Test
    void doRetrieveByCustomer_soloGliOrdiniDelCliente() {
        List<Ordine> ordini = dao.doRetrieveByCustomer(1);

        assertEquals(1, ordini.size());
        assertEquals(1, ordini.get(0).getIdUtente());
        assertTrue(dao.doRetrieveByCustomer(2).isEmpty());
    }

    @Test
    void doSave_nessunaRigaInserita_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeUpdate()).thenReturn(0);
        DatabaseDiTest.usaStatement(ps);

        assertEquals("INSERT error.",
                assertThrows(RuntimeException.class, () -> dao.doSave(nuovoOrdine())).getMessage());
    }

    @Test
    void doSave_idNonGenerato_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet chiavi = mock(ResultSet.class);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.getGeneratedKeys()).thenReturn(chiavi);
        when(chiavi.next()).thenReturn(false);
        DatabaseDiTest.usaStatement(ps);

        assertEquals("Non è stato possibile recuperare dal database l'ID generato",
                assertThrows(RuntimeException.class, () -> dao.doSave(nuovoOrdine())).getMessage());
    }

    @Test
    void databaseNonDisponibile_ogniOperazioneLanciaEccezione() throws Exception {
        DatabaseDiTest.nonDisponibile();

        assertEquals("Errore durante l'accesso agli ordini, riprova più tardi",
                assertThrows(RuntimeException.class, dao::doRetrieveAll).getMessage());
        assertEquals("Errore durante l'accesso ad un ordine con ID specifico, riprova più tardi",
                assertThrows(RuntimeException.class, () -> dao.doRetrieveByIdOrder(1)).getMessage());
        assertEquals("Errore durante l'accesso agli ordini di un utente, riprova più tardi",
                assertThrows(RuntimeException.class, () -> dao.doRetrieveByCustomer(1)).getMessage());
        assertEquals("Errore durante il salvataggio dell'ordine, riprova più tardi",
                assertThrows(RuntimeException.class, () -> dao.doSave(nuovoOrdine())).getMessage());
    }
}
