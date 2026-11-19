package it.unisa.nc26.digitronics.autenticazione.controller;

import it.unisa.nc26.digitronics.autenticazione.service.AutenticazioneService;
import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
import it.unisa.nc26.digitronics.model.bean.Utente;
import it.unisa.nc26.digitronics.utils.MyServletException;
import jakarta.servlet.RequestDispatcher;
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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link AutenticazioneServlet}: instradamento in base allo stato
 * dell'utente, login e unione del carrello di sessione con quello salvato.
 * Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AutenticazioneServletTest {

    private static final String LOGIN = "/WEB-INF/results/login.jsp";
    private static final String PROFILO = "/WEB-INF/results/profile.jsp";
    private static final String ADMIN = "/WEB-INF/results/admin.jsp";

    @Mock private AutenticazioneService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private AutenticazioneServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new AutenticazioneServlet();
        servlet.setAutenticazioneService(service);
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(request.getParameter("email")).thenReturn("mario@test.it");
        when(request.getParameter("password")).thenReturn("password1");
    }

    private static Utente utente(int id, boolean admin) {
        Utente utente = new Utente();
        utente.setIdUtente(id);
        utente.setRuolo(admin);
        return utente;
    }

    private static ItemCarrello item(Integer idUtente, int idProdotto, int quantita) {
        ItemCarrello item = new ItemCarrello();
        item.setIdUtente(idUtente);
        item.setIdProdotto(idProdotto);
        item.setQuantità(quantita);
        return item;
    }

    private void assertInoltroA(String pagina) throws Exception {
        verify(request).getRequestDispatcher(pagina);
        verify(dispatcher).forward(request, response);
    }

    // --- Utente già autenticato e richieste di pagina ---

    @Test
    void adminGiaAutenticato_vaAllaPaginaAdmin() throws Exception {
        when(session.getAttribute("utente")).thenReturn(utente(1, true));

        servlet.doGet(request, response);

        assertInoltroA(ADMIN);
        verifyNoInteractions(service);
    }

    @Test
    void clienteGiaAutenticato_vaAlProfilo() throws Exception {
        when(session.getAttribute("utente")).thenReturn(utente(2, false));

        servlet.doGet(request, response);

        assertInoltroA(PROFILO);
        verifyNoInteractions(service);
    }

    @Test
    void azioneLogin_mostraLaPaginaDiLogin() throws Exception {
        when(request.getParameter("action")).thenReturn("login");

        servlet.doGet(request, response);

        assertInoltroA(LOGIN);
        verifyNoInteractions(service);
    }

    @Test
    void azioneRegister_mostraLaPaginaDiRegistrazione() throws Exception {
        when(request.getParameter("action")).thenReturn("register");

        servlet.doGet(request, response);

        assertInoltroA("/WEB-INF/results/register.jsp");
    }

    // --- Login: errori ---

    @Test
    void emailMancante_lanciaEccezione() {
        when(request.getParameter("email")).thenReturn(null);

        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals("Parametri in input non validi", ex.getMessage());
        verifyNoInteractions(service);
    }

    @Test
    void passwordMancante_lanciaEccezione() {
        when(request.getParameter("password")).thenReturn(null);

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        verifyNoInteractions(service);
    }

    /** Con credenziali errate si torna al login e non si legge alcun carrello. */
    @Test
    void credenzialiErrate_tornaAlLogin() throws Exception {
        when(service.retrieveUtenteEmailPassword("mario@test.it", "password1")).thenReturn(null);

        servlet.doGet(request, response);

        assertInoltroA(LOGIN);
        verify(service, never()).retrieveCarrelloUtente(anyInt());
    }

    // --- Login: percorsi critici ---

    @Test
    void loginAdmin_vaAllaPaginaAdminSenzaCarrello() throws Exception {
        Utente admin = utente(1, true);
        when(service.retrieveUtenteEmailPassword("mario@test.it", "password1")).thenReturn(admin);

        servlet.doGet(request, response);

        verify(session).setAttribute("utente", admin);
        assertInoltroA(ADMIN);
        verify(service, never()).retrieveCarrelloUtente(anyInt());
    }

    /**
     * Al login del cliente il carrello di sessione viene unito a quello salvato:
     * le quantità dello stesso prodotto si sommano, i prodotti nuovi vengono aggiunti
     * con l'id dell'utente.
     */
    @Test
    void loginCliente_unisceCarrelloDiSessioneEQuelloSalvato() throws Exception {
        Utente cliente = utente(7, false);
        when(service.retrieveUtenteEmailPassword("mario@test.it", "password1")).thenReturn(cliente);
        List<ItemCarrello> carrelloSalvato = new ArrayList<>(List.of(item(7, 1, 2)));
        when(service.retrieveCarrelloUtente(7)).thenReturn(carrelloSalvato);
        ItemCarrello nuovo = item(null, 2, 3);
        List<ItemCarrello> carrelloSessione = new ArrayList<>(List.of(item(null, 1, 1), nuovo));
        when(session.getAttribute("carrello")).thenReturn(carrelloSessione);

        servlet.doGet(request, response);

        assertEquals(2, carrelloSalvato.size());
        assertEquals(3, carrelloSalvato.get(0).getQuantità());
        assertSame(nuovo, carrelloSalvato.get(1));
        assertEquals(7, nuovo.getIdUtente());
        verify(session).setAttribute("carrello", carrelloSalvato);
        assertInoltroA(PROFILO);
    }

    /** Senza carrello di sessione resta il carrello salvato. */
    @Test
    void loginCliente_senzaCarrelloDiSessione_usaQuelloSalvato() throws Exception {
        Utente cliente = utente(7, false);
        when(service.retrieveUtenteEmailPassword("mario@test.it", "password1")).thenReturn(cliente);
        List<ItemCarrello> carrelloSalvato = new ArrayList<>(List.of(item(7, 1, 2)));
        when(service.retrieveCarrelloUtente(7)).thenReturn(carrelloSalvato);

        servlet.doGet(request, response);

        assertEquals(1, carrelloSalvato.size());
        assertEquals(2, carrelloSalvato.get(0).getQuantità());
        verify(session).setAttribute("carrello", carrelloSalvato);
    }
}
