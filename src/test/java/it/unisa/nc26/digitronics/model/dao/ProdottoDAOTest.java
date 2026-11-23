package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.Prodotto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di {@link ProdottoDAO} su un database H2 in memoria con tre prodotti
 * (due smartphone, uno in vetrina, e delle cuffie in vetrina); gli errori del database
 * sono simulati con Mockito.
 */
class ProdottoDAOTest {

    private final ProdottoDAO dao = new ProdottoDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    private static Prodotto nuovoProdotto() {
        Prodotto p = new Prodotto();
        p.setNome("Pixel 9");
        p.setPrezzo(799.50);
        p.setDescrizione("Smartphone Google");
        p.setImmagine("pixel.png");
        p.setVetrina(true);
        p.setQuantità(3);
        p.setNomeCategoria("Smartphone");
        return p;
    }

    // --- Letture ---

    @Test
    void doRetrieveAll_restituisceTuttiIProdotti() {
        assertEquals(3, dao.doRetrieveAll().size());
    }

    /** Tutti i campi letti dal database finiscono nel bean. */
    @Test
    void doRetrieveById_esistente_tuttiICampi() {
        Prodotto p = dao.doRetrieveById(1);

        assertEquals(1, p.getIdProdotto());
        assertEquals("iPhone 15", p.getNome());
        assertEquals(999.99, p.getPrezzo(), 0.001);
        assertEquals("Smartphone Apple", p.getDescrizione());
        assertEquals("iphone.png", p.getImmagine());
        assertTrue(p.isVetrina());
        assertEquals(10, p.getQuantità());
        assertEquals("Smartphone", p.getNomeCategoria());
    }

    @Test
    void doRetrieveById_inesistente_null() {
        assertNull(dao.doRetrieveById(99));
    }

    @Test
    void doRetrieveByCategory_soloIProdottiDellaCategoria() {
        List<Prodotto> smartphone = dao.doRetrieveByCategory("Smartphone");

        assertEquals(2, smartphone.size());
        assertTrue(smartphone.stream().allMatch(p -> p.getNomeCategoria().equals("Smartphone")));
        assertTrue(dao.doRetrieveByCategory("Inesistente").isEmpty());
    }

    @Test
    void doRetrieveVetrina_soloIProdottiInVetrina() {
        List<Prodotto> vetrina = dao.doRetrieveVetrina();

        assertEquals(2, vetrina.size());
        assertTrue(vetrina.stream().allMatch(Prodotto::isVetrina));
    }

    /** La ricerca trova anche una parte del nome. */
    @Test
    void doRetrieveByName_corrispondenzaParziale() {
        List<Prodotto> trovati = dao.doRetrieveByName("Gal");

        assertEquals(1, trovati.size());
        assertEquals("Galaxy S24", trovati.get(0).getNome());
        assertTrue(dao.doRetrieveByName("Nokia").isEmpty());
    }

    // --- Scritture ---

    @Test
    void doSave_aggiungeIlProdotto() {
        dao.doSave(nuovoProdotto());

        List<Prodotto> trovati = dao.doRetrieveByName("Pixel");
        assertEquals(1, trovati.size());
        assertEquals(799.50, trovati.get(0).getPrezzo(), 0.001);
        assertEquals(4, dao.doRetrieveAll().size());
    }

    @Test
    void doUpdate_modificaIlProdotto() {
        Prodotto p = dao.doRetrieveById(2);
        p.setPrezzo(849.00);
        p.setQuantità(0);
        p.setVetrina(true);

        dao.doUpdate(p);

        Prodotto aggiornato = dao.doRetrieveById(2);
        assertEquals(849.00, aggiornato.getPrezzo(), 0.001);
        assertEquals(0, aggiornato.getQuantità());
        assertTrue(aggiornato.isVetrina());
    }

    @Test
    void doDelete_esistente_eliminaIlProdotto() {
        dao.doDelete(3);

        assertNull(dao.doRetrieveById(3));
        assertEquals(2, dao.doRetrieveAll().size());
    }

    /** Eliminare un prodotto che non esiste è un errore. */
    @Test
    void doDelete_inesistente_lanciaEccezione() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.doDelete(99));

        assertEquals("DELETE error.", ex.getMessage());
    }

    @Test
    void doSave_nessunaRigaInserita_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeUpdate()).thenReturn(0);
        DatabaseDiTest.usaStatement(ps);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.doSave(nuovoProdotto()));
        assertEquals("Errore nel salvataggio del prodotto", ex.getMessage());
    }

    // --- Database non disponibile ---

    @Test
    void databaseNonDisponibile_ogniOperazioneLanciaEccezione() throws Exception {
        DatabaseDiTest.nonDisponibile();
        Executable[] operazioni = {
                dao::doRetrieveAll,
                () -> dao.doRetrieveByCategory("Smartphone"),
                dao::doRetrieveVetrina,
                () -> dao.doRetrieveById(1),
                () -> dao.doSave(nuovoProdotto()),
                () -> dao.doUpdate(nuovoProdotto()),
                () -> dao.doRetrieveByName("iPhone"),
                () -> dao.doDelete(1)
        };

        for (Executable operazione : operazioni) {
            RuntimeException ex = assertThrows(RuntimeException.class, operazione);
            assertTrue(ex.getMessage().endsWith("riprova più tardi"), ex.getMessage());
        }
    }
}
