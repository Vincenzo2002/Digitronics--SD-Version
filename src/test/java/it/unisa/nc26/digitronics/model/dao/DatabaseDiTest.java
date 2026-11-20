package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.Utente;
import it.unisa.nc26.digitronics.utils.ConPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Supporto ai test dei DAO. Offre due ambienti:
 * <ul>
 *   <li>un database H2 in memoria, ricreato da {@code database.sql} a ogni apertura e
 *       popolato con pochi dati noti, per verificare le query reali;</li>
 *   <li>un database simulato con Mockito, per provocare gli errori che H2 non produce
 *       (database non raggiungibile, insert che non modifica righe, chiavi non generate).</li>
 * </ul>
 * Al termine di ogni test {@link #chiudi()} ripristina {@link ConPool}.
 */
public final class DatabaseDiTest {

    // CASE_INSENSITIVE_IDENTIFIERS: come MySQL, "idUtente" e "IdUtente" indicano la stessa colonna
    private static final String URL = "jdbc:h2:mem:DigitronicsDao;MODE=MySQL;DATABASE_TO_UPPER=false;"
            + "CASE_INSENSITIVE_IDENTIFIERS=TRUE;INIT=RUNSCRIPT FROM 'classpath:database/database.sql'";

    /** Password in chiaro degli utenti di prova. */
    public static final String PASSWORD = "password1";

    private static DataSource pool;

    private DatabaseDiTest() {
    }

    /** Crea il database H2 vuoto e lo collega a {@link ConPool}. */
    public static void apri() throws SQLException {
        PoolProperties p = new PoolProperties();
        p.setUrl(URL);
        p.setDriverClassName("org.h2.Driver");
        p.setUsername("sa");
        p.setPassword("");
        p.setInitialSize(5);
        p.setMinIdle(5);
        p.setMaxActive(5);
        pool = new DataSource();
        pool.setPoolProperties(p);
        ConPool.setDataSource(pool);
        // H2 non ha la funzione SHA1 di MySQL, usata dal login: la si definisce in Java
        esegui("CREATE ALIAS IF NOT EXISTS SHA1 FOR '" + DatabaseDiTest.class.getName() + ".sha1'");
    }

    /** Crea il database H2 e lo popola con categorie, prodotti, utenti e metodi di spedizione. */
    public static void apriConDati() throws SQLException {
        apri();
        esegui(
                "INSERT INTO categoria (nome, descrizione, immagine) VALUES ('Smartphone', 'Telefoni', 'smartphone.png')",
                "INSERT INTO categoria (nome, descrizione, immagine) VALUES ('Audio', 'Cuffie e casse', 'audio.png')",
                // IdProdotto 1, 2 e 3, generati in ordine
                "INSERT INTO prodotto (nome, prezzo, descrizione, immagine, vetrina, quantità, nomecategoria) "
                        + "VALUES ('iPhone 15', 999.99, 'Smartphone Apple', 'iphone.png', TRUE, 10, 'Smartphone')",
                "INSERT INTO prodotto (nome, prezzo, descrizione, immagine, vetrina, quantità, nomecategoria) "
                        + "VALUES ('Galaxy S24', 899.00, 'Smartphone Samsung', 'galaxy.png', FALSE, 5, 'Smartphone')",
                "INSERT INTO prodotto (nome, prezzo, descrizione, immagine, vetrina, quantità, nomecategoria) "
                        + "VALUES ('Cuffie Sony', 199.90, 'Cuffie wireless', 'sony.png', TRUE, 20, 'Audio')",
                // IdUtente 1 (cliente) e 2 (admin)
                "INSERT INTO utente (nome, cognome, email, passwordhash, datadinascita, ruolo) "
                        + "VALUES ('Mario', 'Rossi', 'mario@test.it', '" + hash(PASSWORD) + "', '1990-01-15', FALSE)",
                "INSERT INTO utente (nome, cognome, email, passwordhash, datadinascita, ruolo) "
                        + "VALUES ('Anna', 'Bianchi', 'admin@test.it', '" + hash(PASSWORD) + "', '1985-05-05', TRUE)",
                "INSERT INTO metodospedizione (nome, descrizione, costo) VALUES ('Standard', 'Consegna in 5 giorni', 5.00)",
                "INSERT INTO metodospedizione (nome, descrizione, costo) VALUES ('Express', 'Consegna in 24 ore', 10.00)"
        );
    }

    /** Scollega il database da {@link ConPool} e chiude il pool H2, se aperto. */
    public static void chiudi() {
        ConPool.setDataSource(null);
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    /** Esegue istruzioni SQL sul database di test. */
    public static void esegui(String... istruzioni) throws SQLException {
        try (Connection con = ConPool.getConnection(); Statement st = con.createStatement()) {
            for (String sql : istruzioni) {
                st.execute(sql);
            }
        }
    }

    /** Hash SHA-1 esadecimale, come quello salvato da {@link Utente#setPasswordHash(String)}. */
    public static String hash(String password) {
        Utente utente = new Utente();
        utente.setPasswordHash(password);
        return utente.getPasswordHash();
    }

    /** Implementazione della funzione SQL SHA1 per H2 (stesso risultato di MySQL). */
    public static String sha1(String valore) {
        return valore == null ? null : hash(valore);
    }

    /** Sostituisce il database con uno che rifiuta ogni istruzione con una {@link SQLException}. */
    public static void nonDisponibile() throws SQLException {
        Connection connessione = mock(Connection.class);
        SQLException errore = new SQLException("database non disponibile");
        when(connessione.prepareStatement(anyString())).thenThrow(errore);
        when(connessione.prepareStatement(anyString(), anyInt())).thenThrow(errore);
        usaConnessione(connessione);
    }

    /** Sostituisce il database con uno le cui istruzioni sono tutte {@code statement}. */
    public static void usaStatement(PreparedStatement statement) throws SQLException {
        Connection connessione = mock(Connection.class);
        when(connessione.prepareStatement(anyString())).thenReturn(statement);
        when(connessione.prepareStatement(anyString(), anyInt())).thenReturn(statement);
        usaConnessione(connessione);
    }

    private static void usaConnessione(Connection connessione) throws SQLException {
        DataSource datasource = mock(DataSource.class);
        when(datasource.getConnection()).thenReturn(connessione);
        ConPool.setDataSource(datasource);
    }
}
