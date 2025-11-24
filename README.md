# Card Transactions & Fraud Detection – Spring Boot + Kafka Demo

This repository contains a small event–driven system built with **Java 21**, **Spring Boot**, **Apache Kafka** and **PostgreSQL** to simulate a simple **card transactions** pipeline with a separate **fraud detection** service.

It is meant as a learning/demo project to showcase:

- Clean REST APIs with Spring Boot
- Persistence with Spring Data JPA + PostgreSQL
- Event–driven communication using Kafka (producer + consumer)
- Basic fraud detection rules in a separate microservice
- Centralized error handling and structured logging

---

## Architecture

**Services:**

- **`transactions-service`**
    - Exposes a REST API to create and read card transactions.
    - Persists transactions into PostgreSQL.
    - Publishes a `TransactionCreatedEvent` to Kafka after each successful creation.

- **`fraud-service`**
    - Listens to `TransactionCreatedEvent` messages from Kafka.
    - Applies simple fraud rules based on transaction amount.
    - Stores decisions in memory (for demo purposes).
    - Exposes REST endpoints to query fraud decisions.

**Infrastructure (local):**

- **PostgreSQL** (via Docker)
- **Kafka** broker (via Docker)
- Both services as separate Spring Boot apps (run with Maven)

### High–level flow

```text
[Client] ---> [transactions-service REST API] ---> [PostgreSQL]

[transactions-service] -- publishes --> [Kafka topic: transaction.created.v2]

[fraud-service] -- consumes --> [Kafka topic: transaction.created.v2]
               -- applies rules --> [Fraud decisions in memory]
               -- exposes --> [REST endpoints for decisions]
```

---

## Tech Stack

- **Language:** Java 21
- **Build:** Maven
- **Framework:** Spring Boot
    - Spring Web
    - Spring Data JPA
    - Spring Kafka
    - (Optionally) Spring Boot Actuator
- **Database:** PostgreSQL
- **Messaging:** Apache Kafka
- **Others:** Lombok, SLF4J logging

---

## Project Structure

Assuming the repo root is `card-transactions-kafka-demo/`:

```text
card-transactions-kafka-demo/
├── transactions-service/
│   ├── src/main/java/com/card/transactions/...
│   ├── src/main/resources/application.yml
│   └── docker-compose-kafka.yml      # Kafka broker (defined here)
│
└── fraud-service/
    ├── src/main/java/com/card/fraud/...
    └── src/main/resources/application.yml
```

> Kafka’s `docker-compose-kafka.yml` currently lives inside `transactions-service/`.

---

## Prerequisites

- **Java 21**
- **Maven 3.9+**
- **Docker** and **Docker Compose**
- **Git**

---

## How to run everything locally

### 1. Clone the repository

```bash
git clone https://github.com/ISC-Ignacio-Naranjo/card-transactions-kafka-demo.git
cd card-transactions-kafka-demo
```

---

### 2. Start PostgreSQL

From the repository root (or any folder), run:

```bash
docker run --name transactions-postgres   -e POSTGRES_USER=postgres   -e POSTGRES_PASSWORD=postgres   -e POSTGRES_DB=transactions   -p 5433:5432   -d postgres:16
```

This will expose Postgres on **localhost:5433**, matching the `transactions-service` configuration.

> If you already have something running on 5433, adjust the host port in the `-p` option and update `transactions-service/src/main/resources/application.yml` accordingly.

---

### 3. Start Kafka

Kafka is defined in `transactions-service/docker-compose-kafka.yml`.

From the repo root:

```bash
cd transactions-service
docker-compose -f docker-compose-kafka.yml up -d
cd ..
```

That will start a **single Kafka broker** on **localhost:9092**.

You can verify:

```bash
docker ps
```

You should see a container for Postgres and one for Kafka.

---

### 4. Run `transactions-service`

In a new terminal:

```bash
cd transactions-service
mvn spring-boot:run
```

By default it runs on **port 8080**.

Key configuration (from `transactions-service/src/main/resources/application.yml`):

- HTTP port: `8080`
- DB URL: `jdbc:postgresql://localhost:5433/transactions`
- DB user/password: `postgres` / `postgres`
- Kafka: `localhost:9092`
- Topic: `transaction.created.v2`

---

### 5. Run `fraud-service`

