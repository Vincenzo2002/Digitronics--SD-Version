# Mutation testing di Digitronics

## 1. Configurazione

Il mutation testing usa **PITest 1.30.2** con `pitest-junit5-plugin` 1.2.3, che esegue i test JUnit 6. Il plugin è configurato nel [`pom.xml`](../pom.xml):

| Parametro | Valore | Significato |
|---|---|---|
| `targetClasses` | `it.unisa.nc26.digitronics.*` | Classi di produzione da mutare |
| `targetTests` | `it.unisa.nc26.digitronics.*` | Test eseguiti su ogni mutante |
| `mutators` | `CONDITIONALS_BOUNDARY`, `NEGATE_CONDITIONALS`, `INVERT_NEGS`, `MATH` | Tipi di modifica: confini dei confronti (`<` ↔ `<=`), condizioni invertite, segno dei numeri, operatori aritmetici |
| `mutationThreshold` | 80 | Quality gate: almeno l'80% dei mutanti deve essere rilevato |
| `coverageThreshold` | 80 | Quality gate: almeno l'80% delle righe delle classi mutate deve essere coperto |
| `timeoutConstant`, `timeoutFactor` | 5000 ms, 1,5 | Un mutante va in timeout oltre `tempo normale × 1,5 + 5 s` |
| `outputFormats` | HTML, XML | Report in `target/pit-reports/` |

Comando: `./mvnw test-compile org.pitest:pitest-maven:mutationCoverage`. Il `test-compile` serve perché gli agent di JaCoCo e Mockito, che Surefire passa ai test, sono preparati nelle prime fasi di Maven: lanciando solo `pitest:mutationCoverage` dopo un `clean`, PITest non riesce ad avviare i test. In CI ([`ci.yml`](../.github/workflows/ci.yml)) lo stesso comando gira dopo `verify`, e il report HTML è salvato nell'artifact `quality-reports`.

## 2. Analisi

| Metrica | Prima esecuzione | Dopo i nuovi test |
|---|---|---|
| Mutanti generati | 230 | 230 |
| Uccisi | 217 | 227 |
| Timeout (contati come rilevati) | 1 | 1 |
| Sopravvissuti | 11 | **2** |
| Senza coverage | 1 | 0 |
| **Mutation score** | **95%** (218/230) | **99%** (228/230) |
| Test strength | 95% | 99% |
| Line coverage delle classi mutate | 97% | 97% (1.442/1.483) |

Mutanti rilevati per operatore: `NEGATE_CONDITIONALS` 200 su 200 (1 in timeout), `MATH` 11 su 11, `CONDITIONALS_BOUNDARY` 17 su 19. `INVERT_NEGS` non ha generato mutanti.

**Schemi ricorrenti nei 12 mutanti non rilevati alla prima esecuzione:**
- **Confini non provati (9 su 11):** `>` diventava `>=` senza che nessun test se ne accorgesse, perché mancavano i valori esattamente al limite. Riguardava la quantità pari alla scorta nel checkout, la lunghezza di 255 caratteri di nome, categoria e titolo della recensione, e il punteggio minimo 1. Due di questi, sui limiti della scorta nel carrello, sono equivalenti (vedi sotto).
- **Calcoli con valori neutri (1):** il totale `prezzo * quantità` era provato solo con quantità 1, dove moltiplicare e dividere danno lo stesso risultato.
- **Effetti non verificati (1):** la scorta dopo l'ordine (`scorta - quantità`) non era controllata, quindi `scorta + quantità` sopravviveva.
- **Codice non coperto (1):** la registrazione con un utente già autenticato. Il test ha rivelato un difetto: dopo l'inoltro alla pagina, il metodo continuava e registrava di nuovo l'utente. È stato corretto con un `return`.

**Aree con mutation score più basso:** solo `gestioneCarrello.controller` (32/34, 94%). I 2 sopravvissuti sono **mutanti equivalenti**, in `AggiornaQuantitàCarrelloServlet` (riga 94) e `MostraCarrelloServlet` (riga 79). Il codice è `if (quantità > scorta) quantità = scorta;`: con `>=`, quando la quantità è uguale alla scorta, viene assegnato lo stesso valore. Il comportamento non cambia, quindi nessun test può distinguerli. Tutti gli altri pacchetti sono al 100%.

L'unico timeout è in `AggiungiProdottoServlet` (riga 131): invertendo la condizione del ciclo che rinomina il file caricato, il ciclo diventa infinito. PITest lo conta correttamente come rilevato.

## 3. Piano di miglioramento

1. **Mutanti equivalenti:** riscrivere i due limiti come `quantità = Math.min(quantità, scorta)`. Il comportamento resta lo stesso, ma il codice non genera più mutanti indistinguibili.
2. **Più operatori:** oggi i 4 operatori del lab provano soprattutto le condizioni. Il passo successivo è aggiungere `VOID_METHOD_CALLS` ed `EMPTY_RETURNS`, che verificano che i test controllino le chiamate ai DAO (salvataggi, aggiornamenti) e i valori restituiti dai service, poi `INCREMENTS`.
3. **Regola per i nuovi test:** per ogni condizione con un limite, testare il valore uguale al limite e quello appena oltre; per ogni calcolo, usare valori diversi da 0 e 1; per ogni modifica di stato, verificare il valore risultante e non solo che il metodo sia stato chiamato.
4. **Iterazione in CI:** PITest gira a ogni push con soglia all'80%. Quando la suite cresce si possono alzare le soglie (per esempio al 90%) e usare l'analisi incrementale di PITest (`withHistory`) per tenere bassi i tempi.
