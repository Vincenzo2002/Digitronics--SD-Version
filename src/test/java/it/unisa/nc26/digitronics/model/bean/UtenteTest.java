package it.unisa.nc26.digitronics.model.bean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di unità di {@link Utente#setPasswordHash(String)}, che trasforma la password
 * in un hash SHA-1 esadecimale di 40 caratteri.
 */
class UtenteTest {

    @Test
    void setPasswordHash_passwordNota_produceSha1Atteso() {
        Utente utente = new Utente();
        utente.setPasswordHash("password");

        assertEquals("5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", utente.getPasswordHash());
    }

    @Test
    void setPasswordHash_stringaVuota_produceSha1DellaStringaVuota() {
        Utente utente = new Utente();
        utente.setPasswordHash("");

        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", utente.getPasswordHash());
    }

    @Test
    void setPasswordHash_risultatoSempreDi40CaratteriEsadecimali() {
        Utente utente = new Utente();
        utente.setPasswordHash("Pàsswörd€123");

        assertTrue(utente.getPasswordHash().matches("[0-9a-f]{40}"));
    }

    @Test
    void setPasswordHash_stessaPassword_stessoHash() {
        Utente a = new Utente();
        Utente b = new Utente();
        a.setPasswordHash("segreta");
        b.setPasswordHash("segreta");

        assertEquals(a.getPasswordHash(), b.getPasswordHash());
    }

    @Test
    void setPasswordHash_null_lanciaNullPointerException() {
        Utente utente = new Utente();

        assertThrows(NullPointerException.class, () -> utente.setPasswordHash(null));
    }
}
