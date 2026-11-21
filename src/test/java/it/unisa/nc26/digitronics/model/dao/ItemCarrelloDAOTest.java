package it.unisa.nc26.digitronics.model.dao;

import it.unisa.nc26.digitronics.model.bean.ItemCarrello;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test di {@link ItemCarrelloDAO} su un database H2 in memoria: il carrello salvato del
 * cliente con id 1 contiene due prodotti. Gli errori del database sono simulati con Mockito.
 */
class ItemCarrelloDAOTest {

    private final ItemCarrelloDAO dao = new ItemCarrelloDAO();

    @BeforeEach
    void setUp() throws Exception {
        DatabaseDiTest.apriConDati();
        DatabaseDiTest.esegui(
                "INSERT INTO itemcarrello (IdProdotto, IdUtente, quantità) VALUES (1, 1, 2)",
                "INSERT INTO itemcarrello (IdProdotto, IdUtente, quantità) VALUES (3, 1, 1)");
    }

    @AfterEach
    void tearDown() {
        DatabaseDiTest.chiudi();
    }

    private static ItemCarrello item(int idUtente, int idProdotto, int quantita) {
        ItemCarrello item = new ItemCarrello();
        item.setIdUtente(idUtente);
        item.setIdProdotto(idProdotto);
        item.setQuantità(quantita);
        return item;
    }

    /** Ogni elemento del carrello salvato porta con sé il prodotto completo. */
    @Test
    void doRetrieveByIdUtente_caricaGliElementiConIlProdotto() {
        List<ItemCarrello> carrello = dao.doRetrieveByIdUtente(1);

        assertEquals(2, carrello.size());
        ItemCarrello iphone = carrello.stream().filter(i -> i.getIdProdotto() == 1).findFirst().orElseThrow();
        assertEquals(1, iphone.getIdUtente());
        assertEquals(2, iphone.getQuantità());
        assertEquals("iPhone 15", iphone.getProdotto().getNome());
    }

    @Test
    void doRetrieveByIdUtente_carrelloVuoto() {
        assertTrue(dao.doRetrieveByIdUtente(2).isEmpty());
    }

    @Test
    void doSave_salvaTuttiGliElementi() {
        dao.doSave(List.of(item(2, 1, 1), item(2, 2, 4)));

        List<ItemCarrello> carrello = dao.doRetrieveByIdUtente(2);
        assertEquals(2, carrello.size());
        assertEquals(5, carrello.stream().mapToInt(ItemCarrello::getQuantità).sum());
    }

    /** Un carrello assente o vuoto non viene salvato e non tocca il database. */
    @Test
    void doSave_carrelloNullOVuoto_nessunaOperazione() throws Exception {
        DatabaseDiTest.nonDisponibile();

        assertDoesNotThrow(() -> dao.doSave(null));
        assertDoesNotThrow(() -> dao.doSave(new ArrayList<>()));
    }

    @Test
    void doDelete_svuotaIlCarrelloDellUtente() {
        dao.doDelete(1);

        assertTrue(dao.doRetrieveByIdUtente(1).isEmpty());
    }

    @Test
    void doSave_nessunaRigaInserita_lanciaEccezione() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeUpdate()).thenReturn(0);
        DatabaseDiTest.usaStatement(ps);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> dao.doSave(List.of(item(1, 2, 1))));
        assertEquals("Errore durante l'accesso al carrello, riprova più tardi", ex.getMessage());
    }

    @Test
    void databaseNonDisponibile_ogniOperazioneLanciaEccezione() throws Exception {
        DatabaseDiTest.nonDisponibile();
        String messaggio = "Errore durante l'accesso al carrello, riprova più tardi";

        assertEquals(messaggio, assertThrows(RuntimeException.class, () -> dao.doRetrieveByIdUtente(1)).getMessage());
        assertEquals(messaggio, assertThrows(RuntimeException.class, () -> dao.doDelete(1)).getMessage());
        assertNotNull(assertThrows(RuntimeException.class, () -> dao.doSave(List.of(item(1, 2, 1)))).getCause());
    }
}
