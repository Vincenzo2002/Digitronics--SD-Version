# syntax=docker/dockerfile:1

# ---------- Stage 1: build del WAR ----------
# Compila l'applicazione con Maven e Java 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Scarica le dipendenze prima dei sorgenti per riutilizzare la cache.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Genera il WAR; i test sono eseguiti prima dalla CI.
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: runtime su Tomcat ----------
# Prepara l'immagine finale con Tomcat e Java 21.
FROM tomcat:10.1-jdk21-temurin

# Imposta la memoria heap Java: iniziale 256 MB, massima 512 MB.
ENV CATALINA_OPTS="-Xms256m -Xmx512m"

# Crea l'utente Tomcat, assegna i permessi e rimuove le webapp preinstallate.
RUN groupadd --system tomcat \
 && useradd --system --gid tomcat --home-dir /usr/local/tomcat --shell /usr/sbin/nologin tomcat \
 && rm -rf /usr/local/tomcat/webapps/* \
 && chown -R tomcat:tomcat /usr/local/tomcat

# Copia solo il WAR compilato; ROOT.war rende il sito accessibile su "/".
COPY --from=build --chown=tomcat:tomcat /build/target/digitronics.war /usr/local/tomcat/webapps/ROOT.war

# Esegue Tomcat senza privilegi amministrativi.
USER tomcat
# La porta viene resa accessibile dal Compose.
EXPOSE 8080

# Avvia Tomcat in primo piano.
CMD ["catalina.sh", "run"]
