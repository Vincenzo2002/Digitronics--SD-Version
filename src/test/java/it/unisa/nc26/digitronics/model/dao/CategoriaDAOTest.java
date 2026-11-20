package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.Categoria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di {@link CategoriaDAO} su un database H2 in memoria; gli errori del database
 * sono simulati con Mockito.
 */
class CategoriaDAOTest {

    private final CategoriaDAO dao = new CategoriaDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    @Test
    void doRetrieveAll_restituisceTutteLeCategorie() {
        List<Categoria> categorie = dao.doRetrieveAll();

        assertEquals(2, categorie.size());
        assertTrue(categorie.stream().anyMatch(c -> c.getNome().equals("Smartphone")
                && c.getDescrizione().equals("Telefoni") && c.getImmagine().equals("smartphone.png")));
    }

    @Test
    void doRetrieveAll_nessunaCategoria_listaVuota() throws Exception {
        DatabaseDiTest.esegui("DELETE FROM prodotto", "DELETE FROM categoria");

        assertTrue(dao.doRetrieveAll().isEmpty());
    }

    @Test
    void doRetrieveByNomeCategoria_esistente() {
        Categoria categoria = dao.doRetrieveByNomeCategoria("Audio");

        assertEquals("Audio", categoria.getNome());
        assertEquals("Cuffie e casse", categoria.getDescrizione());
        assertEquals("audio.png", categoria.getImmagine());
    }

    @Test
    void doRetrieveByNomeCategoria_inesistente_null() {
        assertNull(dao.doRetrieveByNomeCategoria("Inesistente"));
    }

    @Test
    void doSave_salvaLaCategoria() {
        Categoria nuova = new Categoria();
        nuova.setNome("Tablet");
        nuova.setDescrizione("Tablet e e-reader");
        nuova.setImmagine("tablet.png");

        dao.doSave(nuova);

        Categoria salvata = dao.doRetrieveByNomeCategoria("Tablet");
        assertEquals("Tablet e e-reader", salvata.getDescrizione());
        assertEquals(3, dao.doRetrieveAll().size());
    }

    @Test
    void doSave_nessunaRigaInserita_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeUpdate()).thenReturn(0);
        DatabaseDiTest.usaStatement(ps);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.doSave(new Categoria()));
        assertEquals("INSERT error.", ex.getMessage());
    }

    @Test
    void databaseNonDisponibile_ogniOperazioneLanciaEccezione() throws Exception {
        DatabaseDiTest.nonDisponibile();
        String messaggio = "Errore durante l'accesso alle categorie, riprova più tardi";

        assertEquals(messaggio, assertThrows(RuntimeException.class, dao::doRetrieveAll).getMessage());
        assertEquals(messaggio, assertThrows(RuntimeException.class,
                () -> dao.doRetrieveByNomeCategoria("Audio")).getMessage());
        assertEquals(messaggio, assertThrows(RuntimeException.class,
                () -> dao.doSave(new Categoria())).getMessage());
    }
}
