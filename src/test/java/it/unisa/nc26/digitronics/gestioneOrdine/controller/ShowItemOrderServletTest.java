package it.unisa.nc26.digitronics.gestioneOrdine.controller;

import it.unisa.nc26.digitronics.gestioneOrdine.service.GestioneOrdineService;
import it.unisa.nc26.digitronics.model.bean.ItemOrdine;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowItemOrderServletTest {

    private static final int ID_UTENTE = 3;
    private static final int ID_ORDINE = 5;
    private static final String PAGINA_DETTAGLI = "/WEB-INF/results/showItemsOrder.jsp";

    @Mock private GestioneOrdineService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private ShowItemOrderServlet servlet;
    private Utente utente;
    private Ordine ordine;

    @BeforeEach
    void setUp() {
        servlet = new ShowItemOrderServlet();
        servlet.setOrderService(service);

        utente = new Utente();
        utente.setIdUtente(ID_UTENTE);
        ordine = new Ordine();
        ordine.setIdOrdine(ID_ORDINE);

        when(request.getParameter("idOrdine")).thenReturn(String.valueOf(ID_ORDINE));
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("utente")).thenReturn(utente);
    }

    @Test
    void clienteVisualizzaIlProprioOrdine() throws Exception {
        ordine.setIdUtente(ID_UTENTE);
        List<ItemOrdine> articoli = List.of(new ItemOrdine());
        when(service.fetchByIdUtente(ID_UTENTE)).thenReturn(List.of(ordine));
        when(service.fetchByIdOrder(ID_ORDINE)).thenReturn(ordine);
        when(service.fetchItemOrder(ID_ORDINE)).thenReturn(articoli);
        when(request.getRequestDispatcher(PAGINA_DETTAGLI)).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute("itemOrdineList", articoli);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void clienteNonVisualizzaOrdiniDiAltriUtenti() throws Exception {
        ordine.setIdUtente(ID_UTENTE + 1);
        Ordine ordineProprio = new Ordine();
        ordineProprio.setIdOrdine(ID_ORDINE + 1);
        ordineProprio.setIdUtente(ID_UTENTE);
        when(service.fetchByIdUtente(ID_UTENTE)).thenReturn(List.of(ordineProprio));
        when(service.fetchByIdOrder(ID_ORDINE)).thenReturn(ordine);

        MyServletException errore = assertThrows(MyServletException.class,
                () -> servlet.doGet(request, response));

        assertEquals("Non puoi visualizzare questo ordine perchè non fa parte dei tuoi ordini", errore.getMessage());
        verify(service, never()).fetchItemOrder(anyInt());
        verify(request, never()).getRequestDispatcher(anyString());
    }

    @Test
    void amministratoreVisualizzaDettagliSenzaControllareLaProprieta() throws Exception {
        utente.setRuolo(true);
        List<ItemOrdine> articoli = List.of(new ItemOrdine());
        when(service.fetchItemOrder(ID_ORDINE)).thenReturn(articoli);
        when(request.getRequestDispatcher(PAGINA_DETTAGLI)).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(service, never()).fetchByIdUtente(anyInt());
        verify(request).setAttribute("itemOrdineList", articoli);
        verify(dispatcher).forward(request, response);
    }
}
