package it.unisa.nc26.digitronics.gestioneOrdine.controller;

import it.unisa.nc26.digitronics.utils.MyServletException;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Misura il controllo dei campi del form di checkout
 * ({@link GestioneOrdineServlet#validaDatiCheckout}).
 *
 * <p>Il metodo contiene solo regex e parsing della scadenza: il recupero del metodo di
 * spedizione (database) e la verifica dell'indirizzo (Nominatim) restano nella servlet.
 * La scadenza 12/99 mantiene valido il caso {@code datiValidi} in qualunque giorno.</p>
 */
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ValidazioneCheckoutBenchmark {

    // Campi non final: un input costante potrebbe essere ottimizzato dal JIT (constant folding)
    private String nome = "Mario";
    private String nomeErrato = "Mario1";
    private String cognome = "Rossi";
    private String via = "Via Giovanni Paolo II";
    private String numeroCivico = "132";
    private String cap = "84084";
    private String città = "Fisciano";
    private String telefono = "+393331234567";
    private String nomeIntestatario = "Mario Rossi";
    private String cvv = "123";
    private String numeroCarta = "4111 1111 1111 1111";
    private String scadenzaCarta = "12/99";

    /** Caso più costoso: tutti i controlli vengono eseguiti e superati. */
    @Benchmark
    public boolean datiValidi() throws MyServletException {
        GestioneOrdineServlet.validaDatiCheckout(nome, cognome, via, numeroCivico, cap, città, telefono,
                nomeIntestatario, cvv, numeroCarta, scadenzaCarta);
        return true;
    }

    /**
     * Il primo campo è errato: si esegue una sola regex, ma il tempo comprende la creazione
     * di {@link MyServletException} con il suo stack trace.
     */
    @Benchmark
    public Exception primoCampoErrato() {
        try {
            GestioneOrdineServlet.validaDatiCheckout(nomeErrato, cognome, via, numeroCivico, cap, città, telefono,
                    nomeIntestatario, cvv, numeroCarta, scadenzaCarta);
            return null;
        } catch (MyServletException e) {
            return e;
        }
    }
}
