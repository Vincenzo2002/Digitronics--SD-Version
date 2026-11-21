package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.MetodoSpedizione;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di {@link MetodoSpedizioneDAO} su un database H2 in memoria con i metodi
 * Standard ed Express; gli errori del database sono simulati con Mockito.
 */
class MetodoSpedizioneDAOTest {

    private final MetodoSpedizioneDAO dao = new MetodoSpedizioneDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    @Test
    void doRetrieveAll_tuttiIMetodi() {
        assertEquals(2, dao.doRetrieveAll().size());
    }

    @Test
    void doRetrieveByName_esistente() {
        MetodoSpedizione express = dao.doRetrieveByName("Express");

        assertEquals("Express", express.getNome());
        assertEquals("Consegna in 24 ore", express.getDescrizione());
        assertEquals(10.00, express.getCosto(), 0.001);
    }

    @Test
    void doRetrieveByName_inesistente_null() {
        assertNull(dao.doRetrieveByName("Drone"));
    }

    @Test
    void databaseNonDisponibile_ogniOperazioneLanciaEccezione() throws Exception {
        DatabaseDiTest.nonDisponibile();

        assertEquals("Errore durante l'accesso ai metodi di spedizione, riprova più tardi",
                assertThrows(RuntimeException.class, dao::doRetrieveAll).getMessage());
        assertEquals("Errore durante il recupero del metodo di spedizione per nome: Express",
                assertThrows(RuntimeException.class, () -> dao.doRetrieveByName("Express")).getMessage());
    }
}
