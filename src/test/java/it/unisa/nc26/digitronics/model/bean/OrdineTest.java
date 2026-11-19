package it.unisa.nc26.digitronics.model.bean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di unità di {@link Ordine#equals(Object)} e {@link Ordine#hashCode()}:
 * due ordini sono uguali se e solo se hanno lo stesso id.
 */
class OrdineTest {

    @Test
    void ordine_stessoId_sonoUgualiEConStessoHash() {
        Ordine a = new Ordine();
        a.setIdOrdine(1);
        a.setTotale(10.0);
        Ordine b = new Ordine();
        b.setIdOrdine(1);
        b.setTotale(999.0);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ordine_idDiversi_nonSonoUguali() {
        Ordine a = new Ordine();
        a.setIdOrdine(1);
        Ordine b = new Ordine();
        b.setIdOrdine(2);

        assertNotEquals(a, b);
    }

    @Test
    void ordine_confrontoConNullOAltroTipo_restituisceFalse() {
        Ordine a = new Ordine();
        a.setIdOrdine(1);

        assertNotEquals(null, a);
        assertNotEquals("1", a);
    }
}
