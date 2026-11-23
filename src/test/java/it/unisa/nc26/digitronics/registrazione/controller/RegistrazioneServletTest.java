package it.unisa.nc26.digitronics.registrazione.controller;

import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
import it.unisa.nc26.digitronics.model.bean.Utente;
import it.unisa.nc26.digitronics.registrazione.service.RegistrazioneService;
import it.unisa.nc26.digitronics.utils.MyServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link RegistrazioneServlet}: regole di validazione dei dati
 * di registrazione e percorso di registrazione riuscita. Il service e le API
 * Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrazioneServletTest {

    @Mock private RegistrazioneService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private RegistrazioneServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new RegistrazioneServlet();
        servlet.setRegistrazioneService(service);
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        impostaParametri("Mario", "Rossi", "mario.rossi@test.it", "password1", "2000-01-15");
    }

    private void impostaParametri(String nome, String cognome, String email, String password, String data) {
        when(request.getParameter("nome")).thenReturn(nome);
        when(request.getParameter("cognome")).thenReturn(cognome);
        when(request.getParameter("email")).thenReturn(email);
        when(request.getParameter("password")).thenReturn(password);
        when(request.getParameter("datadinascita")).thenReturn(data);
    }

    private void assertDatiNonValidi() {
        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals("Dati in input non validi", ex.getMessage());
        verify(service, never()).registraUtente(any());
    }

    // --- Percorso critico: registrazione riuscita ---

    /** Con dati validi l'utente viene salvato come cliente, con password cifrata, e inoltrato al profilo. */
    @Test
    void datiValidi_registraUtenteEInoltraAlProfilo() throws Exception {
        when(service.registraUtente(any(Utente.class))).thenReturn(10);

        servlet.doGet(request, response);

        ArgumentCaptor<Utente> captor = ArgumentCaptor.forClass(Utente.class);
        verify(service).registraUtente(captor.capture());
        Utente salvato = captor.getValue();
        assertEquals("Mario", salvato.getNome());
        assertFalse(salvato.isRuolo());
        assertNotEquals("password1", salvato.getPasswordHash());
        assertEquals(10, salvato.getIdUtente());
        verify(session).setAttribute("utente", salvato);
        verify(request).getRequestDispatcher("/WEB-INF/results/profile.jsp");
        verify(dispatcher).forward(request, response);
    }

    /** Il carrello creato da ospite viene associato all'id del nuovo utente. */
    @Test
    void carrelloDiSessione_vieneAssociatoAlNuovoUtente() throws Exception {
        ItemCarrello item = new ItemCarrello();
        item.setIdUtente(null);
        List<ItemCarrello> carrello = new ArrayList<>(List.of(item));
        when(session.getAttribute("carrello")).thenReturn(carrello);
        when(service.registraUtente(any(Utente.class))).thenReturn(10);

        servlet.doGet(request, response);

        assertEquals(10, item.getIdUtente());
        verify(session).setAttribute("carrello", carrello);
    }

    /** Senza carrello in sessione ne viene impostato uno vuoto. */
    @Test
    void carrelloAssente_vieneImpostatoCarrelloVuoto() throws Exception {
        when(service.registraUtente(any(Utente.class))).thenReturn(10);

        servlet.doGet(request, response);

        verify(session).setAttribute("carrello", new ArrayList<ItemCarrello>());
    }

    // --- Regole di validazione e casi limite ---

    /** Valore limite: una password di 7 caratteri è rifiutata. */
    @Test
    void password7Caratteri_rifiutata() {
        impostaParametri("Mario", "Rossi", "mario.rossi@test.it", "abcdefg", "2000-01-15");
        assertDatiNonValidi();
    }

    /** Valore limite: una password di 8 caratteri è accettata. */
    @Test
    void password8Caratteri_accettata() throws Exception {
        impostaParametri("Mario", "Rossi", "mario.rossi@test.it", "abcdefgh", "2000-01-15");

        servlet.doGet(request, response);

        verify(service).registraUtente(any(Utente.class));
    }

    @Test
    void passwordMancante_rifiutata() {
        impostaParametri("Mario", "Rossi", "mario.rossi@test.it", null, "2000-01-15");
        assertDatiNonValidi();
    }

    @Test
    void nomeConCifre_rifiutato() {
        impostaParametri("Mario1", "Rossi", "mario.rossi@test.it", "password1", "2000-01-15");
        assertDatiNonValidi();
    }

    @Test
    void nomeConSegnoDiMoltiplicazione_rifiutato() {
        when(request.getParameter("nome")).thenReturn("Mario×");
        assertDatiNonValidi();
    }

    @Test
    void cognomeConSegnoDiDivisione_rifiutato() {
        when(request.getParameter("cognome")).thenReturn("Rossi÷");
        assertDatiNonValidi();
    }

    @Test
    void nomeVuoto_rifiutato() {
        impostaParametri("", "Rossi", "mario.rossi@test.it", "password1", "2000-01-15");
        assertDatiNonValidi();
    }

    @Test
    void cognomeMancante_rifiutato() {
        impostaParametri("Mario", null, "mario.rossi@test.it", "password1", "2000-01-15");
        assertDatiNonValidi();
    }

    @Test
    void emailSenzaChiocciola_rifiutata() {
        impostaParametri("Mario", "Rossi", "mario.rossi.test.it", "password1", "2000-01-15");
        assertDatiNonValidi();
    }

    @Test
    void emailSenzaDominio_rifiutata() {
        impostaParametri("Mario", "Rossi", "mario.rossi@", "password1", "2000-01-15");
        assertDatiNonValidi();
    }

    /** Una data di nascita in formato diverso da ISO (aaaa-mm-gg) è rifiutata. */
    @Test
    void dataDiNascitaFormatoErrato_lanciaEccezione() {
        impostaParametri("Mario", "Rossi", "mario.rossi@test.it", "password1", "15/01/2000");

        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals("Data di nascita non valida", ex.getMessage());
        verify(service, never()).registraUtente(any());
    }

    @Test
    void dataDiNascitaMancante_lanciaEccezione() {
        impostaParametri("Mario", "Rossi", "mario.rossi@test.it", "password1", null);

        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals("Data di nascita non valida", ex.getMessage());
    }

    // --- Casi limite su nomi ed email (difetti individuati e corretti) ---

    /**
     * Un nome con lettere accentate è accettato, come avviene nel checkout.
     * In precedenza la regex "[a-zA-Z ]+" lo rifiutava.
     */
    @Test
    void nomeAccentato_accettato() throws Exception {
        impostaParametri("Niccolò", "Rossi", "niccolo.rossi@test.it", "password1", "2000-01-15");

        servlet.doGet(request, response);

        verify(service).registraUtente(any(Utente.class));
    }

    /**
     * Un'email con dominio di primo livello più lungo di 4 caratteri è accettata.
     * In precedenza la regex ammetteva al massimo 4 caratteri dopo l'ultimo punto.
     */
    @Test
    void emailConDominioLungo_accettata() throws Exception {
        impostaParametri("Mario", "Rossi", "mario@museo.museum", "password1", "2000-01-15");

        servlet.doGet(request, response);

        verify(service).registraUtente(any(Utente.class));
    }

    // --- Utente già autenticato (riga non coperta segnalata da PITest) ---

    private static Utente autenticato(boolean admin) {
        Utente utente = new Utente();
        utente.setIdUtente(5);
        utente.setRuolo(admin);
        return utente;
    }

    /** Un admin già autenticato torna alla sua area e non viene registrato di nuovo. */
    @Test
    void adminGiaAutenticato_vaAllAreaAdminSenzaRegistrare() throws Exception {
        when(session.getAttribute("utente")).thenReturn(autenticato(true));

        servlet.doGet(request, response);

        verify(request).getRequestDispatcher("/WEB-INF/results/admin.jsp");
        verify(dispatcher).forward(request, response);
        verify(service, never()).registraUtente(any());
    }

    /** Un cliente già autenticato torna al profilo e non viene registrato di nuovo. */
    @Test
    void clienteGiaAutenticato_vaAlProfiloSenzaRegistrare() throws Exception {
        when(session.getAttribute("utente")).thenReturn(autenticato(false));

        servlet.doGet(request, response);

        verify(request).getRequestDispatcher("/WEB-INF/results/profile.jsp");
        verify(dispatcher, times(1)).forward(request, response);
        verify(service, never()).registraUtente(any());
    }
}
