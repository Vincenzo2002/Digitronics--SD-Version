package it.unisa.nc26.digitronics.utils;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di unità di {@link MyServletException}, l'eccezione applicativa mostrata
 * nella pagina di errore.
 */
class MyServletExceptionTest {

    @Test
    void costruttoreConMessaggio_conservaIlMessaggio() {
        MyServletException ex = new MyServletException("Prodotto non esistente");

        assertEquals("Prodotto non esistente", ex.getMessage());
        assertInstanceOf(ServletException.class, ex);
    }

    @Test
    void costruttoreVuoto_senzaMessaggio() {
        assertNull(new MyServletException().getMessage());
    }
}
