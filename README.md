# Digitronics

[![CI](https://github.com/Vincenzo2002/Digitronics--SD-Version/actions/workflows/ci.yml/badge.svg)](https://github.com/Vincenzo2002/Digitronics--SD-Version/actions/workflows/ci.yml)

Digitronics is a full-stack e-commerce web application for consumer electronics, written in **Java 21** with **Jakarta Servlets**, **JSP**, **MySQL** and **Maven**, and deployed on **Apache Tomcat 10.1**.

It was developed as a team project for the Software Engineering course at the **University of Salerno**, with a focus on the design, implementation and testing of a complete web application using a layered architecture.

---

## Quick start (Docker)

The only requirement is [Docker](https://docs.docker.com/get-docker/) with Docker Compose v2. You do not need to install a JDK, Maven, MySQL or Tomcat.

```bash
git clone https://github.com/Vincenzo2002/Digitronics--SD-Version.git
cd Digitronics--SD-Version

# 1. Create the local secret only if it does not exist
cp -n secrets/db_password.txt.example secrets/db_password.txt
# Windows: copy secrets\db_password.txt.example secrets\db_password.txt (only if missing)

# 2. Set a dedicated password in secrets/db_password.txt, then build and start
docker compose up --build
```

Then open **<http://localhost:8080>**.

On first start, MySQL creates the `digitronics` database and loads the sample data from [`sql/`](sql/). The web container starts only after the database reports healthy.

The password file must contain only the password, on one line. It is excluded from Git and the Docker build context. Keep the existing value when reusing a database volume: editing the file alone does not change the password of an existing MySQL account.

| Task | Command |
|---|---|
| Start in the background | `docker compose up -d --build` |
| Follow the web app logs | `docker compose logs -f web` |
| Stop | `docker compose down` |
| Stop and **delete the database** (re-runs the SQL scripts next time) | `docker compose down -v` |

> The SQL scripts run only when the `db_data` volume is empty; they are not migrations for an existing database. `docker compose down -v` permanently deletes the volume and its data. Use it only for an intentional reset of disposable data; use a reviewed migration when existing data must be preserved.

### How the containers are set up

```text
            host :8080
                │
┌───────────────▼──────────────┐        ┌──────────────────────────┐
│ web                          │  JDBC  │ db                       │
│ tomcat:10.1-jdk21 (non-root) ├───────►│ mysql:8.4                │
│ ROOT.war                     │        │ volume: db_data          │
└──────────────────────────────┘        └──────────────────────────┘
         └──────── secret: db_password ────────┘
```

* [`Dockerfile`](Dockerfile) uses a multi-stage build: Maven builds the WAR, and only the WAR is copied into a Tomcat image that runs as an unprivileged `tomcat` user.
* [`docker-compose.yml`](docker-compose.yml) defines the `web` and `db` services, a named volume, health checks, `restart: on-failure`, and the database password as a Docker secret.

The database check tests whether MySQL responds over TCP. The web check only tests whether Tomcat's TCP port is open; it does not verify HTTP responses or database queries. `depends_on` controls initial startup ordering, while the JDBC pool validates connections before reuse to support recovery after a database restart. Functional checks should include browsing products and searching after recovery.

Compose publishes HTTP on port 8080 for local use. A production deployment requires separate TLS termination, for example with a reverse proxy.

---

## Features

* User registration and authentication
* Product catalog, product details, search and category browsing
* Shopping cart management
* Checkout and order creation, order history
* Product reviews
* Administration area for product management

## Architecture

The backend is split into functional modules (`autenticazione`, `registrazione`, `gestioneCarrello`, `gestioneOrdine`, `gestioneProdotto`, `gestioneRecensione`, `infoProdotto`). Each module follows a layered architecture:

```text
HTTP request → Servlet (controller) → Service → DAO → MySQL
```

The presentation layer is built with JSP/JSTL, CSS and JavaScript.

```text
src/main/java/it/unisa/nc26/digitronics/
├── <module>/controller   # Servlets
├── <module>/service      # Business logic
├── model/bean            # Domain objects
├── model/dao             # JDBC data access
└── utils                 # Connection pool, init servlet, ...
src/main/webapp/          # JSP, CSS, JS, images
src/test/                 # JUnit / Mockito / H2 tests
sql/                      # MySQL schema and seed data
```

## Tech stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Jakarta Servlet 6, JDBC, Tomcat JDBC pool |
| Frontend | JSP / JSTL, HTML, CSS, JavaScript |
| Database | MySQL 8 (H2 for integration tests) |
| Build | Maven (Maven Wrapper included) |
| Testing | JUnit Jupiter 6.1.3, Mockito 5.24.0, H2 |
| Runtime | Apache Tomcat 10.1, Docker, Docker Compose |
| CI | GitHub Actions |

---

## Configuration

The database connection is configured with environment variables. The defaults match a local MySQL installation.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/digitronics?serverTimezone=<JVM timezone>` | JDBC URL |
| `DB_USER` | `root` | Database user |
| `DB_PASSWORD` | `root` | Database password |
| `DB_PASSWORD_FILE` | – | Path to a file containing the password, such as a Docker secret. Takes precedence over `DB_PASSWORD`. |

## Running without Docker

Requirements: JDK 21, MySQL 8, Apache Tomcat 10.1.

1. Create the database by running the scripts in this order:
   `sql/DigitronicsDB.sql`, `sql/AltriInserimenti.sql`, `sql/InserimentoProdotti.sql`, `sql/Utenti.sql`.
2. If your MySQL credentials are not `root`/`root`, set `DB_URL`, `DB_USER` and `DB_PASSWORD` in the environment Tomcat runs in (for example in `bin/setenv.sh` or `bin/setenv.bat`).
3. Build the WAR:
   ```bash
   ./mvnw clean package        # Windows: mvnw.cmd clean package
   ```
4. Copy `target/digitronics.war` to Tomcat's `webapps/` directory. Rename it to `ROOT.war` to serve the app on `/`.

## Testing

```bash
./mvnw clean verify
```

The suite includes unit tests for modules such as authentication, registration and the shopping cart. Integration tests cover orders, products and reviews. Test reports are written to `target/surefire-reports/`.

### Testing approach

* **Unit tests** (JUnit 6.1.3, Mockito 5.24.0): services, servlets, the OpenStreetMap response parsing and password hashing in `Utente`. DAOs, the Servlet API and the OpenStreetMap address API are replaced by mocks.
* **Integration tests** (JUnit 6.1.3, in-memory H2 in MySQL mode): services and controllers of the order, product and review modules using a dedicated test schema. The address API is mocked here too, so no test needs network access.
* **System tests** (Selenium IDE): the test cases in [`projectDocs/testing/`](projectDocs/testing/).

Tests follow the FIRST principles and cover critical paths, boundary values, error conditions and business rules.

The testing strategy, tested scenarios and local results are described in [`report/03_testing.md`](report/03_testing.md).

## Continuous integration

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs `./mvnw clean verify` on JDK 21 for every push to any branch and every pull request to `main`/`master`, and can also be started manually from the Actions tab. It uploads the test reports and the WAR as build artifacts.

After `build-and-test` succeeds, pull requests also validate the Docker build for `linux/amd64`, without DockerHub credentials or publishing. Pushes and manual CI runs on `master` publish `linux/amd64` and `linux/arm64` images through [`.github/workflows/docker-build.yml`](.github/workflows/docker-build.yml) to [DockerHub](https://hub.docker.com/r/vincenzo200230/digitronics). Images receive branch and full commit SHA tags; only `master` updates `latest`.

The Docker workflow reads `DOCKER_USERNAME` and `DOCKER_PASSWORD` from GitHub repository secrets. `DOCKER_PASSWORD` must contain a DockerHub personal access token with read/write permissions. These registry credentials are separate from the local database password file.

The original build-and-test pipeline and its practices are described in [`report/01_CICD.md`](report/01_CICD.md).

Containerization choices, recovery and persistence experiments, DockerHub publishing results, and the self-audit of the Digitronics Docker pipeline are documented in [`report/02_Docker.md`](report/02_Docker.md).

---

## Project documentation

* [`projectDocs/`](projectDocs/): Software Engineering documentation (RAD, SDD, ODD, traceability matrix, testing material)
* [`docs/`](docs/): generated Javadoc

> All users and other data in the SQL seed files are dummy data created for testing purposes only.
