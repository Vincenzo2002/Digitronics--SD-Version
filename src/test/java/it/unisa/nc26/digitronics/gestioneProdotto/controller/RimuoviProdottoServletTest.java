package it.unisa.nc26.digitronics.gestioneProdotto.controller;

import it.unisa.nc26.digitronics.gestioneProdotto.service.GestioneProdottoService;
import it.unisa.nc26.digitronics.infoProdotto.service.InfoProdottoService;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link RimuoviProdottoServlet}: solo l'admin può eliminare un prodotto,
 * che deve esistere. I service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RimuoviProdottoServletTest {

    @Mock private GestioneProdottoService gestioneProdottoService;
    @Mock private InfoProdottoService infoProdottoService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private RimuoviProdottoServlet servlet;

    @BeforeEach
    void setUp() {
        servlet = new RimuoviProdottoServlet();
        servlet.setProdottoService(gestioneProdottoService);
        servlet.setInfoProdottoService(infoProdottoService);
        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(session.getAttribute("utente")).thenReturn(utente(true));
        when(request.getParameter("id")).thenReturn("5");
        when(infoProdottoService.fetchByIdProdotto(5)).thenReturn(new Prodotto());
    }

    private static Utente utente(boolean admin) {
        Utente utente = new Utente();
        utente.setRuolo(admin);
        return utente;
    }

    private void assertErrore(String messaggio) {
        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals(messaggio, ex.getMessage());
        verify(gestioneProdottoService, never()).eliminaProdotto(anyInt());
    }

    /** Percorso critico: l'admin elimina un prodotto esistente e vede la conferma. */
    @Test
    void adminProdottoEsistente_eliminaEConferma() throws Exception {
        servlet.doGet(request, response);

        verify(gestioneProdottoService).eliminaProdotto(5);
        verify(request).setAttribute("successMessage", "Prodotto eliminato con successo");
        verify(request).getRequestDispatcher("WEB-INF/results/confirm-operation.jsp");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void utenteNonAutenticato_rifiutato() {
        when(session.getAttribute("utente")).thenReturn(null);
        assertErrore("Non è possibile accedere a questa pagina");
    }

    @Test
    void cliente_rifiutato() {
        when(session.getAttribute("utente")).thenReturn(utente(false));
        assertErrore("Non è possibile accedere a questa pagina");
    }

    @Test
    void prodottoInesistente_rifiutato() {
        when(infoProdottoService.fetchByIdProdotto(5)).thenReturn(null);
        assertErrore("Prodotto non esistente");
    }

    @Test
    void idNonNumerico_rifiutato() {
        when(request.getParameter("id")).thenReturn("abc");
        assertErrore("Errore di formato nei dati inseriti nel form");
    }

    @Test
    void idMancante_rifiutato() {
        when(request.getParameter("id")).thenReturn(null);
        assertErrore("Errore di formato nei dati inseriti nel form");
    }
}
