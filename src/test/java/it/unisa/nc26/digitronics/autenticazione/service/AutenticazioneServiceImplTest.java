package it.unisa.nc26.digitronics.autenticazione.service;

import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
import it.unisa.nc26.digitronics.model.bean.Utente;
import it.unisa.nc26.digitronics.model.dao.ItemCarrelloDAO;
import it.unisa.nc26.digitronics.model.dao.UtenteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticazioneServiceImplTest {

    @Mock private UtenteDAO utenteDAO;
    @Mock private ItemCarrelloDAO itemCarrelloDAO;

    private AutenticazioneServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AutenticazioneServiceImpl();
        service.setUtenteDAO(utenteDAO);
        service.setItemCarrelloDAO(itemCarrelloDAO);
    }

    @Test
    void retrieveUtenteEmailPassword_credenzialiValide_restituisceUtente() {
        Utente utente = new Utente();
        when(utenteDAO.doRetrieveByEmailPassword("mario@test.it", "hash")).thenReturn(utente);

        assertSame(utente, service.retrieveUtenteEmailPassword("mario@test.it", "hash"));
    }

    @Test
    void retrieveUtenteEmailPassword_credenzialiErrate_restituisceNull() {
        when(utenteDAO.doRetrieveByEmailPassword("mario@test.it", "errata")).thenReturn(null);

        assertNull(service.retrieveUtenteEmailPassword("mario@test.it", "errata"));
    }

    @Test
    void retrieveCarrelloUtente_carrelloPieno_restituisceItemDelDao() {
        List<ItemCarrello> carrello = List.of(new ItemCarrello(), new ItemCarrello());
        when(itemCarrelloDAO.doRetrieveByIdUtente(7)).thenReturn(carrello);

        assertEquals(carrello, service.retrieveCarrelloUtente(7));
    }

    @Test
    void retrieveCarrelloUtente_carrelloVuoto_restituisceListaVuota() {
        when(itemCarrelloDAO.doRetrieveByIdUtente(7)).thenReturn(List.of());

        assertTrue(service.retrieveCarrelloUtente(7).isEmpty());
    }

    @Test
    void clearCarrelloUtente_delegaCancellazioneSoloAlDaoCarrello() {
        service.clearCarrelloUtente(7);

        verify(itemCarrelloDAO).doDelete(7);
        verifyNoMoreInteractions(itemCarrelloDAO);
        verifyNoInteractions(utenteDAO);
    }

    @Test
    void saveCarrelloUtente_passaLaListaAlDao() {
        List<ItemCarrello> carrello = List.of(new ItemCarrello());

        service.saveCarrelloUtente(carrello);

        verify(itemCarrelloDAO).doSave(carrello);
    }
}
