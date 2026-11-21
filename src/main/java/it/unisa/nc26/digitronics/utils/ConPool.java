package it.unisa.nc26.digitronics.utils;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.TimeZone;
import java.util.function.UnaryOperator;

/**
 * Classe per la gestione del pool di connessioni al database.
 *
 * Questa classe utilizza il pool di connessioni fornito da Apache Tomcat
 * per ottimizzare la gestione delle connessioni al database.
 *
 * <p>I parametri di connessione vengono letti da variabili d'ambiente
 * (con valori di default adatti a un'installazione locale):
 * <ul>
 *   <li>{@code DB_URL} &mdash; URL JDBC del database;</li>
 *   <li>{@code DB_USER} &mdash; utente del database;</li>
 *   <li>{@code DB_PASSWORD} &mdash; password del database;</li>
 *   <li>{@code DB_PASSWORD_FILE} &mdash; percorso di un file contenente la password
 *       (es. un Docker secret montato in {@code /run/secrets}); se presente ha
 *       la precedenza su {@code DB_PASSWORD}.</li>
 * </ul>
 */
public class ConPool {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/digitronics?serverTimezone=" + TimeZone.getDefault().getID();
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "root";

    private static DataSource datasource;

    /**
     * Sorgente delle variabili d'ambiente. Di default legge quelle del processo;
     * i test la sostituiscono per verificare la configurazione senza modificare l'ambiente reale.
     */
    static UnaryOperator<String> ambiente = System::getenv;

    /**
     * Permette di sovrascrivere il datasource, utile per i test.
     */
    public static void setDataSource(DataSource ds) {
        datasource = ds;
    }

    /**
     * Recupera una connessione al database dal pool tramite JDBC.
     *
     * Se il pool non è ancora stato inizializzato, viene configurato e creato.
     *
     * @return un oggetto {@link Connection} dal pool di connessioni
     * @throws RuntimeException se si verifica un errore durante la connessione al database
     */
    public static Connection getConnection() {
        try {
            if (datasource == null) {
                // La configurazione viene letta prima di creare il pool: se fallisce,
                // il datasource resta null e il prossimo tentativo la rilegge
                PoolProperties p = configurazionePool();
                DataSource nuovo = new DataSource();
                nuovo.setPoolProperties(p);
                datasource = nuovo;
            }
            // Recupera una connessione dal pool
            return datasource.getConnection();
        } catch (Exception ex) {
            throw new RuntimeException("Connessione persa col database, riprova più tardi");
        }
    }

    /**
     * Costruisce la configurazione del pool di connessioni a partire dalle variabili d'ambiente.
     *
     * @return le proprietà del pool
     */
    static PoolProperties configurazionePool() {
        // Configurazione delle proprietà del pool di connessioni
        PoolProperties p = new PoolProperties();
        p.setUrl(getEnv("DB_URL", DEFAULT_URL));
        p.setDriverClassName("com.mysql.cj.jdbc.Driver");
        p.setUsername(getEnv("DB_USER", DEFAULT_USER));
        p.setPassword(readPassword());
        p.setMaxActive(100);
        p.setInitialSize(10);
        p.setMinIdle(10);
        // Verifica la connessione prima di consegnarla all'applicazione:
        // dopo un riavvio di MySQL il pool tenta di riconnettere quelle
        // non valide. Il timeout della query è espresso in secondi;
        // l'intervallo zero richiede la verifica a ogni prelievo.
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1");
        p.setValidationQueryTimeout(5);
        p.setValidationInterval(0);
        p.setRemoveAbandonedTimeout(60);
        p.setRemoveAbandoned(true);
        return p;
    }

    /**
     * Restituisce il valore di una variabile d'ambiente o un valore di default
     * se la variabile non è definita o è vuota.
     */
    static String getEnv(String name, String defaultValue) {
        String value = ambiente.apply(name);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /**
     * Legge la password del database da {@code DB_PASSWORD_FILE}, se definita,
     * altrimenti da {@code DB_PASSWORD} o dal valore di default.
     */
    static String readPassword() {
        String passwordFile = ambiente.apply("DB_PASSWORD_FILE");
        if (passwordFile != null && !passwordFile.isBlank()) {
            try {
                return Files.readString(Path.of(passwordFile), StandardCharsets.UTF_8).strip();
            } catch (IOException e) {
                throw new IllegalStateException("Impossibile leggere il file della password del database", e);
            }
        }
        return getEnv("DB_PASSWORD", DEFAULT_PASSWORD);
    }
}
