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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link AggiungiProdottoCarrelloServlet}: validazione dei parametri,
 * regola sulla disponibilità in magazzino e gestione del carrello di sessione.
 * Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AggiungiProdottoCarrelloServletTest {

    private static final int ID_PRODOTTO = 5;
    private static final int SCORTA = 5;

    @Mock private GestioneCarrelloService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;

    private AggiungiProdottoCarrelloServlet servlet;
    private List<ItemCarrello> carrello;
    private Prodotto prodotto;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new AggiungiProdottoCarrelloServlet();
        servlet.setGestioneCarrelloService(service);

        carrello = new ArrayList<>();
        prodotto = new Prodotto();
        prodotto.setIdProdotto(ID_PRODOTTO);
        prodotto.setQuantità(SCORTA);

        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(session.getAttribute("carrello")).thenReturn(carrello);
        when(service.fetchByIdProdotto(ID_PRODOTTO)).thenReturn(prodotto);
        impostaParametri(String.valueOf(ID_PRODOTTO), "2");
    }

    private void impostaParametri(String idProdotto, String quantita) {
        when(request.getParameter("idProdotto")).thenReturn(idProdotto);
        when(request.getParameter("quantità")).thenReturn(quantita);
    }

    private void assertErrore(String messaggioAtteso) {
        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals(messaggioAtteso, ex.getMessage());
        assertTrue(carrello.isEmpty());
    }

    private ItemCarrello itemNelCarrello(int quantita) {
        ItemCarrello item = new ItemCarrello();
        item.setIdProdotto(ID_PRODOTTO);
        item.setQuantità(quantita);
        carrello.add(item);
        return item;
    }

    // --- Percorsi critici ---

    /** Un ospite aggiunge un prodotto nuovo: l'item non ha id utente e si va al carrello. */
    @Test
    void ospite_aggiungeProdottoNuovo() throws Exception {
        servlet.doGet(request, response);

        assertEquals(1, carrello.size());
        ItemCarrello item = carrello.get(0);
        assertNull(item.getIdUtente());
        assertEquals(ID_PRODOTTO, item.getIdProdotto());
        assertEquals(2, item.getQuantità());
        verify(request).getRequestDispatcher("/WEB-INF/results/cart.jsp");
        verify(dispatcher).forward(request, response);
    }

    /** Per un cliente autenticato l'item riceve il suo id. */
    @Test
    void clienteAutenticato_itemConIdUtente() throws Exception {
        Utente cliente = new Utente();
        cliente.setIdUtente(3);
        cliente.setRuolo(false);
        when(session.getAttribute("utente")).thenReturn(cliente);

        servlet.doGet(request, response);

        assertEquals(3, carrello.get(0).getIdUtente());
    }

    /** Se il prodotto è già nel carrello la quantità viene sommata, senza duplicare l'item. */
    @Test
    void prodottoGiaNelCarrello_sommaLaQuantita() throws Exception {
        ItemCarrello esistente = itemNelCarrello(1);

        servlet.doGet(request, response);

        assertEquals(1, carrello.size());
        assertEquals(3, esistente.getQuantità());
    }

    // --- Regole di business e casi limite ---

    /** Valore limite: una quantità pari alla scorta è accettata. */
    @Test
    void quantitaUgualeAllaScorta_accettata() throws Exception {
        impostaParametri(String.valueOf(ID_PRODOTTO), String.valueOf(SCORTA));

        servlet.doGet(request, response);

        assertEquals(SCORTA, carrello.get(0).getQuantità());
    }

    /** Valore limite: una quantità superiore di uno alla scorta è rifiutata. */
    @Test
    void quantitaSuperioreAllaScorta_rifiutata() {
        impostaParametri(String.valueOf(ID_PRODOTTO), String.valueOf(SCORTA + 1));
        assertErrore("Quantità selezionata del prodotto non presente in magazzino");
    }

    @Test
    void admin_nonPuoUsareIlCarrello() {
        Utente admin = new Utente();
        admin.setRuolo(true);
        when(session.getAttribute("utente")).thenReturn(admin);

        assertErrore("L'admin non può interagire con il carrello");
    }

    // --- Condizioni di errore sui parametri ---

    @Test
    void idProdottoMancante_rifiutato() {
        impostaParametri(null, "2");
        assertErrore("Il parametro idProdotto non può essere vuoto");
    }

    @Test
    void idProdottoVuoto_rifiutato() {
        impostaParametri("   ", "2");
        assertErrore("Il parametro idProdotto non può essere vuoto");
    }

    @Test
    void idProdottoNonNumerico_rifiutato() {
        impostaParametri("abc", "2");
        assertErrore("Id prodotto non valido: deve essere un numero intero");
    }

    /** Valore limite: id prodotto pari a zero. */
    @Test
    void idProdottoZero_rifiutato() {
        impostaParametri("0", "2");
        assertErrore("Id prodotto non valido: deve essere un numero intero positivo");
    }

    @Test
    void prodottoInesistente_rifiutato() throws Exception {
        when(service.fetchByIdProdotto(9)).thenReturn(null);
        impostaParametri("9", "2");

        assertErrore("Il prodotto non esiste");
    }

    @Test
    void quantitaMancante_rifiutata() {
        impostaParametri(String.valueOf(ID_PRODOTTO), null);
        assertErrore("Inserire la quantità è obbligatorio");
    }

    /** Valore limite: quantità pari a zero. */
    @Test
    void quantitaZero_rifiutata() {
        impostaParametri(String.valueOf(ID_PRODOTTO), "0");
        assertErrore("La quantità deve essere un numero intero positivo");
    }

    @Test
    void quantitaNegativa_rifiutata() {
        impostaParametri(String.valueOf(ID_PRODOTTO), "-1");
        assertErrore("La quantità deve essere un numero intero positivo");
    }

    @Test
    void quantitaNonNumerica_rifiutata() {
        impostaParametri(String.valueOf(ID_PRODOTTO), "due");
        assertErrore("Quantità non valida: deve essere un numero intero");
    }

    // --- Caso limite sulla scorta (difetto individuato e corretto) ---

    /**
     * La quantità totale nel carrello non può superare la scorta: con scorta 5,
     * aggiungere 2 pezzi a 4 già presenti è rifiutato e il carrello resta invariato.
     * In precedenza il controllo considerava solo la quantità aggiunta.
     */
    @Test
    void sommaOltreLaScorta_rifiutata() {
        ItemCarrello esistente = itemNelCarrello(4);
        impostaParametri(String.valueOf(ID_PRODOTTO), "2");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));
        assertEquals(4, esistente.getQuantità());
    }

    @Test
    void sommaOltreIlMassimoDiInt_rifiutataSenzaModificareIlCarrello() {
        prodotto.setQuantità(Integer.MAX_VALUE);
        ItemCarrello esistente = itemNelCarrello(Integer.MAX_VALUE);
        impostaParametri(String.valueOf(ID_PRODOTTO), "1");

        assertThrows(MyServletException.class, () -> servlet.doGet(request, response));

        assertEquals(Integer.MAX_VALUE, esistente.getQuantità());
        assertEquals(List.of(esistente), carrello);
    }

    /** Valore limite: la somma con la quantità già nel carrello pari alla scorta è accettata. */
    @Test
    void sommaUgualeAllaScorta_accettata() throws Exception {
        ItemCarrello esistente = itemNelCarrello(3);
        impostaParametri(String.valueOf(ID_PRODOTTO), "2");

        servlet.doGet(request, response);

        assertEquals(SCORTA, esistente.getQuantità());
        assertEquals(1, carrello.size());
    }
}
