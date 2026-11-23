package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.Recensione;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link RecensioneDAO} su un database H2 in memoria: il prodotto con id 1 ha due
 * recensioni (punteggi 4 e 5). Gli errori del database sono simulati con Mockito.
 */
class RecensioneDAOTest {

    private final RecensioneDAO dao = new RecensioneDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
        DatabaseDiTest.esegui(
                "INSERT INTO recensione (titolo, descrizione, punteggio, IdProdotto, IdUtente) "
                        + "VALUES ('Ottimo', 'Veloce e leggero', 5, 1, 1)",
                "INSERT INTO recensione (titolo, descrizione, punteggio, IdProdotto, IdUtente) "
                        + "VALUES ('Buono', 'Batteria migliorabile', 4, 1, 2)");
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    /** La recensione salvata riceve l'id generato dal database. */
    @Test
    void doSave_assegnaLId() throws Exception {
        Recensione r = new Recensione();
        r.setTitolo("Comode");
        r.setDescrizione("Ottimo isolamento");
        r.setPunteggio(4);
        r.setIdUtente(1);
        r.setIdProdotto(3);

        dao.doSave(r);

        assertEquals(3, r.getIdRecensione());
        assertEquals(1, dao.doRetrieveByProduct(3).size());
    }

    /** Le recensioni di un prodotto portano con sé il loro autore. */
    @Test
    void doRetrieveByProduct_conAutore() throws Exception {
        Collection<Recensione> recensioni = dao.doRetrieveByProduct(1);

        assertEquals(2, recensioni.size());
        assertTrue(recensioni.stream().allMatch(r -> r.getUtente() != null));
        assertTrue(recensioni.stream().anyMatch(r -> r.getTitolo().equals("Ottimo") && r.getPunteggio() == 5
                && r.getUtente().getNome().equals("Mario")));
        assertTrue(dao.doRetrieveByProduct(2).isEmpty());
    }

    @Test
    void doRetrieveByUtente_soloLeRecensioniDellUtente() throws Exception {
        Collection<Recensione> recensioni = dao.doRetrieveByUtente(2);

        assertEquals(1, recensioni.size());
        Recensione r = recensioni.iterator().next();
        assertEquals("Buono", r.getTitolo());
        assertEquals("Batteria migliorabile", r.getDescrizione());
        assertEquals(1, r.getIdProdotto());
    }

    @Test
    void doRetrieveAveragePunteggio_mediaDeiPunteggi() throws Exception {
        assertEquals(4.5, dao.doRetrieveAveragePunteggio(1), 0.001);
    }

    /** Un prodotto senza recensioni ha media zero. */
    @Test
    void doRetrieveAveragePunteggio_senzaRecensioni_zero() throws Exception {
        assertEquals(0.0, dao.doRetrieveAveragePunteggio(2), 0.001);
    }

    /** Questo DAO non trasforma gli errori: la SQLException arriva al chiamante. */
    @Test
    void databaseNonDisponibile_propagaSQLException() throws Exception {
        DatabaseDiTest.nonDisponibile();

        assertThrows(SQLException.class, () -> dao.doSave(new Recensione()));
        assertThrows(SQLException.class, () -> dao.doRetrieveByProduct(1));
        assertThrows(SQLException.class, () -> dao.doRetrieveByUtente(1));
        assertThrows(SQLException.class, () -> dao.doRetrieveAveragePunteggio(1));
    }
}
