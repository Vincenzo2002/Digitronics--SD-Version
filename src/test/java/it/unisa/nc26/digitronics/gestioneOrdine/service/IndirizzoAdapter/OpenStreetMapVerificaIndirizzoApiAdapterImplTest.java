package it.unisa.nc26.digitronics.gestioneOrdine.service.IndirizzoAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test di unità di {@link OpenStreetMapVerificaIndirizzoApiAdapterImpl}: costruzione dell'URL
 * e interpretazione della risposta JSON di Nominatim. Nessun test effettua chiamate di rete:
 * le risposte dell'API sono stringhe JSON scritte nel test.
 */
class OpenStreetMapVerificaIndirizzoApiAdapterImplTest {

    private static final String RISPOSTA_ROMA = """
            [{"address": {"road": "Via Antonio Cantore", "city": "Roma", "postcode": "00195"}}]
            """;

    // --- Costruzione dell'URL ---

    /** L'URL contiene la query con gli spazi sostituiti da "+" ed è limitato all'Italia. */
    @Test
    void costruisciUrl_componeLaQueryPerNominatim() {
        String url = OpenStreetMapVerificaIndirizzoApiAdapterImpl.costruisciUrl("Piazza Duomo", "84011", "Pogerola");

        assertEquals("https://nominatim.openstreetmap.org/search"
                + "?q=Piazza+Duomo,+84011,+Pogerola,+Italia&format=json&addressdetails=1&countrycodes=it", url);
    }

    // --- Indirizzo trovato ---

    @Test
    void contieneIndirizzo_corrispondenzaEsatta_true() throws JsonProcessingException {
        assertTrue(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(RISPOSTA_ROMA, "Via Antonio Cantore", "00195", "Roma"));
    }

    /** Città e via vengono confrontate senza distinguere maiuscole e minuscole. */
    @Test
    void contieneIndirizzo_maiuscoleDiverse_true() throws JsonProcessingException {
        assertTrue(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(RISPOSTA_ROMA, "via antonio cantore", "00195", "ROMA"));
    }

    /** La via indicata può essere solo una parte del nome restituito. */
    @Test
    void contieneIndirizzo_viaParziale_true() throws JsonProcessingException {
        assertTrue(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(RISPOSTA_ROMA, "Cantore", "00195", "Roma"));
    }

    /** Una frazione (hamlet) vale come città. */
    @Test
    void contieneIndirizzo_cittaComeFrazione_true() throws JsonProcessingException {
        String json = """
                [{"address": {"road": "Piazza Duomo", "hamlet": "Pogerola", "postcode": "84011"}}]
                """;

        assertTrue(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(json, "Piazza Duomo", "84011", "Pogerola"));
    }

    /** Un paese (village) vale come città. */
    @Test
    void contieneIndirizzo_cittaComePaese_true() throws JsonProcessingException {
        String json = """
                [{"address": {"road": "Via Roma", "village": "Furore", "postcode": "84010"}}]
                """;

        assertTrue(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(json, "Via Roma", "84010", "Furore"));
    }

    /** Basta che uno dei risultati corrisponda, anche se i precedenti non hanno l'indirizzo. */
    @Test
    void contieneIndirizzo_secondoRisultatoCorrisponde_true() throws JsonProcessingException {
        String json = """
                [{"display_name": "senza address"},
                 {"address": {"road": "Via Antonio Cantore", "city": "Roma", "postcode": "00195"}}]
                """;

        assertTrue(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(json, "Via Antonio Cantore", "00195", "Roma"));
    }

    // --- Indirizzo non trovato ---

    @Test
    void contieneIndirizzo_nessunRisultato_false() throws JsonProcessingException {
        assertFalse(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo("[]", "Via Antonio Cantore", "00195", "Roma"));
    }

    @Test
    void contieneIndirizzo_rispostaNonArray_false() throws JsonProcessingException {
        assertFalse(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo("{}", "Via Antonio Cantore", "00195", "Roma"));
    }

    @Test
    void contieneIndirizzo_capDiverso_false() throws JsonProcessingException {
        assertFalse(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(RISPOSTA_ROMA, "Via Antonio Cantore", "00196", "Roma"));
    }

    @Test
    void contieneIndirizzo_cittaDiversa_false() throws JsonProcessingException {
        assertFalse(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(RISPOSTA_ROMA, "Via Antonio Cantore", "00195", "Milano"));
    }

    @Test
    void contieneIndirizzo_viaDiversa_false() throws JsonProcessingException {
        assertFalse(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo(RISPOSTA_ROMA, "Via Nazionale", "00195", "Roma"));
    }

    @Test
    void contieneIndirizzo_risultatoSenzaAddress_false() throws JsonProcessingException {
        assertFalse(OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo("[{\"display_name\": \"Roma\"}]", "Via Antonio Cantore", "00195", "Roma"));
    }

    @Test
    void contieneIndirizzo_jsonNonValido_lanciaEccezione() {
        assertThrows(JsonProcessingException.class, () -> OpenStreetMapVerificaIndirizzoApiAdapterImpl
                .contieneIndirizzo("non è json", "Via Antonio Cantore", "00195", "Roma"));
    }
}
