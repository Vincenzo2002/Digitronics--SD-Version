package it.unisa.nc26.digitronics.infoProdotto.controller;

import it.unisa.nc26.digitronics.infoProdotto.service.InfoProdottoService;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link ProdottoRicercaServlet}: la ricerca restituisce i prodotti trovati
 * in JSON; un errore del service diventa un errore della servlet.
 * Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProdottoRicercaServletTest {

    @Mock private InfoProdottoService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private ProdottoRicercaServlet servlet;
    private StringWriter corpoRisposta;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new ProdottoRicercaServlet();
        servlet.setProductService(service);
        corpoRisposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(corpoRisposta));
        when(request.getParameter("query")).thenReturn("iphone");
    }

    private static Prodotto prodotto() {
        Prodotto p = new Prodotto();
        p.setIdProdotto(3);
        p.setNome("iPhone 15");
        p.setDescrizione("Smartphone Apple");
        p.setPrezzo(999.99);
        p.setVetrina(true);
        p.setImmagine("images/iphone.png");
        p.setQuantità(7);
        p.setNomeCategoria("Smartphone");
        return p;
    }

    private JSONArray rispostaJson() throws Exception {
        return (JSONArray) new JSONParser().parse(corpoRisposta.toString());
    }

    /** Percorso critico: ogni prodotto trovato diventa un oggetto JSON con tutti i suoi campi. */
    @Test
    void prodottiTrovati_restituisceJsonCompleto() throws Exception {
        when(service.cercaProdottoPerNome("iphone")).thenReturn(List.of(prodotto()));

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
        JSONArray risultati = rispostaJson();
        assertEquals(1, risultati.size());
        JSONObject json = (JSONObject) risultati.get(0);
        assertEquals(3L, json.get("idProdotto"));
        assertEquals("iPhone 15", json.get("nome"));
        assertEquals("Smartphone Apple", json.get("descrizione"));
        assertEquals(999.99, (Double) json.get("prezzo"), 0.001);
        assertEquals(true, json.get("vetrina"));
        assertEquals("images/iphone.png", json.get("immagine"));
        assertEquals(7L, json.get("quantita"));
        assertEquals("Smartphone", json.get("nomecategoria"));
    }

    /** Nessun risultato: la risposta è un array JSON vuoto. */
    @Test
    void nessunProdotto_restituisceArrayVuoto() throws Exception {
        when(service.cercaProdottoPerNome("iphone")).thenReturn(List.of());

        servlet.doGet(request, response);

        assertTrue(rispostaJson().isEmpty());
    }

    /** doPost delega a doGet. */
    @Test
    void doPost_comeDoGet() throws Exception {
        when(service.cercaProdottoPerNome("iphone")).thenReturn(List.of());

        servlet.doPost(request, response);

        assertEquals("[]", corpoRisposta.toString());
    }

    @Test
    void erroreDelService_diventaServletException() {
        RuntimeException causa = new RuntimeException("database non raggiungibile");
        when(service.cercaProdottoPerNome("iphone")).thenThrow(causa);

        ServletException ex = assertThrows(ServletException.class, () -> servlet.doGet(request, response));
        assertEquals("Errore nella ricerca dei prodotti", ex.getMessage());
        assertSame(causa, ex.getCause());
    }
}
