# Testing di Digitronics con JUnit e Mockito

## Obiettivo e configurazione

Il laboratorio ha esteso la suite automatica di Digitronics per individuare regressioni nei percorsi critici. Il [Test Plan](../projectDocs/testing/documenti/TP_Digitronics_2024-25.pdf) prevedeva test di unità e integrazione per checkout, modifica prodotto e aggiunta recensione. Le 12 classi esistenti sono state aggiornate e ne sono state aggiunte 18 per verificare autenticazione, registrazione, carrello, catalogo, gestione dei prodotti e accesso agli ordini.

La suite usa JUnit Jupiter 6.1.3 e Mockito 5.24.0. I test ancora su JUnit 4 sono stati migrati a Jupiter e il motore Vintage è stato rimosso dal [pom.xml](../pom.xml). Maven Surefire, già usato dal progetto, è stato aggiornato alla versione 3.5.4. Mockito viene caricato all'avvio della JVM dei test con `-javaagent` per evitare l'avviso di aggancio dinamico su Java 21. In locale, `./mvnw -B clean verify` su Java 21 ha superato tutti i **238 test** e prodotto il WAR.

## Strategia di testing

I test di unità verificano il comportamento di un componente isolando le sue dipendenze. Nei test dei servizi, Mockito sostituisce i DAO e permette di simulare sia i dati restituiti sia gli errori di accesso al database. Nei test delle servlet si preparano richieste HTTP e sessioni simulate e si controlla come la servlet risponde. Le verifiche di Mockito accertano anche quali operazioni vengono chieste alle dipendenze, come il salvataggio dei dati o l'inoltro a una pagina.

I test di integrazione verificano la collaborazione tra servizi e DAO eseguendo query su H2 in memoria. Il database viene inizializzato con lo [schema SQL di prova](../src/test/resources/database/database.sql) e usa la modalità di compatibilità MySQL. Le prove controllano che le scritture producano i dati attesi e che le letture li recuperino correttamente. Nei test che partono dalle servlet, la richiesta HTTP è simulata e i servizi accedono al database di prova attraverso i DAO.

Le chiamate ai servizi esterni sono sostituite da mock per poter controllare il comportamento dell'applicazione con risposte diverse. Le trasformazioni dei dati, come il parsing di una risposta JSON, sono verificate direttamente con dati di prova validi e non validi.

## Principi FIRST

- **Fast.** I test di unità isolano l'accesso a rete e database. I test di caricamento delle immagini usano una cartella temporanea.
- **Independent.** Dati e mock vengono ricreati per ogni test di unità. I test di integrazione chiudono il pool H2 e ripristinano `ConPool`. Nei test del servizio recensioni il database con i dati iniziali viene ricreato per ogni prova.
- **Repeatable.** Il mock di Nominatim rende ripetibili le prove del checkout. Le carte di pagamento fittizie usate nei casi validi hanno una scadenza calcolata a due anni dalla data della prova.
- **Self-validating.** Le asserzioni di JUnit e le verifiche di Mockito determinano automaticamente l'esito.
- **Thorough.** Le prove esercitano percorsi di successo, errori e valori limite, come una quantità pari alla scorta e un'immagine di un byte oltre il limite.

## Scenari verificati

1. **Autenticazione e registrazione.** Al login del cliente il carrello della sessione viene unito a quello salvato. Al logout il carrello viene salvato e la sessione chiusa. La registrazione rifiuta una password di 7 caratteri e ne accetta una di 8.
2. **Carrello e ordini.** I test esercitano visualizzazione, aggiunta, aggiornamento e rimozione dei prodotti. All'apertura del carrello, `MostraCarrelloServlet` riduce le quantità che superano la scorta. Durante l'aggiunta, un totale pari alla scorta è accettato. Se il totale supera la scorta, la richiesta è respinta e la quantità nel carrello resta quella precedente. Lo storico mostra al cliente i suoi ordini e all'amministratore l'elenco completo. L'accesso senza login viene respinto. `ShowItemOrderServlet` mostra i dettagli al proprietario e all'amministratore. Per un altro cliente la richiesta viene respinta prima di recuperare gli articoli.
3. **Catalogo e gestione dei prodotti.** I test coprono la ricerca senza risultati, l'aggiunta e la rimozione riservate all'amministratore e il limite delle immagini. Un file di 10 MiB è accettato e un byte in più causa il rifiuto.
4. **Oggetti del dominio.** Sono verificati il calcolo dell'hash della password in `Utente` e il confronto tra ordini in base all'identificativo.
