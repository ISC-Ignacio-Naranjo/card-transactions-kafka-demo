# Card Transactions Kafka Demo

Sample project with **Java 21 + Spring Boot microservices**, asynchronous communication via **Kafka**, persistence in **PostgreSQL**, all orchestrated with **Docker Compose** and wired into **GitHub Actions** CI.  
It also includes a real **integration test** using **Testcontainers + PostgreSQL**.

---

## 🧱 High-level Architecture

Services:

- **transactions-service**
  - Exposes a REST API to create and query card transactions.
  - Persists transactions in PostgreSQL using Spring Data JPA.
  - Publishes a `TransactionCreated` event to Kafka whenever a transaction is created.

- **fraud-service**
  - Consumes `TransactionCreated` events from Kafka.
  - Applies simple fraud rules (e.g. based on transaction amount).
  - Exposes a REST API to query fraud decisions.

Infrastructure (defined in `docker-compose.yml`):

- **PostgreSQL** (`postgres`)
- **Zookeeper** (`zookeeper`)
- **Kafka** (`kafka`)
- **transactions-service**
- **fraud-service**

---

## 🛠️ Tech Stack

- **Language:** Java 21
- **Framework:** Spring Boot
  - Spring Web
  - Spring Data JPA
  - Spring for Apache Kafka
- **Database:** PostgreSQL
- **Messaging:** Kafka (Confluent image)
- **Containerization:** Docker & Docker Compose
- **Testing:** JUnit 5, AssertJ, Testcontainers
- **CI:** GitHub Actions

---

## 🚀 Running the System with Docker Compose

### Prerequisites
## How to Run the System

This project can be run in two main ways:

1. Full Docker Compose (all 5 services as containers).
2. Hybrid dev mode (infrastructure in Docker, microservices running locally with the `dev` profile).

---

### Option 1 – Full Docker Compose (5 containers)

In this mode everything runs in Docker: database, Kafka and both microservices.

From the project root:

```bash
cd /path/to/card-transactions-kafka-demo
docker compose up --build
```

This will start:

- transactions-postgres (PostgreSQL)
- zookeeper
- kafka
- transactions-service (port 8080)
- fraud-service (port 8081)

Check container status:

```bash
docker compose ps
```

Default exposed ports (host -> container):

- localhost:5433  -> PostgreSQL (5432 inside the container)
- localhost:9092  -> Kafka
- localhost:8080  -> transactions-service (container port 8080)
- localhost:8081  -> fraud-service (container port 8080)

#### Test the flow (Docker-only)

Create a transaction:

```bash
curl -X POST "http://localhost:8080/api/v1/transactions"   -H "Content-Type: application/json"   -d '{
        "userId": "fraud-user",
        "amount": 1234.56
      }'
```

If everything is working:

- The transactions-service will persist the transaction in PostgreSQL.
- It will publish a TransactionCreatedEvent to Kafka (topic `transaction.created.v2`).
- The fraud-service will consume the event and log the fraud evaluation.

To stop all containers:

```bash
docker compose down
```

---

### Option 2 – Hybrid Dev Mode (Docker infra + local services with `dev` profile)

This mode is convenient for development:

- Docker runs only the infrastructure: PostgreSQL, Kafka, ZooKeeper.
- The microservices run locally (via IntelliJ or Maven) with the `dev` profile.
- Local ports use 8082 and 8083 to avoid conflicts with the Docker mapping.

#### 1. Start infrastructure only

From the project root:

```bash
docker compose up -d zookeeper kafka transactions-postgres
```

#### 2. Start transactions-service (dev profile)

```bash
cd transactions-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev profile (`transactions-service/src/main/resources/application-dev.yml`):

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

The service will be available at:

- http://localhost:8082

#### 3. Start fraud-service (dev profile)

```bash
cd ../fraud-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Dev profile (`fraud-service/src/main/resources/application-dev.yml`):

```yaml
server:
  port: 8083

spring:
  kafka:
    bootstrap-servers: localhost:9092
```

The service will be available at:

- http://localhost:8083

#### 4. Test the flow (Hybrid mode)

Create a transaction:

```bash
curl -X POST "http://localhost:8082/api/v1/transactions"   -H "Content-Type: application/json"   -d '{
        "userId": "fraud-user",
        "amount": 1234.56
      }'
```

- The transactions-service will use PostgreSQL and Kafka in Docker.
- The fraud-service (running locally) will consume the event from Kafka and log the decision.

---

### Notes

