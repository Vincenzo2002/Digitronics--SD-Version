# Strategia di testing di Digitronics

## Obiettivo

Il lab estende i test di Digitronics oltre le tre funzionalità coperte dal progetto originale (checkout, modifica prodotto, aggiunta recensione), seguendo i principi FIRST e usando Mockito per isolare database, API Servlet e servizi esterni.

## Punto di partenza

Il [Test Plan](../projectDocs/testing/documenti/TP_Digitronics_2024-25.pdf) e la [Test Case Specification](../projectDocs/testing/documenti/TCS_Digitronics_2024-25.pdf) definivano 32 test case di sistema (Selenium IDE) e test di unità e di integrazione solo per controller e service delle tre funzionalità, scritti con JUnit 4. Bean e DAO erano esclusi dai test di unità.

## Livelli di test

| Livello | Strumenti | Dipendenze sostituite |
|---|---|---|
| Unità | JUnit 6.1.3, Mockito 5.24.0 | DAO, API Servlet, API OpenStreetMap |
| Integrazione | JUnit 6.1.3, H2 in memoria in modalità MySQL | Database MySQL, API OpenStreetMap |
| Sistema | Selenium IDE | Nessuna |

Tutti i test JUnit 4 sono stati migrati a JUnit 6 (Jupiter); JUnit 4 e il motore Vintage sono stati rimossi dal `pom.xml`.

## Componenti aggiunte

- **Service:** `AutenticazioneServiceImpl`, `RegistrazioneServiceImpl`, `GestioneCarrelloServiceImpl`, `InfoProdottoServiceImpl`.
- **Controller:** `AutenticazioneServlet`, `LogoutServlet`, `RegistrazioneServlet`, le servlet del carrello, `AggiungiProdottoServlet`, `RimuoviProdottoServlet`.
- **Trasformazione dei dati:** interpretazione della risposta di OpenStreetMap, separata dalla chiamata HTTP per poterla testare; hash della password in `Utente`; uguaglianza tra ordini.

## Principi FIRST

- **Fast:** i test di unità non accedono a database, rete o file reali; le immagini caricate finiscono in una cartella temporanea.
- **Independent:** oggetto sotto test e mock vengono ricreati per ogni test; i test di integrazione chiudono il pool H2 e ripristinano `ConPool` al termine.
- **Repeatable:** nessun test chiama Nominatim e le scadenze delle carte sono calcolate dalla data di esecuzione invece di essere fisse.
- **Self-validating:** ogni test termina con asserzioni o `verify` esplicite.
- **Thorough:** oltre al caso di successo, ogni componente è verificata su valori limite, errori e regole di business.

## Copertura

- **Percorsi critici:** login con unione dei carrelli, logout con salvataggio del carrello, registrazione, gestione del carrello, aggiunta ed eliminazione di prodotti.
- **Casi limite:** password di 7 e 8 caratteri, quantità pari alla scorta e superiore di uno, quantità zero o negativa, file di 10 MB e di un byte in più.
- **Condizioni di errore:** parametri mancanti, vuoti o non numerici, prodotto inesistente, utente non autorizzato, risposta JSON non valida.
- **Regole di business:** disponibilità in magazzino, funzioni riservate all'admin, carrello associato all'utente, password salvata come hash.

La coverage viene misurata con JaCoCo; la CI carica il report come artifact.

## Difetti individuati e corretti

1. **Scorta del carrello:** il controllo ignorava la quantità già presente nel carrello, contro il requisito del RAD sulla disponibilità in magazzino. Ora somma le due quantità.
2. **Nomi in registrazione:** venivano rifiutati nomi accentati, come "Niccolò", che il checkout accettava. Ora i due controlli usano lo stesso formato, anche nella pagina di registrazione.
3. **Email:** i domini di primo livello oltre 4 caratteri, come `.museum`, erano rifiutati.

## Limiti

Il calcolo del totale dell'ordine resta in `GestioneOrdineServlet`, dove è coperto solo dai test della servlet. La chiamata HTTP a Nominatim non è testata, perché richiederebbe la rete.

## Integrazione con la CI

[ci.yml](../.github/workflows/ci.yml) esegue `./mvnw -B clean verify` a ogni push e pull request e carica i report di Surefire e JaCoCo anche quando un test fallisce. Un test fallito blocca i job Docker. Mockito viene caricato come agent all'avvio della JVM, come indicato per Java 21.
