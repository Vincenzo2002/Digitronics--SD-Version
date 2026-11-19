package it.unisa.nc26.digitronics.gestioneCarrello.service;

import it.unisa.nc26.digitronics.model.bean.Prodotto;
import it.unisa.nc26.digitronics.model.dao.ItemCarrelloDAO;
import it.unisa.nc26.digitronics.model.dao.ProdottoDAO;
import it.unisa.nc26.digitronics.utils.MyServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestioneCarrelloServiceImplTest {

    @Mock private ProdottoDAO prodottoDAO;
    @Mock private ItemCarrelloDAO itemCarrelloDAO;

    private GestioneCarrelloServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GestioneCarrelloServiceImpl();
        service.setProdottoDAO(prodottoDAO);
        service.setItemCarrelloDAO(itemCarrelloDAO);
    }

    @Test
    void fetchByIdProdotto_prodottoEsistente_restituisceProdotto() throws MyServletException {
        Prodotto prodotto = new Prodotto();
        prodotto.setIdProdotto(5);
        when(prodottoDAO.doRetrieveById(5)).thenReturn(prodotto);

        assertSame(prodotto, service.fetchByIdProdotto(5));
    }

    @Test
    void fetchByIdProdotto_prodottoInesistente_lanciaMyServletException() {
        when(prodottoDAO.doRetrieveById(99)).thenReturn(null);

        MyServletException ex = assertThrows(MyServletException.class,
                () -> service.fetchByIdProdotto(99));
        assertEquals("Prodotto con ID 99 non esistente.", ex.getMessage());
    }

    @Test
    void fetchByIdProdotto_nonUsaIlDaoCarrello() throws MyServletException {
        when(prodottoDAO.doRetrieveById(5)).thenReturn(new Prodotto());

        service.fetchByIdProdotto(5);

        verifyNoInteractions(itemCarrelloDAO);
    }
}
