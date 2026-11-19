# syntax=docker/dockerfile:1
# Contexto: PIPA/ (inclui os módulos Java vizinhos).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY API_AI/pom.xml API_AI/pom.xml
COPY API_AI/src API_AI/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -f API_AI/pom.xml install -Dmaven.test.skip=true

COPY PIPA_INTEGRATOR/pom.xml PIPA_INTEGRATOR/pom.xml
COPY PIPA_INTEGRATOR/src PIPA_INTEGRATOR/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -f PIPA_INTEGRATOR/pom.xml install -Dmaven.test.skip=true

COPY PIPA_EMAIL/pom.xml PIPA_EMAIL/pom.xml
COPY PIPA_EMAIL/src PIPA_EMAIL/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -f PIPA_EMAIL/pom.xml install -Dmaven.test.skip=true

COPY PIPA_MIDDLEWARE/pom.xml PIPA_MIDDLEWARE/pom.xml
COPY PIPA_MIDDLEWARE/src PIPA_MIDDLEWARE/src
# Consumido como biblioteca pelo Core, sem reempacotar como aplicação Boot.
RUN --mount=type=cache,target=/root/.m2 mvn -B -f PIPA_MIDDLEWARE/pom.xml install -Dmaven.test.skip=true -Dspring-boot.repackage.skip=true

COPY UEG_PROVIDER/pom.xml UEG_PROVIDER/pom.xml
COPY UEG_PROVIDER/src UEG_PROVIDER/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -f UEG_PROVIDER/pom.xml install -Dmaven.test.skip=true

COPY PIPA/pom.xml PIPA/pom.xml
COPY PIPA/src PIPA/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -f PIPA/pom.xml package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl fonts-dejavu-core && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/PIPA/target/PIPA-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
