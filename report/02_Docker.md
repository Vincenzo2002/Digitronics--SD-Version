# Containerizzazione di Digitronics

## Obiettivo e configurazione

Sono state verificate la distribuibilità di Digitronics, la capacità di recupero dopo un'interruzione del database e la persistenza dei dati. Dockerfile e Compose preesistenti sono stati revisionati. È stata attivata la verifica delle connessioni nel pool JDBC ed è stata aggiunta la pipeline di pubblicazione su DockerHub.

Il [Dockerfile](../Dockerfile) usa una build multi-stage: Maven con Java 21 produce il WAR, poi lo trasferisce a Tomcat 10.1. Copiare prima il pom favorisce la cache. Sono usati `WORKDIR`, `COPY`, `CMD` exec e `USER tomcat`, verificato a runtime con UID 999. La CI esegue i test prima della build Docker, che usa `-DskipTests`.

Il [Compose](../docker-compose.yml) separa `web` e MySQL 8.4 (`db`) sulla rete interna. `db_data` conserva il database; gli script SQL, montati in sola lettura, inizializzano soltanto un volume nuovo. `depends_on: service_healthy` ordina l'avvio; entrambi i servizi hanno health check e `restart: on-failure`. Solo Tomcat pubblica una porta sull'host, la 8080.

La password del database è letta da un file locale montato in `/run/secrets`, escluso da Git e dal contesto di build. I GitHub Secrets `DOCKER_USERNAME` e `DOCKER_PASSWORD` contengono invece username e PAT DockerHub. Sono evitati password nei file Docker, basi `latest`, `ADD`, esecuzione privilegiata dell'applicazione e aggiornamenti `apt-get upgrade`; `.dockerignore` limita il contesto a pom e sorgenti.

## Esperimenti e risultati

L'ambiente è stato avviato con `docker compose -p digitronics-lab2a up --build --detach`. Alla prima interruzione di MySQL con `stop db`, l'applicazione non recuperava dopo `start db`: la homepage restituiva HTTP 500, mentre una connessione JDBC nuova trovava 60 prodotti, di cui 33 in vetrina.

In `ConPool.java` sono stati aggiunti `testOnBorrow`, `SELECT 1`, timeout di validazione di 5 secondi e intervallo zero. Dopo la ricostruzione dell'immagine, la ripetizione della prova ha confermato il recupero dopo il riavvio del database. La successiva revisione ha verificato homepage HTTP 200 e ricerca con 60 prodotti.

Per la persistenza, dopo la registrazione di un account, `down` senza `-v` e ricreazione dei container, il login è riuscito.

La [prima pubblicazione su DockerHub](https://github.com/Vincenzo2002/Digitronics--SD-Version/actions/runs/44061402928) è riuscita per AMD64 e ARM64 dal branch `lab-2a-containerization`, abilitato temporaneamente per la prova. L'immagine scaricata da DockerHub è stata avviata su ARM64, dove la homepage ha restituito HTTP 200 e la ricerca ha trovato 60 prodotti.

## Self-audit della pipeline Docker

La pipeline definita in [ci.yml](../.github/workflows/ci.yml) e [docker-build.yml](../.github/workflows/docker-build.yml) adotta le seguenti scelte di configurazione:

1. **Attivazione.** `needs: build-and-test` consente la build Docker soltanto dopo il successo dei controlli Java. Il workflow riutilizzabile usa lo stesso commit della CI. Le PR eseguono una verifica senza pubblicazione. L'immagine viene caricata su DockerHub solo dopo un push su `master`, incluso il merge di una PR, oppure un avvio manuale della CI su `master`.
2. **Secrets e permessi.** Il login usa i GitHub Secrets DockerHub, passati solo alla pubblicazione. Le PR non ricevono queste credenziali. `GITHUB_TOKEN` ha `contents: read`; le action del workflow Docker sono fissate a SHA.
3. **Piattaforme.** Le PR verificano AMD64; la pubblicazione produce immagini AMD64 e ARM64 tramite Buildx e QEMU, per supportare entrambe le architetture.
4. **Cache.** La CI conserva le dipendenze Maven con `setup-java`. BuildKit usa la cache GitHub Actions e `mode=max` per riutilizzare anche gli stadi intermedi del Dockerfile.
5. **Tag e tracciabilità.** La metadata action genera label e tag del branch e del commit completo; `latest` è riservato a master. Lo SHA identifica i sorgenti, mentre il digest identifica l'immagine prodotta.
