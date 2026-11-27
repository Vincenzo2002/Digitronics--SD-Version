package it.unisa.nc26.digitronics.gestioneOrdine.service.IndirizzoAdapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Misura l'analisi della risposta JSON di Nominatim fatta a ogni ordine
 * ({@link OpenStreetMapVerificaIndirizzoApiAdapterImpl#contieneIndirizzo}).
 *
 * <p>La risposta è una stringa costruita in {@link #prepara()}: la chiamata di rete
 * resta fuori dalla misura. Variano il numero di risultati restituiti e la presenza
 * dell'indirizzo cercato, che se manca obbliga a scorrere tutto l'array.</p>
 */
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ContieneIndirizzoBenchmark {

    private static final String VIA = "Via Giovanni Paolo II";
    private static final String CAP = "84084";
    private static final String CITTÀ = "Fisciano";

    /** Numero di risultati nella risposta. */
    @Param({"1", "10", "50"})
    public int risultati;

    /** Se {@code true} l'indirizzo cercato è l'ultimo risultato, altrimenti manca. */
    @Param({"true", "false"})
    public boolean presente;

    private String rispostaJson;

    @Setup(Level.Trial)
    public void prepara() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < risultati; i++) {
            boolean corrispondente = presente && i == risultati - 1;
            String via = corrispondente ? VIA : "Via Numero " + i;
            String cap = corrispondente ? CAP : String.valueOf(80000 + i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"place_id\": ").append(1000 + i)
                    .append(", \"display_name\": \"").append(via).append(", ").append(CITTÀ).append(", Italia\"")
                    .append(", \"address\": {\"road\": \"").append(via)
                    .append("\", \"city\": \"").append(CITTÀ)
                    .append("\", \"postcode\": \"").append(cap)
                    .append("\", \"country\": \"Italia\"}}");
        }
        rispostaJson = json.append(']').toString();
    }

    @Benchmark
    public boolean contieneIndirizzo() throws JsonProcessingException {
        return OpenStreetMapVerificaIndirizzoApiAdapterImpl.contieneIndirizzo(rispostaJson, VIA, CAP, CITTÀ);
    }
}
