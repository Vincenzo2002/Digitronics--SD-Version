# Code coverage di Digitronics

## 1. Configurazione

La coverage è misurata con il plugin **JaCoCo 0.8.15**, configurato nel [`pom.xml`](../pom.xml) con tre esecuzioni:

| Esecuzione | Fase | Cosa fa |
|---|---|---|
| `prepare-agent` | `initialize` | Prepara l'agent di JaCoCo, che Surefire carica nella JVM dei test tramite `@{argLine}` insieme all'agent di Mockito |
| `report` | `test` | Scrive il report in `target/site/jacoco/` (HTML, XML e CSV) |
| `check` | `verify` | Applica il quality gate e fa fallire la build se una soglia non è rispettata |

Il quality gate vale per **ogni pacchetto** (`<element>PACKAGE</element>`):

| Contatore | Soglia minima |
|---|---|
| Righe (`LINE`) | 80% |
| Rami (`BRANCH`) | 75% |
| Classi (`CLASS`) | 100% |

`./mvnw clean verify` esegue test, report e controllo. La [CI](../.github/workflows/ci.yml) salva i report nell'artifact `quality-reports`. Un job separato scarica l'artifact e invia l'XML di JaCoCo a **Codecov** con `CODECOV_TOKEN`.

## 2. Analisi

Con il quality gate attivo, la prima esecuzione falliva su 4 pacchetti:

| Pacchetto | Righe | Rami | Classi |
|---|---|---|---|
| `infoProdotto.controller` | 0% | 0% | 0/3 |
| `utils` | 14% | 5% | 3/5 |
| `model.dao` | 43% | 29% | 8/8 |
| `gestioneOrdine.service.IndirizzoAdapter` | 63% | 83% | 1/1 |

Dopo i nuovi test (355 in totale, tutti verdi) tutti i pacchetti rispettano le soglie:

| Metrica | Coperto | Percentuale |
|---|---|---|
| **Righe** | 1.656 / 1.702 | **97,3%** |
| **Rami** | 405 / 445 | **91,0%** |
| Istruzioni | 6.067 / 6.194 | 97,9% |
| Metodi | 285 / 299 | 95,3% |
| Classi | 46 / 46 | 100% |

**Classi con la coverage più bassa (righe):** `RimuoviProdottoCarrelloServlet` 81,8%, `ShowItemOrderServlet` 82,1%, `ConPool` 86,1%, `StoricoOrdiniServlet` 90,0%. Per i rami il pacchetto più basso è `model.bean` (75,0%, esattamente la soglia), seguito da `gestioneRecensione.controller` (83,3%) e `gestioneOrdine.controller` (86,2%).

**Metodi non coperti (14):**
- `doPost` di 11 servlet, che si limita a chiamare `doGet`;
- `RimuoviProdottoCarrelloServlet.setGestioneCarrelloService` e `ItemOrdine.getIdItemOrdine`, mai usati dai test;
- il costruttore implicito di `ConPool`, classe con soli metodi statici.

**Come è stata alzata la coverage:**
- test di unità per le servlet di `infoProdotto` e `utils`;
- test dei DAO su H2 in memoria, con gli errori del database simulati con Mockito;
- test della chiamata HTTP simulata e della codifica UTF-8 del parametro di ricerca dell'adapter OpenStreetMap;
- test della rilettura della configurazione di `ConPool` dopo un errore di accesso al file della password.

Per renderli possibili sono stati separati in metodi sostituibili nei test la lettura delle variabili d'ambiente in `ConPool` e l'apertura della connessione nell'adapter. I test hanno anche fatto emergere un difetto: `CategoriaDAO.doSave` inseriva nella tabella `utente` invece che in `categoria`. È stato corretto.

## 3. Piano di miglioramento

1. **`doPost` delle servlet:** un test per servlet che verifichi la delega a `doGet`. Sono pochi test e chiudono quasi tutti i metodi scoperti.
2. **Rami di `gestioneOrdine.controller`:** coprire il carrello assente in sessione. La condizione `carrelloSession.isEmpty() || carrelloSession == null` controlla `null` dopo averlo già usato: con il carrello assente produce una `NullPointerException`. Va prima corretta (invertendo i due controlli) e poi testata.
3. **`ShowItemOrderServlet`:** aggiungere i due percorsi di errore non coperti, cioè id dell'ordine non numerico e ordine senza elementi.
4. **`ConPool`:** la creazione del pool MySQL vero resta scoperta perché richiede un database. Si può coprire con un test di integrazione su un MySQL in container (Testcontainers), eseguito solo in CI.
5. **`model.bean`:** i rami del pacchetto sono esattamente al 75% per `Ordine.equals`. `OrdineTest` passa `null` e una stringa come primo argomento di `assertNotEquals`, quindi viene chiamato l'`equals` dell'altro oggetto e non quello di `Ordine`. Basta chiamare `ordine.equals(null)` e `ordine.equals("1")` direttamente.
6. **Iterazione:** la CI esegue il quality gate a ogni push e Codecov mostra la variazione di coverage di ogni pull request. Ogni nuova funzionalità deve arrivare con i suoi test, così le soglie restano rispettate.
