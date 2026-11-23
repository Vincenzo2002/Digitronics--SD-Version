package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.ItemOrdine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di {@link ItemOrdineDAO} su un database H2 in memoria con un ordine vuoto;
 * gli errori del database sono simulati con Mockito.
 */
class ItemOrdineDAOTest {

    private final ItemOrdineDAO dao = new ItemOrdineDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
        // IdOrdine 1
        DatabaseDiTest.esegui("INSERT INTO ordine (totale, IdUtente, cap, città, numerocivico, nome, cognome, via, "
                + "telefono, nomemetodospedizione, dataordine) VALUES (1999.98, 1, '84084', 'Fisciano', '1', "
                + "'Mario', 'Rossi', 'Via Giovanni Paolo II', NULL, 'Standard', '2026-01-15')");
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    private static ItemOrdine item() {
        ItemOrdine item = new ItemOrdine();
        item.setNome("iPhone 15");
        item.setImmagine("iphone.png");
        item.setPrezzo(999.99);
        item.setQuantità(2);
        item.setIdProdotto(1);
        item.setIdOrdine(1);
        return item;
    }

    /** L'elemento salvato conserva nome e prezzo del prodotto al momento dell'ordine. */
    @Test
    void doSave_eDoRetrieveByOrdine() {
        dao.doSave(item());

        List<ItemOrdine> items = dao.doRetrieveByOrdine(1);
        assertEquals(1, items.size());
        ItemOrdine salvato = items.get(0);
        assertEquals("iPhone 15", salvato.getNome());
        assertEquals("iphone.png", salvato.getImmagine());
        assertEquals(999.99, salvato.getPrezzo(), 0.001);
        assertEquals(2, salvato.getQuantità());
        assertEquals(1, salvato.getIdProdotto());
        assertEquals(1, salvato.getIdOrdine());
    }

    @Test
    void doRetrieveByOrdine_ordineSenzaElementi() {
        assertTrue(dao.doRetrieveByOrdine(1).isEmpty());
    }

    @Test
    void doSave_nessunaRigaInserita_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeUpdate()).thenReturn(0);
        DatabaseDiTest.usaStatement(ps);

        assertEquals("INSERT error.", assertThrows(RuntimeException.class, () -> dao.doSave(item())).getMessage());
    }

    @Test
    void databaseNonDisponibile_ogniOperazioneLanciaEccezione() throws Exception {
        DatabaseDiTest.nonDisponibile();
        String messaggio = "Errore durante l'accesso agli oggetti dell'ordine, riprova più tardi";

        assertEquals(messaggio, assertThrows(RuntimeException.class, () -> dao.doSave(item())).getMessage());
        assertEquals(messaggio, assertThrows(RuntimeException.class, () -> dao.doRetrieveByOrdine(1)).getMessage());
    }
}
