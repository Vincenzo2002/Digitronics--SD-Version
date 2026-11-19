package it.unisa.nc26.digitronics.infoProdotto.service;

import it.unisa.nc26.digitronics.model.bean.Categoria;
import it.unisa.nc26.digitronics.model.bean.Prodotto;
import it.unisa.nc26.digitronics.model.bean.Recensione;
import it.unisa.nc26.digitronics.model.dao.CategoriaDAO;
import it.unisa.nc26.digitronics.model.dao.ProdottoDAO;
import it.unisa.nc26.digitronics.model.dao.RecensioneDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfoProdottoServiceImplTest {

    @Mock private ProdottoDAO prodottoDAO;
    @Mock private CategoriaDAO categoriaDAO;
    @Mock private RecensioneDAO recensioneDAO;

    private InfoProdottoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InfoProdottoServiceImpl();
        service.setProdottoDAO(prodottoDAO);
        service.setCategoriaDAO(categoriaDAO);
        service.setRecensioneDAO(recensioneDAO);
    }

    @Test
    void fetchByIdProdotto_inesistente_restituisceNullSenzaEccezione() {
        when(prodottoDAO.doRetrieveById(99)).thenReturn(null);

        assertNull(service.fetchByIdProdotto(99));
    }

    @Test
    void cercaProdottoPerNome_restituisceRisultatiDelDao() {
        List<Prodotto> risultati = List.of(new Prodotto());
        when(prodottoDAO.doRetrieveByName("iphone")).thenReturn(risultati);

        assertEquals(risultati, service.cercaProdottoPerNome("iphone"));
    }

    @Test
    void cercaProdottoPerNome_nessunRisultato_restituisceListaVuota() {
        when(prodottoDAO.doRetrieveByName("xyz")).thenReturn(List.of());

        assertTrue(service.cercaProdottoPerNome("xyz").isEmpty());
    }

    @Test
    void getProdottiPerCategoria_restituisceProdottiDellaCategoria() {
        List<Prodotto> prodotti = List.of(new Prodotto(), new Prodotto());
        when(prodottoDAO.doRetrieveByCategory("Smartphone")).thenReturn(prodotti);

        assertEquals(2, service.getProdottiPerCategoria("Smartphone").size());
    }

    @Test
    void getCategoriaPerNome_inesistente_restituisceNull() {
        when(categoriaDAO.doRetrieveByNomeCategoria("Inesistente")).thenReturn(null);

        assertNull(service.getCategoriaPerNome("Inesistente"));
    }

    @Test
    void getAllCategorie_restituisceTutteLeCategorie() {
        List<Categoria> categorie = List.of(new Categoria(), new Categoria());
        when(categoriaDAO.doRetrieveAll()).thenReturn(categorie);

        assertEquals(categorie, service.getAllCategorie());
    }

    @Test
    void fetchRecensioniByIdProdotto_restituisceRecensioniComeLista() throws SQLException {
        List<Recensione> recensioni = new ArrayList<>(List.of(new Recensione()));
        when(recensioneDAO.doRetrieveByProduct(3)).thenReturn(recensioni);

        assertEquals(recensioni, service.fetchRecensioniByIdProdotto(3));
    }

    @Test
    void fetchRecensioniByIdProdotto_errorDb_propagaSQLException() throws SQLException {
        when(recensioneDAO.doRetrieveByProduct(3)).thenThrow(new SQLException("db down"));

        assertThrows(SQLException.class, () -> service.fetchRecensioniByIdProdotto(3));
    }

    @Test
    void fetchProdottoVetrina_restituisceProdottiInVetrina() {
        List<Prodotto> vetrina = List.of(new Prodotto());
        when(prodottoDAO.doRetrieveVetrina()).thenReturn(vetrina);

        assertEquals(vetrina, service.fetchProdottoVetrina());
    }
}