In another terminal:

```bash
cd fraud-service
mvn spring-boot:run
```

By default it runs on **port 8081**.

Key configuration (from `fraud-service/src/main/resources/application.yml`):

- HTTP port: `8081`
- Kafka: `localhost:9092`
- Topic: `transaction.created.v2`

Both services should start successfully once Postgres and Kafka are up.

---

## API Usage

### 1. Create a transaction

**Service:** `transactions-service`  
**Endpoint:** `POST /api/v1/transactions`

```bash
curl -X POST "http://localhost:8080/api/v1/transactions"   -H "Content-Type: application/json"   -d '{
        "userId": "fraud-user",
        "amount": 1234.56
      }'
```

Example response:

```json
{
  "id": 12,
  "userId": "fraud-user",
  "amount": 1234.56,
  "status": "CREATED",
  "createdAt": "2025-11-24T00:40:28.659887Z"
}
```

This will:

1. Persist the transaction to PostgreSQL.
2. Publish a `TransactionCreatedEvent` to Kafka.
3. Trigger the `fraud-service` listener.

---

### 2. List all transactions

**Service:** `transactions-service`  
**Endpoint:** `GET /api/v1/transactions`

```bash
curl "http://localhost:8080/api/v1/transactions"
```

---

### 3. Get transaction by id

**Service:** `transactions-service`  
**Endpoint:** `GET /api/v1/transactions/{id}`

```bash
curl "http://localhost:8080/api/v1/transactions/12"
```

---

### 4. List transactions by user

**Service:** `transactions-service`  
**Endpoint:** `GET /api/v1/transactions/user/{userId}`

```bash
curl "http://localhost:8080/api/v1/transactions/user/fraud-user"
```

---

## Fraud service – rules and endpoints

The `fraud-service` listens to the topic and applies **simple amount–based rules**:

- `amount >= 50,000` → **REJECTED**, risk = **HIGH**
- `amount >= 10,000` → **REVIEW**, risk = **MEDIUM**
- `amount < 10,000` → **CLEAR**, risk = **LOW**

Each decision is stored in memory as a `FraudDecision` object:

```java
record FraudDecision(
    Long transactionId,
    String userId,
    BigDecimal amount,
    String transactionStatus,
    String fraudStatus,
    String riskLevel,
    String reason,
    Instant evaluatedAt
)
```

---

### 1. List all fraud decisions

**Service:** `fraud-service`  
**Endpoint:** `GET /api/v1/fraud/decisions`

```bash
curl "http://localhost:8081/api/v1/fraud/decisions"
```

---

### 2. Get decision by transaction id

**Service:** `fraud-service`  
**Endpoint:** `GET /api/v1/fraud/decisions/transaction/{transactionId}`

Example (using `12` as transaction id):
  
```bash
curl "http://localhost:8081/api/v1/fraud/decisions/transaction/12"
```

Example response:

```json
{
  "transactionId": 12,
  "userId": "fraud-user",
  "amount": 1234.56,
  "transactionStatus": "CREATED",
  "fraudStatus": "CLEAR",
  "riskLevel": "LOW",
  "reason": "No risk rules matched",
  "evaluatedAt": "2025-11-24T00:40:28.900000Z"
}
```

---

## Error handling & logging

- `transactions-service` uses **centralized error handling** (`@RestControllerAdvice`) to return consistent JSON error responses (404 for not-found, 400 for validation errors, etc.).
- Logging is done via **SLF4J + Lombok `@Slf4j`** in both services.
- Kafka configuration:
    - Producer uses `JacksonJsonSerializer` to serialize `TransactionCreatedEvent` to JSON.
    - Consumer uses `JacksonJsonDeserializer<TransactionCreatedEvent>` with `ignoreTypeHeaders()` to avoid tight coupling to the producer’s Java class name.

---

## Possible next steps

Some ideas for improving/expanding this demo:

- Persist fraud decisions to a real database instead of in-memory.
- Add more advanced fraud rules or integrate a machine learning model.
- Containerize both services and run everything (Postgres + Kafka + services) from a single `docker-compose.yml`.
- Add Kubernetes manifests and deploy to AKS/EKS/GKE.
- Add CI/CD with GitHub Actions (build, test, Docker image, deploy).

---

## License

