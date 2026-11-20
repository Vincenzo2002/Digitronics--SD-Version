package it.unisa.nc26.digitronics.gestioneOrdine.service.IndirizzoAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link OpenStreetMapVerificaIndirizzoApiAdapterImpl}: costruzione dell'URL,
 * interpretazione della risposta JSON di Nominatim e chiamata HTTP. Nessun test effettua chiamate
 * di rete: le risposte dell'API sono stringhe JSON scritte nel test e la connessione è un mock.
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

    // --- Verifica completa con la connessione HTTP simulata ---

    /** Adapter la cui connessione verso Nominatim è sostituita da {@code connessione}. */
    private static OpenStreetMapVerificaIndirizzoApiAdapterImpl adapterCon(HttpURLConnection connessione) {
        return new OpenStreetMapVerificaIndirizzoApiAdapterImpl() {
            @Override
            HttpURLConnection apriConnessione(String url) {
                return connessione;
            }
        };
    }

    private static HttpURLConnection connessioneCheRisponde(int codice, String corpo) throws IOException {
        HttpURLConnection connessione = mock(HttpURLConnection.class);
        when(connessione.getResponseCode()).thenReturn(codice);
        when(connessione.getInputStream())
                .thenReturn(new ByteArrayInputStream(corpo.getBytes(StandardCharsets.UTF_8)));
        return connessione;
    }

    /** Percorso critico: la richiesta è una GET con User-Agent e la risposta contiene l'indirizzo. */
    @Test
    void verifica_indirizzoTrovato_true() throws IOException {
        HttpURLConnection connessione = connessioneCheRisponde(200, RISPOSTA_ROMA);

        assertTrue(adapterCon(connessione).verifica("Via Antonio Cantore", "00195", "Roma"));
        verify(connessione).setRequestMethod("GET");
        verify(connessione).setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Digitronics/1.0)");
    }

    /** Una risposta su più righe viene letta per intero. */
    @Test
    void verifica_rispostaSuPiuRighe_true() throws IOException {
        String json = "[{\"address\":\n {\"road\": \"Via Antonio Cantore\",\n"
                + " \"city\": \"Roma\", \"postcode\": \"00195\"}}]";

        assertTrue(adapterCon(connessioneCheRisponde(200, json)).verifica("Via Antonio Cantore", "00195", "Roma"));
    }

    @Test
    void verifica_indirizzoNonTrovato_false() throws IOException {
        assertFalse(adapterCon(connessioneCheRisponde(200, "[]")).verifica("Via Inesistente", "00000", "Nessuna"));
    }

    /** Un codice HTTP diverso da 200 è un errore della verifica. */
    @Test
    void verifica_rispostaNon200_lanciaEccezione() throws IOException {
        HttpURLConnection connessione = connessioneCheRisponde(503, "");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adapterCon(connessione).verifica("Via Antonio Cantore", "00195", "Roma"));
        assertEquals("Errore durante la verifica dell'indirizzo: "
                + "Errore durante la connessione all'API di OpenStreetMap: 503", ex.getMessage());
    }

    /** Un errore di rete diventa un errore della verifica, con la causa originale. */
    @Test
    void verifica_erroreDiRete_lanciaEccezione() throws IOException {
        HttpURLConnection connessione = mock(HttpURLConnection.class);
        IOException causa = new IOException("rete non raggiungibile");
        when(connessione.getResponseCode()).thenThrow(causa);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> adapterCon(connessione).verifica("Via Antonio Cantore", "00195", "Roma"));
        assertSame(causa, ex.getCause());
    }

    /** Senza sostituzioni l'adapter apre una connessione HTTP verso l'URL indicato (senza inviarla). */
    @Test
    void apriConnessione_creaConnessioneHttpVersoLUrl() throws IOException {
        String url = OpenStreetMapVerificaIndirizzoApiAdapterImpl.costruisciUrl("Via Roma", "84010", "Furore");

        HttpURLConnection connessione = new OpenStreetMapVerificaIndirizzoApiAdapterImpl().apriConnessione(url);

        assertEquals(url, connessione.getURL().toString());
    }
}
