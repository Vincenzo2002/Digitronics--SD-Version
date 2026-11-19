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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
 * Test di unità di {@link LogoutServlet}: al logout del cliente il carrello di sessione
 * sostituisce quello salvato, poi la sessione viene invalidata.
 * Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LogoutServletTest {

    @Mock private AutenticazioneService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private LogoutServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new LogoutServlet();
        servlet.setAutenticazioneService(service);
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    private static Utente utente(int id, boolean admin) {
        Utente utente = new Utente();
        utente.setIdUtente(id);
        utente.setRuolo(admin);
        return utente;
    }

    private static ItemCarrello item(int idProdotto, int quantita) {
        ItemCarrello item = new ItemCarrello();
        item.setIdProdotto(idProdotto);
        item.setQuantità(quantita);
        return item;
    }

    private void assertSessioneChiusa() throws Exception {
        verify(session).invalidate();
        verify(request).getRequestDispatcher("index.html");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void utenteNonAutenticato_lanciaEccezione() throws Exception {
        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));

        assertEquals("Utente non registrato: non puoi fare il logout", ex.getMessage());
        verify(session, never()).invalidate();
        verifyNoInteractions(service);
    }

    /** Percorso critico: il carrello salvato viene svuotato e sostituito da quello di sessione. */
    @Test
    void clienteConCarrello_salvaIlCarrelloEChiudeLaSessione() throws Exception {
        when(session.getAttribute("utente")).thenReturn(utente(7, false));
        when(session.getAttribute("carrello")).thenReturn(new ArrayList<>(List.of(item(1, 2), item(3, 1))));

        servlet.doGet(request, response);

        InOrder ordine = inOrder(service);
        ordine.verify(service).clearCarrelloUtente(7);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemCarrello>> captor = ArgumentCaptor.forClass(List.class);
        ordine.verify(service).saveCarrelloUtente(captor.capture());
        List<ItemCarrello> salvato = captor.getValue();
        assertEquals(2, salvato.size());
        assertEquals(7, salvato.get(0).getIdUtente());
        assertEquals(1, salvato.get(0).getIdProdotto());
        assertEquals(2, salvato.get(0).getQuantità());
        assertEquals(7, salvato.get(1).getIdUtente());
        assertEquals(3, salvato.get(1).getIdProdotto());
        assertSessioneChiusa();
    }

    @Test
    void clienteConCarrelloVuoto_svuotaSoloQuelloSalvato() throws Exception {
        when(session.getAttribute("utente")).thenReturn(utente(7, false));
        when(session.getAttribute("carrello")).thenReturn(new ArrayList<ItemCarrello>());

        servlet.doGet(request, response);

        verify(service).clearCarrelloUtente(7);
        verify(service, never()).saveCarrelloUtente(any());
        assertSessioneChiusa();
    }

    @Test
    void clienteSenzaCarrelloInSessione_svuotaSoloQuelloSalvato() throws Exception {
        when(session.getAttribute("utente")).thenReturn(utente(7, false));

        servlet.doGet(request, response);

        verify(service).clearCarrelloUtente(7);
        verify(service, never()).saveCarrelloUtente(any());
        assertSessioneChiusa();
    }

    /** L'admin non ha un carrello: il logout chiude solo la sessione. */
    @Test
    void admin_chiudeLaSessioneSenzaToccareIlCarrello() throws Exception {
        when(session.getAttribute("utente")).thenReturn(utente(1, true));

        servlet.doGet(request, response);

        verifyNoInteractions(service);
        assertSessioneChiusa();
    }
}
