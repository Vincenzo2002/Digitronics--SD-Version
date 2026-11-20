package it.unisa.nc26.digitronics.utils;

import it.unisa.nc26.digitronics.infoProdotto.service.InfoProdottoService;
import it.unisa.nc26.digitronics.model.bean.Categoria;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Test di unità di {@link InitServlet}: all'avvio dell'applicazione le categorie vengono
 * caricate una volta e messe nel contesto, a disposizione di tutte le pagine.
 * Il service e le API Servlet sono sostituiti da mock.
 */
@ExtendWith(MockitoExtension.class)
class InitServletTest {

    @Mock private InfoProdottoService service;
    @Mock private ServletConfig config;
    @Mock private ServletContext context;

    @Test
    void init_mettiLeCategorieNelContesto() throws Exception {
        List<Categoria> categorie = List.of(new Categoria(), new Categoria());
        when(service.getAllCategorie()).thenReturn(categorie);
        when(config.getServletContext()).thenReturn(context);
        InitServlet servlet = new InitServlet();
        servlet.setInfoProdottoService(service);

        servlet.init(config);

        verify(context).setAttribute("categorie", categorie);
    }
}
