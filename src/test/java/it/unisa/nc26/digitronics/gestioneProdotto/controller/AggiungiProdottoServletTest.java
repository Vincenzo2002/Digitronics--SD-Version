package it.unisa.nc26.digitronics.gestioneProdotto.controller;

import it.unisa.nc26.digitronics.gestioneProdotto.service.GestioneProdottoService;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
import it.unisa.nc26.digitronics.model.bean.Utente;
import it.unisa.nc26.digitronics.utils.MyServletException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link AggiungiProdottoServlet}: accesso riservato all'admin, controlli
 * sul file caricato (estensione e dimensione), salvataggio dell'immagine e creazione del prodotto.
 * Il service e le API Servlet sono mock; l'immagine viene scritta in una cartella temporanea.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AggiungiProdottoServletTest {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final String CARTELLA_UPLOAD = "./images/prodotti";
    private static final String PAGINA_ERRORE = "/WEB-INF/results/general-error.jsp";

    @TempDir Path cartellaWebapp;

    @Mock private GestioneProdottoService service;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher dispatcher;
    @Mock private ServletConfig config;
    @Mock private ServletContext context;
    @Mock private Part file;

    private AggiungiProdottoServlet servlet;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new AggiungiProdottoServlet();
        servlet.setModifyProductService(service);
        when(config.getServletContext()).thenReturn(context);
        servlet.init(config);
        // Il percorso reale della webapp è la cartella temporanea del test
        when(context.getRealPath(anyString()))
                .thenAnswer(inv -> cartellaWebapp.resolve(inv.getArgument(0, String.class)).normalize().toString());

        when(request.getSession()).thenReturn(session);
        when(request.getRequestDispatcher(anyString())).thenReturn(dispatcher);
        when(session.getAttribute("utente")).thenReturn(utente(true));
        when(request.getPart("file")).thenReturn(file);
        impostaFile("foto.png", 4);

        when(request.getParameter("nome")).thenReturn("Telefono");
        when(request.getParameter("descrizione")).thenReturn("Descrizione valida");
        when(request.getParameter("prezzo")).thenReturn("499.99");
        when(request.getParameter("quantita")).thenReturn("10");
        when(request.getParameter("nomecategoria")).thenReturn("Smartphone");
        when(request.getParameter("vetrina")).thenReturn("on");
    }

    private void impostaFile(String nome, long dimensione) throws Exception {
        when(file.getSubmittedFileName()).thenReturn(nome);
        when(file.getSize()).thenReturn(dimensione);
        when(file.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream("dati".getBytes(StandardCharsets.UTF_8)));
    }

    private static Utente utente(boolean admin) {
        Utente utente = new Utente();
        utente.setRuolo(admin);
        return utente;
    }

    private static String percorso(String nomeFile) {
        return CARTELLA_UPLOAD + File.separator + nomeFile;
    }

    private void assertPaginaErrore(String messaggio) throws Exception {
        verify(request).setAttribute("errorMessage", messaggio);
        verify(request).getRequestDispatcher(PAGINA_ERRORE);
        verify(dispatcher).forward(request, response);
    }

    // --- Percorso critico ---

    /** L'admin aggiunge un prodotto valido: l'immagine viene salvata e il prodotto passato al service. */
    @Test
    void datiValidi_salvaImmagineEAggiungeProdotto() throws Exception {
        servlet.doPost(request, response);

        ArgumentCaptor<Prodotto> captor = ArgumentCaptor.forClass(Prodotto.class);
        verify(service).aggiungiProdotto(captor.capture());
        Prodotto prodotto = captor.getValue();
        assertEquals("Telefono", prodotto.getNome());
        assertEquals(499.99, prodotto.getPrezzo(), 0.001);
        assertEquals(10, prodotto.getQuantità());
        assertEquals("Smartphone", prodotto.getNomeCategoria());
        assertTrue(prodotto.isVetrina());
        assertEquals(percorso("foto.png"), prodotto.getImmagine());
        assertTrue(Files.exists(cartellaWebapp.resolve(percorso("foto.png")).normalize()));
        verify(request).setAttribute("successMessage", "Prodotto aggiunto con successo");
        verify(request).getRequestDispatcher("WEB-INF/results/confirm-operation.jsp");
    }

    /** Se esiste già un'immagine con lo stesso nome, la nuova viene salvata con un prefisso numerico. */
    @Test
    void immagineGiaEsistente_vieneRinominata() throws Exception {
        Path esistente = cartellaWebapp.resolve(percorso("foto.png")).normalize();
        Files.createDirectories(esistente.getParent());
        Files.writeString(esistente, "vecchia");

        servlet.doPost(request, response);

        ArgumentCaptor<Prodotto> captor = ArgumentCaptor.forClass(Prodotto.class);
        verify(service).aggiungiProdotto(captor.capture());
        assertEquals(percorso("2_foto.png"), captor.getValue().getImmagine());
        assertEquals("vecchia", Files.readString(esistente));
    }

    /** Senza la checkbox il prodotto non è in vetrina. */
    @Test
    void vetrinaNonSelezionata_prodottoNonInVetrina() throws Exception {
        when(request.getParameter("vetrina")).thenReturn(null);

        servlet.doPost(request, response);

        ArgumentCaptor<Prodotto> captor = ArgumentCaptor.forClass(Prodotto.class);
        verify(service).aggiungiProdotto(captor.capture());
        assertFalse(captor.getValue().isVetrina());
    }

    // --- Accesso ---

    @Test
    void utenteNonAutenticato_rifiutato() {
        when(session.getAttribute("utente")).thenReturn(null);

        MyServletException ex = assertThrows(MyServletException.class, () -> servlet.doPost(request, response));
        assertEquals("Non puoi accedere a questa pagina", ex.getMessage());
        verifyNoInteractions(service);
    }

    @Test
    void cliente_rifiutato() {
        when(session.getAttribute("utente")).thenReturn(utente(false));

        assertThrows(MyServletException.class, () -> servlet.doPost(request, response));
        verifyNoInteractions(service);
    }

    // --- Controlli sul file e casi limite ---

    @Test
    void estensioneNonAmmessa_mostraErrore() throws Exception {
        impostaFile("documento.pdf", 4);

        servlet.doPost(request, response);

        assertPaginaErrore("Tipo di file non supportato. Carica un file con estensione JPG, JPEG, PNG o GIF.");
        verifyNoInteractions(service);
    }

    @Test
    void fileSenzaEstensione_mostraErrore() throws Exception {
        impostaFile("foto", 4);

        servlet.doPost(request, response);

        assertPaginaErrore("Tipo di file non supportato. Carica un file con estensione JPG, JPEG, PNG o GIF.");
        verifyNoInteractions(service);
    }

    /** L'estensione viene confrontata senza distinguere maiuscole e minuscole. */
    @Test
    void estensioneMaiuscola_accettata() throws Exception {
        impostaFile("FOTO.JPG", 4);

        servlet.doPost(request, response);

        verify(service).aggiungiProdotto(any(Prodotto.class));
    }

    /** Valore limite: un file di esattamente 10 MB è accettato. */
    @Test
    void fileDiDieciMegabyte_accettato() throws Exception {
        impostaFile("foto.png", MAX_FILE_SIZE);

        servlet.doPost(request, response);

        verify(service).aggiungiProdotto(any(Prodotto.class));
    }

    /** Valore limite: un byte oltre i 10 MB il file è rifiutato. */
    @Test
    void fileOltreDieciMegabyte_mostraErrore() throws Exception {
        impostaFile("foto.png", MAX_FILE_SIZE + 1);

        servlet.doPost(request, response);

        assertPaginaErrore("Dimensione del file superiore al limite consentito (10MB).");
        verifyNoInteractions(service);
    }

    // --- Errori sui dati del prodotto ---

    @Test
    void prezzoNonNumerico_mostraErrore() throws Exception {
        when(request.getParameter("prezzo")).thenReturn("abc");

        servlet.doPost(request, response);

        verify(request).setAttribute(eq("errorMessage"), anyString());
        verify(request).getRequestDispatcher(PAGINA_ERRORE);
        verifyNoInteractions(service);
    }

    @Test
    void categoriaInesistente_mostraMessaggioDelService() throws Exception {
        doThrow(new MyServletException("Categoria non esistente")).when(service).aggiungiProdotto(any(Prodotto.class));

        servlet.doPost(request, response);

        assertPaginaErrore("Categoria non esistente");
    }
}
