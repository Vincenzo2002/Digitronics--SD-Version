package it.unisa.nc26.digitronics.registrazione.service;

import it.unisa.nc26.digitronics.model.bean.Utente;
import it.unisa.nc26.digitronics.model.dao.UtenteDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrazioneServiceImplTest {

    @Mock private UtenteDAO utenteDAO;

    private RegistrazioneServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RegistrazioneServiceImpl();
        service.setUtenteDAO(utenteDAO);
    }

    @Test
    void registraUtente_salvataggioRiuscito_restituisceEsitoDelDao() {
        Utente utente = new Utente();
        when(utenteDAO.doSave(utente)).thenReturn(42);

        assertEquals(42, service.registraUtente(utente));
        verify(utenteDAO).doSave(utente);
    }

    @Test
    void registraUtente_daoLanciaEccezione_propagaEccezione() {
        Utente utente = new Utente();
        when(utenteDAO.doSave(utente)).thenThrow(new RuntimeException("email duplicata"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.registraUtente(utente));
        assertEquals("email duplicata", ex.getMessage());
    }
}
