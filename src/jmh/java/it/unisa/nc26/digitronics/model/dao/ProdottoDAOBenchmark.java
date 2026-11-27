package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.Prodotto;
import org.openjdk.jmh.annotations.*;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Misura le due letture di {@link ProdottoDAO} più frequenti: la ricerca per nome,
 * chiamata a ogni carattere digitato nella barra di ricerca, e la vetrina della home.
 *
 * <p>Il database è H2 in memoria, nella stessa JVM: niente rete e niente MySQL.
 * Viene creato una sola volta per fork ({@link Level#Trial}) con un catalogo di
 * dimensione nota; le query sono solo letture, quindi lo stato non cambia tra
 * una misura e l'altra. Si misura quindi DAO + JDBC + pool + H2, non MySQL.</p>
 */
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class ProdottoDAOBenchmark {

    /** Numero di prodotti aggiunti al catalogo, oltre ai 3 di {@link DatabaseDiTest#apriConDati()}. */
    @Param({"100", "1000"})
    public int prodotti;

    private ProdottoDAO prodottoDAO;

    /** Termine cercato, separato dallo stato principale perché riguarda solo la ricerca. */
    @State(Scope.Benchmark)
    public static class Ricerca {
        /** Molti risultati (tutti i prodotti aggiunti), pochi risultati, nessun risultato. */
        @Param({"Modello", "Modello 42", "Inesistente"})
        public String termine;
    }

    @Setup(Level.Trial)
    public void preparaDatabase() throws SQLException {
        DatabaseDiTest.apriConDati();
        String[] insert = new String[prodotti];
        for (int i = 0; i < prodotti; i++) {
            String categoria = (i % 2 == 0) ? "Smartphone" : "Audio";
            insert[i] = "INSERT INTO prodotto (nome, prezzo, descrizione, immagine, vetrina, quantità, nomecategoria) "
                    + "VALUES ('" + categoria + " Modello " + i + "', " + (100 + i) + ".00, "
                    + "'Descrizione del prodotto " + i + "', 'prodotto" + i + ".png', "
                    + (i % 5 == 0 ? "TRUE" : "FALSE") + ", 10, '" + categoria + "')";
        }
        DatabaseDiTest.esegui(insert);
        prodottoDAO = new ProdottoDAO();
    }

    @TearDown(Level.Trial)
    public void chiudiDatabase() {
        DatabaseDiTest.chiudi();
    }

    @Benchmark
    public List<Prodotto> doRetrieveByName(Ricerca ricerca) {
        return prodottoDAO.doRetrieveByName(ricerca.termine);
    }

    @Benchmark
    public List<Prodotto> doRetrieveVetrina() {
        return prodottoDAO.doRetrieveVetrina();
    }
}
