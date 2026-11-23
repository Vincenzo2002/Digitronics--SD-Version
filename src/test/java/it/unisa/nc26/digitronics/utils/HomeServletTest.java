package it.unisa.nc26.digitronics.utils;

import it.unisa.nc26.digitronics.infoProdotto.service.InfoProdottoService;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link HomeServlet}: la home mostra i prodotti in vetrina.
 * Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
class HomeServletTest {

    @Mock private InfoProdottoService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private RequestDispatcher dispatcher;

    private HomeServlet servlet;
    private List<Prodotto> vetrina;

    @BeforeEach
    void setUp() {
        servlet = new HomeServlet();
        servlet.setInfoProdottoService(service);
        vetrina = List.of(new Prodotto(), new Prodotto());
        when(service.fetchProdottoVetrina()).thenReturn(vetrina);
        when(request.getRequestDispatcher("/WEB-INF/results/home.jsp")).thenReturn(dispatcher);
    }

    @Test
    void doGet_mostraIProdottiInVetrina() throws Exception {
        servlet.doGet(request, response);

        verify(request).setAttribute("prodottiVetrina", vetrina);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void doPost_comeDoGet() throws Exception {
        servlet.doPost(request, response);

        verify(request).setAttribute("prodottiVetrina", vetrina);
        verify(dispatcher).forward(request, response);
    }
}
