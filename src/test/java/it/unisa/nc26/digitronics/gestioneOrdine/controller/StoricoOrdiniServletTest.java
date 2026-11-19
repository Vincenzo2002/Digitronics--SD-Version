package it.unisa.nc26.digitronics.gestioneOrdine.controller;

import it.unisa.nc26.digitronics.gestioneOrdine.service.GestioneOrdineService;
import it.unisa.nc26.digitronics.model.bean.Ordine;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoricoOrdiniServletTest {

    private static final int ID_CLIENTE = 3;
    private static final String PAGINA_STORICO = "/WEB-INF/results/storicoOrdini.jsp";

    @Mock private GestioneOrdineService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private StoricoOrdiniServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new StoricoOrdiniServlet();
        servlet.setOrderService(service);
        when(request.getSession()).thenReturn(session);
    }

    @Test
    void clienteVisualizzaSoloIPropriOrdini() throws Exception {
        Utente cliente = new Utente();
        cliente.setIdUtente(ID_CLIENTE);
        List<Ordine> ordini = List.of(ordine(5, ID_CLIENTE));
        when(session.getAttribute("utente")).thenReturn(cliente);
        when(service.fetchByIdUtente(ID_CLIENTE)).thenReturn(ordini);
        when(request.getRequestDispatcher(PAGINA_STORICO)).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(service).fetchByIdUtente(ID_CLIENTE);
        verify(service, never()).fetchAllOrders();
        verify(request).setAttribute("ordini", ordini);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void amministratoreVisualizzaGliOrdiniDiTuttiGliUtenti() throws Exception {
        Utente amministratore = new Utente();
        amministratore.setRuolo(true);
        List<Ordine> ordini = List.of(ordine(5, ID_CLIENTE), ordine(6, ID_CLIENTE + 1));
        when(session.getAttribute("utente")).thenReturn(amministratore);
        when(service.fetchAllOrders()).thenReturn(ordini);
        when(request.getRequestDispatcher(PAGINA_STORICO)).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(service).fetchAllOrders();
        verify(service, never()).fetchByIdUtente(anyInt());
        verify(request).setAttribute("ordini", ordini);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void utenteNonAutenticatoNonAccedeAlloStorico() {
        when(session.getAttribute("utente")).thenReturn(null);

        MyServletException errore = assertThrows(MyServletException.class,
                () -> servlet.doGet(request, response));

        assertEquals("Non puoi accedere a questa pagina. Accedi per continuare", errore.getMessage());
        verifyNoInteractions(service);
        verify(request, never()).getRequestDispatcher(anyString());
    }

    private static Ordine ordine(int idOrdine, int idUtente) {
        Ordine ordine = new Ordine();
        ordine.setIdOrdine(idOrdine);
        ordine.setIdUtente(idUtente);
        return ordine;
    }
}
