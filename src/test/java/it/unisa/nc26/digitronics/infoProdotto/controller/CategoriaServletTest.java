package it.unisa.nc26.digitronics.infoProdotto.controller;

import it.unisa.nc26.digitronics.infoProdotto.service.InfoProdottoService;
import it.unisa.nc26.digitronics.model.bean.Categoria;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
import it.unisa.nc26.digitronics.utils.MyServletException;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link CategoriaServlet}: la pagina di una categoria mostra i suoi prodotti
 * e l'elenco delle categorie; nome mancante o categoria inesistente sono rifiutati.
 * Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CategoriaServletTest {

    @Mock private InfoProdottoService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private RequestDispatcher dispatcher;

    private CategoriaServlet servlet;
    private Categoria smartphone;

    @BeforeEach
    void setUp() {
        servlet = new CategoriaServlet();
        servlet.setCategoryService(service);
        smartphone = new Categoria();
        smartphone.setNome("Smartphone");
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(request.getParameter("categoria")).thenReturn("Smartphone");
        when(service.getCategoriaPerNome("Smartphone")).thenReturn(smartphone);
    }

    private void assertErrore(String messaggio) {
        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals(messaggio, ex.getMessage());
        verify(service, never()).getProdottiPerCategoria(anyString());
    }

    /** Percorso critico: categoria esistente, con prodotti e categorie passati alla JSP. */
    @Test
    void categoriaEsistente_mostraIProdotti() throws Exception {
        List<Prodotto> prodotti = List.of(new Prodotto(), new Prodotto());
        List<Categoria> categorie = List.of(smartphone, new Categoria());
        when(service.getProdottiPerCategoria("Smartphone")).thenReturn(prodotti);
        when(service.getAllCategorie()).thenReturn(categorie);

        servlet.doGet(request, response);

        verify(request).setAttribute("prodottiPerCategoria", prodotti);
        verify(request).setAttribute("categoriaScelta", smartphone);
        verify(request).setAttribute("categorie", categorie);
        verify(request).getRequestDispatcher("/WEB-INF/results/category.jsp");
        verify(dispatcher).forward(request, response);
    }

    /** doPost delega a doGet. */
    @Test
    void doPost_comeDoGet() throws Exception {
        servlet.doPost(request, response);

        verify(dispatcher).forward(request, response);
    }

    @Test
    void parametroMancante_rifiutato() {
        when(request.getParameter("categoria")).thenReturn(null);
        assertErrore("Parametro categoria non valido");
    }

    @Test
    void parametroVuoto_rifiutato() {
        when(request.getParameter("categoria")).thenReturn("");
        assertErrore("Parametro categoria non valido");
    }

    @Test
    void categoriaInesistente_rifiutata() {
        when(request.getParameter("categoria")).thenReturn("Inesistente");
        when(service.getCategoriaPerNome("Inesistente")).thenReturn(null);
        assertErrore("La categoria non è stata trovata");
    }
}
