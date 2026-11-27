package it.unisa.nc26.digitronics.registrazione.controller;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Misura il controllo dei dati del form di registrazione
 * ({@link RegistrazioneServlet#datiRegistrazioneValidi}), senza il salvataggio dell'utente.
 */
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class ValidazioneRegistrazioneBenchmark {

    // Campi non final: un input costante potrebbe essere ottimizzato dal JIT (constant folding)
    private String nome = "Mario";
    private String nomeErrato = "Mario1";
    private String cognome = "Rossi";
    private String email = "mario.rossi@studenti.unisa.it";
    private String password = "password1";

    /** Caso più costoso: tutte le regex vengono eseguite. */
    @Benchmark
    public boolean datiValidi() {
        return RegistrazioneServlet.datiRegistrazioneValidi(nome, cognome, email, password);
    }

    /** Il nome è errato: la valutazione si ferma alla prima regex. */
    @Benchmark
    public boolean primoCampoErrato() {
        return RegistrazioneServlet.datiRegistrazioneValidi(nomeErrato, cognome, email, password);
    }
}
