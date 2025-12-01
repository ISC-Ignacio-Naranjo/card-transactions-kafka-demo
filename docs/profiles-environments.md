# Profiles & Environments

This document describes how the **card-transactions-kafka-demo** project is configured to run in different environments using **Spring profiles** and externalized configuration.

---

## 🔧 Overview

The project uses Spring profiles to separate configuration by environment:

- **`dev`** – Local development:
  - Services run locally (IntelliJ / Maven).
  - Infrastructure (PostgreSQL + Kafka + Zookeeper) runs in Docker via `docker-compose.yml`.

- **`test`** – Cloud-like / container-friendly:
  - Designed for running in containers (Docker/Kubernetes/AKS).
  - All infrastructure endpoints (DB, Kafka, etc.) are read from **environment variables**.

Common configuration (application name, JPA settings, Actuator, topic names, etc.) lives in `application.yml`.

Environment-specific settings live in:

- `application-dev.yml`
- `application-test.yml`

for each microservice.

---

## 🧑‍💻 `dev` – Local development

**Goal:** Run the microservices locally and connect them to Kafka/PostgreSQL running in Docker.

### How to run

```bash
# From the project root
docker compose up -d zookeeper kafka transactions-postgres
```

Then start each service with the `dev` profile:

```bash
# transactions-service
cd transactions-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# fraud-service
cd ../fraud-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Infrastructure (Docker Compose)

In this mode:

- **PostgreSQL** is exposed on `localhost:5433`
- **Kafka** is exposed on `localhost:9092`
- **Zookeeper** is used by Kafka internally

The microservices run on the host (not in containers) and connect to these services via `localhost`.

### Example config – `transactions-service` (`application-dev.yml`)

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/transactions
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  kafka:
    bootstrap-servers: localhost:9092
```

### Example config – `fraud-service` (`application-dev.yml`)

```yaml
server:
  port: 8083

spring:
  kafka:
    bootstrap-servers: localhost:9092
```

### Optional: reduce Kafka log noise

To keep logs readable during local development, you can lower Kafka log levels in `application-dev.yml`:

```yaml
logging:
  level:
    org.apache.kafka: WARN
    org.springframework.kafka: WARN
```

---

## 🧪 `test` – Cloud-like / container-friendly

**Goal:** Prepare the services for running in a container-based environment such as Kubernetes/AKS or any cloud platform.

### How to run

Locally, you can simulate the `test` profile with defaults:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

In a real test/staging/prod environment, the platform will provide the proper environment variables.

### Key idea

- **No hard-coded hosts** in the profile.
- Database and Kafka endpoints are defined via **environment variables**, with reasonable defaults for local testing.

### Example – `transactions-service` (`application-test.yml`)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:transactions}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

### Example – `fraud-service` (`application-test.yml`)

```yaml
server:
  port: 8080

spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

### Expected environment variables in real environments

In a real **test / staging / production** setup, the platform should provide:

- `DB_HOST` – database host
- `DB_PORT` – database port
- `DB_NAME` – database name
- `DB_USERNAME` – database username
- `DB_PASSWORD` – database password
- `KAFKA_BOOTSTRAP_SERVERS` – Kafka / Event Hub bootstrap server(s)

This allows the same application artifacts to be deployed in different environments without code changes, just by changing profiles and environment variables.

---

## 🐳 Local Docker vs. real environments

### Local: `docker-compose.yml`

The `docker-compose.yml` file is meant for **local development only**. It:

- Starts **PostgreSQL**, **Zookeeper** and **Kafka**.
- Exposes:
  - PostgreSQL on `localhost:5433`
  - Kafka on `localhost:9092`
- Configures Kafka advertised listeners as:

  ```yaml
  KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
  ```

So that microservices running **on the host** can connect to Kafka without hostname issues.

### Real test/staging/prod

Real environments are expected to use managed services and platform configuration, for example:

- **Database:** Azure Database for PostgreSQL (or similar).
- **Messaging:** Azure Event Hubs (Kafka-compatible), Confluent Cloud, or a managed Kafka cluster.
- **Orchestration:** Kubernetes / AKS, which injects DB and Kafka configs via environment variables.

The `test` profile is already designed to consume these values via env vars.

---

## ✅ Summary

- `application.yml` → common settings (app name, JPA, Actuator, topic names, etc.).
- `application-dev.yml` → local development:
  - Services on the host.
  - Kafka/PostgreSQL in Docker.
  - Uses `localhost` for infrastructure.
- `application-test.yml` → cloud/container-friendly:
  - All infrastructure configured via environment variables.
  - Ready for AKS/Kubernetes/other cloud environments.

This setup lets you:

- Develop comfortably on your laptop, with a simple Docker Compose stack.
- Reuse the same codebase for cloud environments by just:
  - switching the active profile, and
  - providing the right env vars in the deployment manifests.
