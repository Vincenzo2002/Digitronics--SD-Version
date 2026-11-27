package it.unisa.nc26.digitronics.autenticazione.controller;

import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
import org.openjdk.jmh.annotations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Misura come scala l'unione del carrello di sessione con quello salvato nel database,
 * eseguita al login ({@link AutenticazioneServlet#mergeCarrello}).
 *
 * <p>I due carrelli hanno la stessa dimensione e metà dei prodotti in comune.
 * {@code mergeCarrello} modifica il carrello del database (aggiunge elementi e somma
 * quantità), quindi ogni invocazione lavora su copie nuove; il benchmark
 * {@link #soloCopia()} misura il costo delle sole copie, da sottrarre al risultato.</p>
 */
@Fork(2)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class MergeCarrelloBenchmark {

    /** Numero di prodotti in ciascuno dei due carrelli. */
    @Param({"5", "50", "500"})
    public int dimensione;

    private List<ItemCarrello> carrelloDB;
    private List<ItemCarrello> carrelloSessione;

    @Setup(Level.Trial)
    public void prepara() {
        carrelloDB = new ArrayList<>();
        carrelloSessione = new ArrayList<>();
        for (int i = 0; i < dimensione; i++) {
            carrelloDB.add(item(i));
            // Prodotti da dimensione/2 a dimensione*3/2: la prima metà è in comune con il database
            carrelloSessione.add(item(i + dimensione / 2));
        }
    }

    @Benchmark
    public List<ItemCarrello> mergeCarrello() {
        List<ItemCarrello> db = copia(carrelloDB);
        AutenticazioneServlet.mergeCarrello(db, copia(carrelloSessione));
        return db;
    }

    /** Riferimento: costo delle copie dei due carrelli, senza unione. */
    @Benchmark
    public List<ItemCarrello> soloCopia() {
        List<ItemCarrello> db = copia(carrelloDB);
        copia(carrelloSessione);
        return db;
    }

    private static List<ItemCarrello> copia(List<ItemCarrello> carrello) {
        List<ItemCarrello> copia = new ArrayList<>(carrello.size());
        for (ItemCarrello originale : carrello) {
            ItemCarrello item = new ItemCarrello();
            item.setIdUtente(originale.getIdUtente());
            item.setIdProdotto(originale.getIdProdotto());
            item.setQuantità(originale.getQuantità());
            copia.add(item);
        }
        return copia;
    }

    private static ItemCarrello item(int idProdotto) {
        ItemCarrello item = new ItemCarrello();
        item.setIdUtente(1);
        item.setIdProdotto(idProdotto);
        item.setQuantità(1);
        return item;
    }
}
