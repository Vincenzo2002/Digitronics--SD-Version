package it.unisa.nc26.digitronics.gestioneCarrello.controller;

import it.unisa.nc26.digitronics.gestioneCarrello.service.GestioneCarrelloService;
import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MostraCarrelloServletTest {

    private static final String PAGINA_CARRELLO = "/WEB-INF/results/cart.jsp";

    @Mock private GestioneCarrelloService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private MostraCarrelloServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new MostraCarrelloServlet();
        servlet.setGestioneCarrelloService(service);
        when(request.getSession()).thenReturn(session);
    }

    @Test
    void clienteConScortaRidotta_riduceSoloLaQuantitaOltreLaDisponibilita() throws Exception {
        ItemCarrello conScortaRidotta = itemCarrello(5, 4);
        ItemCarrello conScortaSufficiente = itemCarrello(6, 2);
        List<ItemCarrello> carrello = List.of(conScortaRidotta, conScortaSufficiente);
        when(session.getAttribute("utente")).thenReturn(new Utente());
        when(session.getAttribute("carrello")).thenReturn(carrello);
        when(service.fetchByIdProdotto(5)).thenReturn(prodottoConScorta(1));
        when(service.fetchByIdProdotto(6)).thenReturn(prodottoConScorta(3));
        when(request.getRequestDispatcher(PAGINA_CARRELLO)).thenReturn(dispatcher);

        servlet.doGet(request, response);

        assertEquals(1, conScortaRidotta.getQuantità());
        assertEquals(2, conScortaSufficiente.getQuantità());
        verify(session).setAttribute("carrello", carrello);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void ospiteSenzaCarrello_inizializzaUnCarrelloVuoto() throws Exception {
        when(request.getRequestDispatcher(PAGINA_CARRELLO)).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(session).setAttribute("carrello", List.of());
        verify(dispatcher).forward(request, response);
        verifyNoInteractions(service);
    }

    @Test
    void amministratore_nonAccedeAlCarrello() {
        Utente amministratore = new Utente();
        amministratore.setRuolo(true);
        when(session.getAttribute("utente")).thenReturn(amministratore);

        MyServletException errore = assertThrows(MyServletException.class,
                () -> servlet.doGet(request, response));

        assertEquals("L'admin non può interagire con il carrello", errore.getMessage());
        verifyNoInteractions(service);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    private static ItemCarrello itemCarrello(int idProdotto, int quantita) {
        ItemCarrello item = new ItemCarrello();
        item.setIdProdotto(idProdotto);
        item.setQuantità(quantita);
        return item;
    }

    private static Prodotto prodottoConScorta(int scorta) {
        Prodotto prodotto = new Prodotto();
        prodotto.setQuantità(scorta);
        return prodotto;
    }
}
