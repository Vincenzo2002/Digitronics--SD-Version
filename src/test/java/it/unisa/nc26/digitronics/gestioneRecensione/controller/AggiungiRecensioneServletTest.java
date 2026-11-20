package it.unisa.nc26.digitronics.gestioneRecensione.controller;

import it.unisa.nc26.digitronics.gestioneRecensione.service.RecensioneService;
import it.unisa.nc26.digitronics.model.bean.Recensione;
import it.unisa.nc26.digitronics.model.bean.Utente;
import it.unisa.nc26.digitronics.utils.MyServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test della servlet {@link AggiungiRecensioneServlet}.
 *
 * Questa classe utilizza Mockito per simulare il comportamento di servlet, richieste HTTP,
 * sessioni e servizi di business. Include vari test unitari per verificare il comportamento della servlet
 * con input validi e non validi.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AggiungiRecensioneServletTest {
    /** La servlet sotto test. */
    private AggiungiRecensioneServlet servlet;

    /** Mock della richiesta HTTP. */
    @Mock
    private HttpServletRequest request;

    /** Mock della risposta HTTP. */
    @Mock
    private HttpServletResponse response;

    /** Mock della sessione HTTP. */
    @Mock
    private HttpSession session;

    /** Mock del servizio di gestione delle recensioni. */
    @Mock
    private RecensioneService recensioneService;

    /** Mock dell'utente autenticato. */
    @Mock
    private Utente utente;

    /**
     * Configura l'ambiente di test prima di ogni esecuzione.
     */
    @BeforeEach
    public void setUp() {
        servlet = new AggiungiRecensioneServlet();
        servlet.setRecensioneService(recensioneService);
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} se l'utente non è registrato.
     */
    @Test
    public void testUtenteNonRegistrato() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(null);

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} se il titolo della recensione non è valido.
     */
    @Test
    public void testTitoloNonValido() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);

        when(request.getParameter("title")).thenReturn("");
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn("3");
        when(request.getParameter("productId")).thenReturn("1");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} se la descrizione della recensione non è valida.
     */
    @Test
    public void testDescrizioneNonValido() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);

        when(request.getParameter("title")).thenReturn("Titolo valido");
        when(request.getParameter("description")).thenReturn("");
        when(request.getParameter("score")).thenReturn("3");
        when(request.getParameter("productId")).thenReturn("1");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} se il punteggio è vuoto.
     */
    @Test
    public void testPunteggioVuoto() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);

        when(request.getParameter("title")).thenReturn("Titolo valido");
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn("");
        when(request.getParameter("productId")).thenReturn("1");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} se il punteggio è fuori dai limiti consentiti.
     */
    @Test
    public void testPunteggioNonValido() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);

        when(request.getParameter("title")).thenReturn("Titolo valido");
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn("6");
        when(request.getParameter("productId")).thenReturn("1");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} se il punteggio non è un numero.
     */
    @Test
    public void testPunteggioNonNumero() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);

        when(request.getParameter("title")).thenReturn("Titolo valido");
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn("nonNumero");
        when(request.getParameter("productId")).thenReturn("1");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} se l'ID del prodotto è vuoto.
     */
    @Test
    public void testIdProdottoVuoto() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);

        when(request.getParameter("title")).thenReturn("Titolo valido");
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn("5");
        when(request.getParameter("productId")).thenReturn("");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che venga sollevata un'eccezione {@link MyServletException} in caso di errore del database.
     */
    @Test
    public void testErroreDatabase() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);
        when(utente.getIdUtente()).thenReturn(1);

        when(request.getParameter("title")).thenReturn("Titolo valido");
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn("5");
        when(request.getParameter("productId")).thenReturn("1");

        doThrow(new SQLException()).when(recensioneService).saveRecensione(any(Recensione.class));

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /**
     * Verifica che la servlet reindirizzi correttamente in caso di parametri validi.
     */
    @Test
    public void testParametriValidi() throws Exception {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);
        when(utente.getIdUtente()).thenReturn(1);

        when(request.getParameter("title")).thenReturn("Titolo valido");
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn("5");
        when(request.getParameter("productId")).thenReturn("1");

        servlet.doGet(request, response);

        // Verifica che venga effettuato il reindirizzamento corretto
        verify(response).sendRedirect("dettagliProdotto?id=1");
    }

    // --- Valori limite (mutanti sopravvissuti a PITest) ---

    /** Parametri validi per una recensione, con titolo e punteggio indicati. */
    private void parametriRecensione(String titolo, String punteggio) {
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);
        when(utente.getIdUtente()).thenReturn(1);
        when(request.getParameter("title")).thenReturn(titolo);
        when(request.getParameter("description")).thenReturn("Descrizione valida");
        when(request.getParameter("score")).thenReturn(punteggio);
        when(request.getParameter("productId")).thenReturn("1");
    }

    /** Valore limite: un titolo di esattamente 255 caratteri è accettato. */
    @Test
    public void testTitoloDi255Caratteri() throws Exception {
        parametriRecensione("T".repeat(255), "3");

        servlet.doGet(request, response);

        verify(response).sendRedirect("dettagliProdotto?id=1");
    }

    /** Valore limite: un titolo di 256 caratteri è rifiutato. */
    @Test
    public void testTitoloDi256Caratteri() throws Exception {
        parametriRecensione("T".repeat(256), "3");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }

    /** Valore limite: il punteggio minimo 1 è accettato. */
    @Test
    public void testPunteggioMinimo() throws Exception {
        parametriRecensione("Titolo valido", "1");

        servlet.doGet(request, response);

        verify(response).sendRedirect("dettagliProdotto?id=1");
    }

    /** Valore limite: il punteggio 0 è rifiutato. */
    @Test
    public void testPunteggioZero() throws Exception {
        parametriRecensione("Titolo valido", "0");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
    }
}
