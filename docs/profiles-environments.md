# Profiles & Environments

This document describes how the **card-transactions-kafka-demo** project is configured across
environments using **Spring profiles** and externalized configuration.

---

## 🔧 Overview

Each service ships three configuration files:

- **`application.yml`** — common settings (application name, JPA settings for
  `transactions-service`, Kafka consumer settings for `fraud-service`, Actuator exposure, the Kafka
  topic name). This is what Spring Boot loads when **no profile is active**.
- **`application-dev.yml`** — local development settings, activated explicitly with
  `-Dspring-boot.run.profiles=dev` when running a service directly on the host.
- **`application-test.yml`** — an environment-variable-driven configuration file, present in both
  services but **not activated automatically anywhere in this repository**. `SPRING_PROFILES_ACTIVE`
  is never set — not in `docker-compose.yml`, not in either Dockerfile, and not in CI. If you want to
  exercise it, you must activate it explicitly (see below).

---

## 🧑‍💻 `dev` – Local development

**Goal:** run the microservices locally and connect them to Kafka/PostgreSQL running in Docker.

### How to run

```bash
# From the project root — starts PostgreSQL and Kafka only
docker compose up -d postgres kafka
```

Then start each service with the `dev` profile, using the Maven Wrapper (no global Maven install
required):

```bash
# transactions-service
cd transactions-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# fraud-service
cd ../fraud-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Infrastructure (Docker Compose)

In this mode:

- **PostgreSQL** is exposed on `localhost:5433`.
- **Kafka** runs in **KRaft mode** (`apache/kafka:4.0.0`, no ZooKeeper) and exposes its host-facing
  listener on `localhost:9092`.

The microservices run on the host (not in containers) and connect to both via `localhost`.

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

Both `application-dev.yml` files already lower the Kafka client log level to keep local output
readable:

```yaml
logging:
  level:
    org.apache.kafka: WARN
    org.springframework.kafka: WARN
```

---

## 🧪 `test` – Environment-variable-driven configuration (not auto-activated)

**Goal:** a configuration file with no hard-coded hosts, so that database and Kafka endpoints come
entirely from environment variables. This is the shape a container- or cloud-style deployment would
need, but at the moment nothing in this repository sets `SPRING_PROFILES_ACTIVE=test` — Docker
Compose, the Dockerfiles, and the CI workflow all leave the active profile unset, so both services
run with the plain `application.yml` defaults.

### How to activate it manually

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

Without the corresponding environment variables set, the defaults below apply.

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

### Environment variables this profile reads

If activated, the profile would read:

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` — database connection.
- `KAFKA_BOOTSTRAP_SERVERS` — Kafka bootstrap address.

This pattern would let the same application artifact target a managed PostgreSQL instance and a
Kafka-compatible broker without code changes, just by providing environment variables — but that
deployment target does not exist in this repository today; no Kubernetes/AKS manifests are included,
and this is tracked as future work in the [README](../README.md#-future-improvements).

---

## 🐳 How Docker Compose actually configures the services today

`docker-compose.yml` does **not** set `SPRING_PROFILES_ACTIVE` for either service — it runs them
with the default `application.yml` and overrides individual values directly through plain Spring
Boot relaxed-binding environment variables:

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/transactions
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
```

`transactions-service` and `fraud-service` reach Kafka at `kafka:29092` (the internal Docker
listener) in this mode. Host-side tools and the `dev` profile instead use `localhost:9092`. See the
[Kafka Listeners](../README.md#-kafka-listeners) section in the README for why both exist.

---

## ✅ Summary

| File | When it applies | Purpose |
|---|---|---|
| `application.yml` | Always (default) | Common settings: app name, JPA, Actuator, topic name. Active in Full Docker Compose mode, since no profile is set there. |
| `application-dev.yml` | `-Dspring-boot.run.profiles=dev` | Local development: services on the host, infrastructure in Docker, `localhost` addresses. |
| `application-test.yml` | `-Dspring-boot.run.profiles=test` (manual only) | Environment-variable-driven configuration; not activated by Docker Compose, the Dockerfiles, or CI today. |

Docker Compose provides connection details directly via environment variables rather than through
a named Spring profile — the `test` profile remains available for a future container/cloud
deployment path that has not been implemented yet.