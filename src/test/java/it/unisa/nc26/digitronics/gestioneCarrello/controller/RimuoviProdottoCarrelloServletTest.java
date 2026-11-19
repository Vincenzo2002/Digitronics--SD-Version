package it.unisa.nc26.digitronics.gestioneCarrello.controller;

import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
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
 * Test di unità di {@link RimuoviProdottoCarrelloServlet}: rimozione di un prodotto
 * dal carrello di sessione e gestione degli id non validi. Le API Servlet sono mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RimuoviProdottoCarrelloServletTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private RimuoviProdottoCarrelloServlet servlet;
    private List<ItemCarrello> carrello;

    @BeforeEach
    void setUp() {
        servlet = new RimuoviProdottoCarrelloServlet();
        carrello = new ArrayList<>(List.of(item(1), item(2)));
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(session.getAttribute("carrello")).thenReturn(carrello);
    }

    private static ItemCarrello item(int idProdotto) {
        ItemCarrello item = new ItemCarrello();
        item.setIdProdotto(idProdotto);
        item.setQuantità(1);
        return item;
    }

    private void assertInoltroAlCarrello() throws Exception {
        verify(request).getRequestDispatcher("/WEB-INF/results/cart.jsp");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void prodottoNelCarrello_vieneRimosso() throws Exception {
        when(request.getParameter("idProdotto")).thenReturn("1");

        servlet.doGet(request, response);

        assertEquals(1, carrello.size());
        assertEquals(2, carrello.get(0).getIdProdotto());
        verify(session).setAttribute("carrello", carrello);
        assertInoltroAlCarrello();
    }

    @Test
    void prodottoNonNelCarrello_carrelloInvariato() throws Exception {
        when(request.getParameter("idProdotto")).thenReturn("9");

        servlet.doGet(request, response);

        assertEquals(2, carrello.size());
        assertInoltroAlCarrello();
    }

    /** Senza carrello in sessione si mostra il carrello senza leggere l'id. */
    @Test
    void carrelloAssente_inoltraSenzaErrori() throws Exception {
        when(session.getAttribute("carrello")).thenReturn(null);

        servlet.doGet(request, response);

        verify(request, never()).getParameter("idProdotto");
        assertInoltroAlCarrello();
    }

    @Test
    void idNonNumerico_lanciaEccezione() {
        when(request.getParameter("idProdotto")).thenReturn("abc");

        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals("Errore in idProdotto: non è un numero!!", ex.getMessage());
        assertEquals(2, carrello.size());
    }

    @Test
    void idMancante_lanciaEccezione() {
        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals(2, carrello.size());
    }
}
