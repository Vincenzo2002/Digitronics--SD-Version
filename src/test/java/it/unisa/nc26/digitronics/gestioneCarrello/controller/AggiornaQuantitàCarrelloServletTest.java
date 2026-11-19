package it.unisa.nc26.digitronics.gestioneCarrello.controller;

import it.unisa.nc26.digitronics.gestioneCarrello.service.GestioneCarrelloService;
import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link AggiornaQuantitàCarrelloServlet}: la nuova quantità viene
 * limitata tra 1 e la scorta disponibile, e gli errori restituiscono 400 con un
 * messaggio JSON. Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AggiornaQuantitàCarrelloServletTest {

    private static final int ID_PRODOTTO = 5;
    private static final int SCORTA = 5;

    @Mock private GestioneCarrelloService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    private AggiornaQuantitàCarrelloServlet servlet;
    private StringWriter corpoRisposta;
    private List<ItemCarrello> carrello;
    private ItemCarrello item;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new AggiornaQuantitàCarrelloServlet();
        servlet.setGestioneCarrelloService(service);

        Prodotto prodotto = new Prodotto();
        prodotto.setIdProdotto(ID_PRODOTTO);
        prodotto.setQuantità(SCORTA);

        item = new ItemCarrello();
        item.setIdProdotto(ID_PRODOTTO);
        item.setQuantità(1);
        carrello = new ArrayList<>(List.of(item));

        corpoRisposta = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(corpoRisposta));
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("carrello")).thenReturn(carrello);
        when(service.fetchByIdProdotto(ID_PRODOTTO)).thenReturn(prodotto);
        impostaParametri(String.valueOf(ID_PRODOTTO), "3");
    }

    private void impostaParametri(String idProdotto, String nuovaQuantita) {
        when(request.getParameter("idProdotto")).thenReturn(idProdotto);
        when(request.getParameter("nuovaQuantita")).thenReturn(nuovaQuantita);
    }

    // --- Percorso critico ---

    /** Una quantità valida aggiorna l'item e viene restituita in JSON. */
    @Test
    void quantitaValida_aggiornaItemERestituisceJson() throws Exception {
        servlet.doGet(request, response);

        assertEquals(3, item.getQuantità());
        assertEquals("{\"nuovaQuantita\":3}", corpoRisposta.toString());
        verify(response, never()).setStatus(anyInt());
        verify(session).setAttribute("carrello", carrello);
    }

    // --- Regole di business e casi limite ---

    /** Valore limite: una quantità pari alla scorta resta invariata. */
    @Test
    void quantitaUgualeAllaScorta_accettata() throws Exception {
        impostaParametri(String.valueOf(ID_PRODOTTO), String.valueOf(SCORTA));

        servlet.doGet(request, response);

        assertEquals(SCORTA, item.getQuantità());
    }

    /** Una quantità oltre la scorta viene ridotta alla scorta disponibile. */
    @Test
    void quantitaOltreLaScorta_limitataAllaScorta() throws Exception {
        impostaParametri(String.valueOf(ID_PRODOTTO), String.valueOf(SCORTA + 1));

        servlet.doGet(request, response);

        assertEquals(SCORTA, item.getQuantità());
        assertEquals("{\"nuovaQuantita\":" + SCORTA + "}", corpoRisposta.toString());
    }

    /** Valore limite: una quantità pari a zero viene portata a 1. */
    @Test
    void quantitaZero_portataAUno() throws Exception {
        impostaParametri(String.valueOf(ID_PRODOTTO), "0");

        servlet.doGet(request, response);

        assertEquals(1, item.getQuantità());
    }

    @Test
    void quantitaNegativa_portataAUno() throws Exception {
        item.setQuantità(4);
        impostaParametri(String.valueOf(ID_PRODOTTO), "-3");

        servlet.doGet(request, response);

        assertEquals(1, item.getQuantità());
    }

    // --- Condizioni di errore ---

    @Test
    void prodottoInesistente_rispondeBadRequest() throws Exception {
        when(service.fetchByIdProdotto(9)).thenReturn(null);
        impostaParametri("9", "3");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(corpoRisposta.toString().contains("Prodotto non esistente"));
        assertEquals(1, item.getQuantità());
    }

    @Test
    void prodottoNonNelCarrello_rispondeBadRequest() throws Exception {
        carrello.clear();

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(corpoRisposta.toString().contains("Prodotto non trovato nel carrello"));
    }

    @Test
    void quantitaNonNumerica_rispondeBadRequestSenzaModifiche() throws Exception {
        impostaParametri(String.valueOf(ID_PRODOTTO), "tre");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertEquals(1, item.getQuantità());
    }

    @Test
    void idProdottoNonNumerico_rispondeBadRequest() throws Exception {
        impostaParametri("abc", "3");

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(service, never()).fetchByIdProdotto(anyInt());
    }

    @Test
    void carrelloAssenteInSessione_rispondeBadRequest() throws Exception {
        when(session.getAttribute("carrello")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
}
