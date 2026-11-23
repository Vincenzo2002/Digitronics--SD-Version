package it.unisa.nc26.digitronics.utils;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link AdminChoiceServlet}: ogni scelta del pannello admin porta alla
 * pagina corrispondente; una scelta mancante o sconosciuta è rifiutata.
 * Le API Servlet sono sostituite da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminChoiceServletTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private RequestDispatcher dispatcher;

    private AdminChoiceServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new AdminChoiceServlet();
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
    }

    private void assertInoltroPerScelta(String scelta, String pagina) throws Exception {
        when(request.getParameter("choice")).thenReturn(scelta);

        servlet.doGet(request, response);

        verify(request).getRequestDispatcher(pagina);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void sceltaAggiungi_inoltraAllaPaginaDiAggiunta() throws Exception {
        assertInoltroPerScelta("addproduct", "/WEB-INF/results/add-product.jsp");
    }

    @Test
    void sceltaModifica_inoltraAllaPaginaDiModifica() throws Exception {
        assertInoltroPerScelta("modifyproduct", "/WEB-INF/results/modify-product.jsp");
    }

    @Test
    void sceltaElimina_inoltraAllaPaginaDiEliminazione() throws Exception {
        assertInoltroPerScelta("deleteproduct", "/WEB-INF/results/delete-product.jsp");
    }

    @Test
    void doPost_comeDoGet() throws Exception {
        when(request.getParameter("choice")).thenReturn("addproduct");

        servlet.doPost(request, response);

        verify(request).getRequestDispatcher("/WEB-INF/results/add-product.jsp");
    }

    @Test
    void sceltaMancante_rifiutata() {
        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));

        assertEquals("Errore nella richiesta", ex.getMessage());
        verifyNoInteractions(dispatcher);
    }

    @Test
    void sceltaSconosciuta_rifiutata() {
        when(request.getParameter("choice")).thenReturn("cancellatutto");

        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));

        assertEquals("Errore nella richiesta", ex.getMessage());
        verifyNoInteractions(dispatcher);
    }
}
