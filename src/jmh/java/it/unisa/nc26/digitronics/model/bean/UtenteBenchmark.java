package it.unisa.nc26.digitronics.model.bean;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * Misura il calcolo dell'hash della password ({@link Utente#setPasswordHash(String)}),
 * eseguito a ogni login e a ogni registrazione.
 */
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class UtenteBenchmark {

    /** Lunghezza della password: il minimo accettato in registrazione e una password lunga. */
    @Param({"8", "64"})
    public int lunghezza;

    private String password;
    private Utente utente;

    @Setup(Level.Trial)
    public void prepara() {
        password = "p".repeat(lunghezza);
        utente = new Utente();
    }

    @Benchmark
    public String setPasswordHash() {
        utente.setPasswordHash(password);
        // Restituire l'hash impedisce al JIT di eliminare il calcolo come codice morto
        return utente.getPasswordHash();
    }
}
