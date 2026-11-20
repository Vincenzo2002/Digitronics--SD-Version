package it.unisa.nc26.digitronics.infoProdotto.controller;

import it.unisa.nc26.digitronics.infoProdotto.service.InfoProdottoService;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
import it.unisa.nc26.digitronics.model.bean.Recensione;
import it.unisa.nc26.digitronics.utils.MyServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link DettagliProdottoServlet}: la pagina di dettaglio mostra il prodotto
 * con le sue recensioni; id mancante, non numerico o inesistente ed errori del database
 * sono rifiutati. Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DettagliProdottoServletTest {

    private static final String PAGINA = "/WEB-INF/results/dettagliProdotto.jsp";

    @Mock private InfoProdottoService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private ServletConfig config;
    @Mock private ServletContext context;
    @Mock private RequestDispatcher dispatcher;

    private DettagliProdottoServlet servlet;
    private Prodotto prodotto;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new DettagliProdottoServlet();
        servlet.setProdottoService(service);
        when(config.getServletContext()).thenReturn(context);
        servlet.init(config);
        when(context.getRequestDispatcher(anyString())).thenReturn(dispatcher);

        prodotto = new Prodotto();
        prodotto.setIdProdotto(5);
        when(request.getParameter("id")).thenReturn("5");
        when(service.fetchByIdProdotto(5)).thenReturn(prodotto);
    }

    private void assertErrore(String messaggio) {
        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals(messaggio, ex.getMessage());
        verifyNoInteractions(dispatcher);
    }

    /** Percorso critico: prodotto esistente, mostrato con le sue recensioni. */
    @Test
    void prodottoEsistente_mostraDettagliERecensioni() throws Exception {
        List<Recensione> recensioni = List.of(new Recensione());
        when(service.fetchRecensioniByIdProdotto(5)).thenReturn(recensioni);

        servlet.doGet(request, response);

        verify(request).setAttribute("product", prodotto);
        verify(request).setAttribute("recensioni", recensioni);
        verify(context).getRequestDispatcher(PAGINA);
        verify(dispatcher).forward(request, response);
    }

    /** doPost delega a doGet. */
    @Test
    void doPost_comeDoGet() throws Exception {
        servlet.doPost(request, response);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void idMancante_rifiutato() {
        when(request.getParameter("id")).thenReturn(null);
        assertErrore("Id prodotto non può essere vuoto!");
    }

    @Test
    void idNonNumerico_rifiutato() {
        when(request.getParameter("id")).thenReturn("abc");
        assertErrore("Id prodotto deve essere un numero!");
    }

    @Test
    void prodottoInesistente_rifiutato() {
        when(service.fetchByIdProdotto(5)).thenReturn(null);
        assertErrore("Id prodotto non valido");
    }

    /** Un errore del database sulle recensioni diventa un errore della servlet con il suo messaggio. */
    @Test
    void erroreDatabase_rifiutato() throws Exception {
        when(service.fetchRecensioniByIdProdotto(5)).thenThrow(new SQLException("connessione persa"));
        assertErrore("Errore database:connessione persa");
    }
}
