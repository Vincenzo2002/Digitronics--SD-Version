package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.Utente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di {@link UtenteDAO} su un database H2 in memoria con un cliente e un admin;
 * gli errori del database sono simulati con Mockito.
 */
class UtenteDAOTest {

    private final UtenteDAO dao = new UtenteDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    private static Utente nuovoUtente(String email) {
        Utente u = new Utente();
        u.setNome("Luca");
        u.setCognome("Verdi");
        u.setEmail(email);
        u.setPasswordHash("segreta123");
        u.setDataDiNascita(LocalDate.of(2000, 3, 1));
        u.setRuolo(false);
        return u;
    }

    // --- Login ---

    /** Percorso critico: con email e password corrette si ottiene l'utente con tutti i dati. */
    @Test
    void doRetrieveByEmailPassword_credenzialiCorrette() {
        Utente u = dao.doRetrieveByEmailPassword("mario@test.it", DatabaseDiTest.PASSWORD);

        assertEquals(1, u.getIdUtente());
        assertEquals("Mario", u.getNome());
        assertEquals("Rossi", u.getCognome());
        assertEquals("mario@test.it", u.getEmail());
        assertEquals(DatabaseDiTest.hash(DatabaseDiTest.PASSWORD), u.getPasswordHash());
        assertEquals(LocalDate.of(1990, 1, 15), u.getDataDiNascita());
        assertFalse(u.isRuolo());
    }

    @Test
    void doRetrieveByEmailPassword_passwordErrata_null() {
        assertNull(dao.doRetrieveByEmailPassword("mario@test.it", "sbagliata"));
    }

    @Test
    void doRetrieveByEmailPassword_admin_ruoloAdmin() {
        assertTrue(dao.doRetrieveByEmailPassword("admin@test.it", DatabaseDiTest.PASSWORD).isRuolo());
    }

    // --- Registrazione e lettura per id ---

    /** La registrazione restituisce l'id generato, con cui l'utente si ritrova. */
    @Test
    void doSave_restituisceIdGenerato() {
        int id = dao.doSave(nuovoUtente("luca@test.it"));

        Utente salvato = dao.doRetrieveById(id);
        assertEquals(3, id);
        assertEquals("luca@test.it", salvato.getEmail());
        assertEquals(LocalDate.of(2000, 3, 1), salvato.getDataDiNascita());
    }

    /** L'email è unica: una seconda registrazione con la stessa email è rifiutata. */
    @Test
    void doSave_emailGiaRegistrata_lanciaEccezione() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.doSave(nuovoUtente("mario@test.it")));

        assertEquals("Salvataggio dell'utente nel database non riuscito, riprova più tardi", ex.getMessage());
    }

    @Test
    void doRetrieveById_esistente() {
        Utente u = dao.doRetrieveById(2);

        assertEquals("Anna", u.getNome());
        assertTrue(u.isRuolo());
    }

    @Test
    void doRetrieveById_inesistente_null() {
        assertNull(dao.doRetrieveById(99));
    }

    // --- Errori simulati ---

    @Test
    void doSave_nessunaRigaInserita_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeUpdate()).thenReturn(0);
        DatabaseDiTest.usaStatement(ps);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.doSave(nuovoUtente("x@test.it")));
        assertEquals("INSERT error.", ex.getMessage());
    }

    @Test
    void doSave_idNonGenerato_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet chiavi = mock(ResultSet.class);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.getGeneratedKeys()).thenReturn(chiavi);
        when(chiavi.next()).thenReturn(false);
        DatabaseDiTest.usaStatement(ps);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.doSave(nuovoUtente("x@test.it")));
        assertEquals("Non è stato possibile recuperare l'id utente generato dal database, riprova più tardi",
                ex.getMessage());
    }

    /** Un errore nella chiusura del risultato viene segnalato anche se la query è andata a buon fine. */
    @Test
    void erroreInChiusura_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        doThrow(new SQLException("chiusura fallita")).when(rs).close();
        DatabaseDiTest.usaStatement(ps);

        assertEquals("Errore del database durante l'accesso, riprova più tardi", assertThrows(
                RuntimeException.class, () -> dao.doRetrieveByEmailPassword("mario@test.it", "x")).getMessage());
        assertEquals("Recupero dell'utente dal database non riuscito, riprova più tardi", assertThrows(
                RuntimeException.class, () -> dao.doRetrieveById(1)).getMessage());
    }

    @Test
    void databaseNonDisponibile_ogniOperazioneLanciaEccezione() throws Exception {
        DatabaseDiTest.nonDisponibile();

        assertInstanceOf(SQLException.class, assertThrows(RuntimeException.class,
                () -> dao.doRetrieveByEmailPassword("mario@test.it", "x")).getCause());
        assertInstanceOf(SQLException.class, assertThrows(RuntimeException.class,
                () -> dao.doRetrieveById(1)).getCause());
        assertEquals("Salvataggio dell'utente nel database non riuscito, riprova più tardi",
                assertThrows(RuntimeException.class, () -> dao.doSave(nuovoUtente("x@test.it"))).getMessage());
    }
}
