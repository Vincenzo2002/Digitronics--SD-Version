package it.unisa.nc26.digitronics.utils;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link ConPool}: lettura della configurazione dalle variabili d'ambiente
 * (simulate, senza toccare quelle reali), lettura della password da file e gestione degli
 * errori di connessione. Nessun test si collega a un database.
 */
class ConPoolTest {

    @TempDir Path cartella;

    private final Map<String, String> variabili = new HashMap<>();

    @AfterEach
    void ripristina() {
        ConPool.ambiente = System::getenv;
        ConPool.setDataSource(null);
    }

    private void usaVariabili() {
        ConPool.ambiente = variabili::get;
    }

    // --- getEnv ---

    @Test
    void getEnv_variabileDefinita_restituisceIlValore() {
        variabili.put("DB_USER", "digitronics");
        usaVariabili();

        assertEquals("digitronics", ConPool.getEnv("DB_USER", "root"));
    }

    @Test
    void getEnv_variabileAssente_restituisceIlDefault() {
        usaVariabili();

        assertEquals("root", ConPool.getEnv("DB_USER", "root"));
    }

    /** Una variabile composta solo da spazi vale come assente. */
    @Test
    void getEnv_variabileVuota_restituisceIlDefault() {
        variabili.put("DB_USER", "   ");
        usaVariabili();

        assertEquals("root", ConPool.getEnv("DB_USER", "root"));
    }

    // --- readPassword ---

    /** La password letta dal file (Docker secret) perde gli spazi e l'a capo finali. */
    @Test
    void readPassword_daFile_senzaSpaziFinali() throws Exception {
        Path file = cartella.resolve("db_password.txt");
        Files.writeString(file, "segreta\n");
        variabili.put("DB_PASSWORD_FILE", file.toString());
        variabili.put("DB_PASSWORD", "ignorata");
        usaVariabili();

        assertEquals("segreta", ConPool.readPassword());
    }

    @Test
    void readPassword_fileInesistente_lanciaEccezione() {
        variabili.put("DB_PASSWORD_FILE", cartella.resolve("mancante.txt").toString());
        usaVariabili();

        IllegalStateException ex = assertThrows(IllegalStateException.class, ConPool::readPassword);
        assertEquals("Impossibile leggere il file della password del database", ex.getMessage());
    }

    /** Senza file, o con un percorso vuoto, si usa DB_PASSWORD. */
    @Test
    void readPassword_percorsoFileVuoto_usaLaVariabile() {
        variabili.put("DB_PASSWORD_FILE", " ");
        variabili.put("DB_PASSWORD", "dalla-variabile");
        usaVariabili();

        assertEquals("dalla-variabile", ConPool.readPassword());
    }

    @Test
    void readPassword_nessunaConfigurazione_usaIlDefault() {
        usaVariabili();

        assertEquals("root", ConPool.readPassword());
    }

    // --- configurazionePool ---

    @Test
    void configurazionePool_usaLeVariabiliDAmbiente() {
        variabili.put("DB_URL", "jdbc:mysql://db:3306/digitronics");
        variabili.put("DB_USER", "digitronics");
        variabili.put("DB_PASSWORD", "segreta");
        usaVariabili();

        PoolProperties p = ConPool.configurazionePool();

        assertEquals("jdbc:mysql://db:3306/digitronics", p.getUrl());
        assertEquals("digitronics", p.getUsername());
        assertEquals("segreta", p.getPassword());
        assertEquals("com.mysql.cj.jdbc.Driver", p.getDriverClassName());
    }

    /** Senza variabili si usa il database MySQL locale; le connessioni vengono verificate prima dell'uso. */
    @Test
    void configurazionePool_senzaVariabili_usaIDefault() {
        usaVariabili();

        PoolProperties p = ConPool.configurazionePool();

        assertTrue(p.getUrl().startsWith("jdbc:mysql://localhost:3306/digitronics"));
        assertEquals("root", p.getUsername());
        assertEquals("root", p.getPassword());
        assertTrue(p.isTestOnBorrow());
        assertEquals("SELECT 1", p.getValidationQuery());
        assertEquals(5, p.getValidationQueryTimeout());
    }

    // --- getConnection ---

    @Test
    void getConnection_conDatasource_restituisceLaConnessioneDelPool() throws Exception {
        DataSource datasource = mock(DataSource.class);
        Connection connessione = mock(Connection.class);
        when(datasource.getConnection()).thenReturn(connessione);
        ConPool.setDataSource(datasource);

        assertSame(connessione, ConPool.getConnection());
    }

    @Test
    void getConnection_erroreDelPool_lanciaRuntimeException() throws Exception {
        DataSource datasource = mock(DataSource.class);
        when(datasource.getConnection()).thenThrow(new SQLException("database spento"));
        ConPool.setDataSource(datasource);

        RuntimeException ex = assertThrows(RuntimeException.class, ConPool::getConnection);
        assertEquals("Connessione persa col database, riprova più tardi", ex.getMessage());
    }

    /** Ogni tentativo rilegge la configurazione finché il file della password resta illeggibile. */
    @Test
    void getConnection_configurazioneNonLeggibile_rileggeLaConfigurazione() {
        variabili.put("DB_PASSWORD_FILE", cartella.resolve("mancante.txt").toString());
        AtomicInteger letturePasswordFile = new AtomicInteger();
        ConPool.ambiente = nome -> {
            if ("DB_PASSWORD_FILE".equals(nome)) {
                letturePasswordFile.incrementAndGet();
            }
            return variabili.get(nome);
        };

        RuntimeException ex = assertThrows(RuntimeException.class, ConPool::getConnection);
        assertEquals("Connessione persa col database, riprova più tardi", ex.getMessage());
        assertEquals(1, letturePasswordFile.get());

        assertThrows(RuntimeException.class, ConPool::getConnection);
        assertEquals(2, letturePasswordFile.get());
    }
}
