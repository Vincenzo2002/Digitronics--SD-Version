package it.unisa.nc26.digitronics.gestioneOrdine.service.IndirizzoAdapter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementazione dell'interfaccia VerificaIndirizzoApiAdapter che utilizza l'API di OpenStreetMap
 * (Nominatim) per verificare la validità di un indirizzo specificato.
 */
public class OpenStreetMapVerificaIndirizzoApiAdapterImpl implements VerificaIndirizzoApiAdapter {

    /**
     * Verifica se un indirizzo composto da via, CAP e città è valido utilizzando l'API di OpenStreetMap. Si verificano
     * solo indirizzi italiani
     *
     * @param via   il nome della via
     * @param cap   il codice di avviamento postale (CAP)
     * @param città il nome della città
     * @return true se l'indirizzo è valido, false altrimenti
     * @throws RuntimeException se si verifica un errore durante la connessione o l'elaborazione della risposta API
     */
    @Override
    public boolean verifica(String via, String cap, String città) {
        try {
            HttpURLConnection conn = apriConnessione(costruisciUrl(via, cap, città));
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; Digitronics/1.0)");

            // Lettura della risposta dall'API
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("Errore durante la connessione all'API di OpenStreetMap: " + responseCode);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return contieneIndirizzo(response.toString(), via, cap, città);
        } catch (Exception e) {
            throw new RuntimeException("Errore durante la verifica dell'indirizzo: " + e.getMessage(), e);
        }
    }

    /**
     * Apre la connessione HTTP verso l'URL indicato. È un metodo a parte perché i test
     * lo sostituiscono con una connessione simulata, senza usare la rete.
     *
     * @param url l'URL della richiesta
     * @return la connessione, non ancora avviata
     * @throws IOException se la connessione non si può aprire
     */
    HttpURLConnection apriConnessione(String url) throws IOException {
        URL indirizzo = URI.create(url).toURL();
        return (HttpURLConnection) indirizzo.openConnection();
    }

    /**
     * Costruisce l'URL della ricerca su OpenStreetMap (Nominatim), limitata agli indirizzi italiani.
     *
     * @param via   il nome della via
     * @param cap   il codice di avviamento postale (CAP)
     * @param città il nome della città
     * @return l'URL della richiesta
     */
    static String costruisciUrl(String via, String cap, String città) {
        String baseUrl = "https://nominatim.openstreetmap.org/search";
        String query = String.format("%s, %s, %s, Italia", via, cap, città);
        return String.format("%s?q=%s&format=json&addressdetails=1&countrycodes=it",
                baseUrl,
                URLEncoder.encode(query, StandardCharsets.UTF_8));
    }

    /**
     * Controlla se la risposta JSON di Nominatim contiene un indirizzo con la città
     * (anche come frazione o paese), il CAP e la via indicati. Il confronto su città
     * e via non distingue maiuscole e minuscole; la via può essere parte del nome restituito.
     *
     * @param rispostaJson il corpo della risposta dell'API
     * @param via          il nome della via
     * @param cap          il codice di avviamento postale (CAP)
     * @param città        il nome della città
     * @return true se almeno un risultato corrisponde, false altrimenti
     * @throws JsonProcessingException se la risposta non è un JSON valido
     */
    static boolean contieneIndirizzo(String rispostaJson, String via, String cap, String città)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(rispostaJson);

        // Se l'array JSON è vuoto, l'indirizzo non esiste
        if (!rootNode.isArray() || rootNode.size() == 0) {
            return false;
        }

        // Verifica se l'indirizzo corrisponde alla via, CAP e città specificati
        for (JsonNode node : rootNode) {
            JsonNode address = node.get("address");
            if (address != null) {
                String returnedCity = address.path("city").asText("").toLowerCase();
                String returnedHamlet = address.path("hamlet").asText("").toLowerCase();
                String returnedVillage = address.path("village").asText("").toLowerCase();
                String returnedPostcode = address.path("postcode").asText("");
                String returnedRoad = address.path("road").asText("").toLowerCase();

                boolean cittàEsiste = città.toLowerCase().equals(returnedCity) ||
                        città.toLowerCase().equals(returnedHamlet) ||
                        città.toLowerCase().equals(returnedVillage);

                if (cittàEsiste &&
                        returnedPostcode.equals(cap) &&
                        returnedRoad.contains(via.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }
}