- For local development, Option 2 (Hybrid + `dev` profile) is usually more convenient.
- For demo / "it just works" mode, Option 1 (full Docker, 5 containers) is simpler to start with a single command.
- For details about environment-specific configuration, see:
  - docs/profiles-environments.md
  - docs/kafka-docker-cheatsheet.md

## 📡 Main Endpoints

### 1. Create a Transaction

**Service:** `transactions-service`  
**URL:** `POST http://localhost:8080/api/v1/transactions`

Example request:

```bash
curl -X POST "http://localhost:8080/api/v1/transactions"   -H "Content-Type: application/json"   -d '{
        "userId": "fraud-user",
        "amount": 1234.56
      }'
```

Effects:

1. The transaction is stored in PostgreSQL.
2. A `TransactionCreated` event is published to Kafka (e.g. topic `transaction.created.v1`).
3. `fraud-service` consumes the event and evaluates fraud rules.

---

### 2. List Fraud Decisions

**Service:** `fraud-service`  
**URL:** `GET http://localhost:8081/api/v1/fraud/decisions`

Example:

```bash
curl "http://localhost:8081/api/v1/fraud/decisions"
```

It should return the fraud decisions calculated based on the consumed transaction events.

---

## 🧪 Tests & Testcontainers

In `transactions-service` there is a **real integration test** using **Testcontainers + PostgreSQL**:

- Class: `com.card.transactions.repository.TransactionRepositoryTest`
- Key aspects:
  - Annotated with `@SpringBootTest` and `@Testcontainers`.
  - Uses `PostgreSQLContainer` to spin up a **real PostgreSQL** instance in Docker only for the test.
  - Uses `@DynamicPropertySource` to dynamically configure:
    - `spring.datasource.url`
    - `spring.datasource.username`
    - `spring.datasource.password`

The test:

1. Builds a `TransactionEntity`.
2. Persists it using `TransactionRepository`.
3. Reads it back from the database.
4. Asserts that the data is consistent.

### Running tests locally

From the `transactions-service` module:

```bash
cd transactions-service
mvn test
```

Notes:

- `TransactionRepositoryTest` runs and uses Testcontainers.
- The Spring Initializr-generated test `TransactionsServiceApplicationTests.contextLoads` is **disabled** via `@Disabled` to avoid bringing up the full application context with real infrastructure config until a dedicated test profile is provided.

In **CI**, tests are currently skipped using `-DskipTests` to avoid dealing with Docker/Testcontainers on the GitHub runner for now.

---

## ⚙️ Continuous Integration (GitHub Actions)

The workflow file lives at:

```text
.github/workflows/ci.yml
```

The CI pipeline triggers on:

- `push` to `main` and `dev`
- `pull_request` targeting `main` or `dev`

Pipeline steps:

1. **Checkout** the repository.
2. Set up **JDK 21 (Temurin)**.
3. Build both services with Maven (skipping tests):

  - `transactions-service`
  - `fraud-service`

The current goal is to ensure that both microservices **compile and package successfully** on every change.

---

## 🤝 Collaboration & Branching Model

Branches:

- **`main`**
  - Protected branch.
  - Updated only via Pull Requests from `dev`.
  - Represents the “stable” state.

- **`dev`**
  - Default development branch.
  - Feature work is integrated here first.
  - Later merged into `main` via PR.

Typical collaboration flow:

1. Clone the repo and switch to `dev`:

   ```bash
   git checkout dev
   ```

2. (Recommended) Create a feature branch:

   ```bash
   git checkout -b feature/your-feature-name
   ```

3. Make changes and commit:

   ```bash
   git add .
   git commit -m "Describe your change"
   ```

4. Push the feature branch:

   ```bash
   git push origin feature/your-feature-name
   ```

5. Open a **Pull Request** on GitHub:
  - From `feature/your-feature-name` into `dev`.

6. After review and approval, merge into `dev`.  
   Later, `dev` is merged into `main` via another PR.

---
## 📚 Documentation

- [Kafka & Docker Cheat Sheet](docs/kafka-docker-cheatsheet.md)
- [Profiles & Environments](docs/profiles-environments.md)



## 🧹 Future Improvements

Some potential next steps:

- Add more integration tests (e.g. Kafka producer/consumer tests using `KafkaContainer`).
- Add observability (metrics / tracing) for both services.
- Extend fraud rules and persist fraud decisions in the database.
- Add Kubernetes manifests (AKS-ready) to deploy both services in a cluster.
- Enable Testcontainers-based integration tests in CI (Docker-enabled GitHub runners).

---
