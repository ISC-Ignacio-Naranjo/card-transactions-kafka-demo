# Kafka & Docker Cheat Sheet for Card Transactions Demo

This document summarizes the most useful Docker + Kafka commands for the
**card-transactions-kafka-demo** project.

The Kafka broker runs **`apache/kafka:4.0.0`** in **KRaft mode** (no ZooKeeper). All commands below
target the container by its Compose service name, `kafka`.

---

## 🧱 1. Start / Stop the Stack with Docker Compose

All commands assume you are in the project root, where `docker-compose.yml` lives.

### Start everything (DB + Kafka + services)

```bash
docker compose up -d --build
```

### See what is running

```bash
docker compose ps
```

### View logs

All services:

```bash
docker compose logs
```

Follow logs for specific services:

```bash
docker compose logs -f kafka transactions-service fraud-service
```

> `-f` means *follow* (stream logs in real time).

### Stop everything

```bash
docker compose down
```

---

## 🔍 2. Kafka CLI Tools Inside the Container

The `apache/kafka` image ships its CLI tools under `/opt/kafka/bin/`, and that directory is **not**
on the default `PATH`. Run each tool with its full path, either directly through
`docker compose exec` (no shell needed):

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
```

or by opening a shell first and using the full path for every command:

```bash
docker compose exec kafka bash
/opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
```

All examples below use `kafka:29092` — the **internal** listener. It works whether the command runs
from inside the container's own network namespace or from any other container on the same Docker
network. See the [Kafka Listeners](../README.md#-kafka-listeners) section in the README for why the
broker also exposes a separate `localhost:9092` listener for host-side clients.

---

## 📡 3. List Topics

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:29092 \
  --list
```

You should see, among others, the topic used by this project:

- `transaction.created.v2`

---

## ℹ️ 4. Describe a Topic

To inspect topic configuration (partitions, replicas, etc.):

```bash
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:29092 \
  --describe \
  --topic transaction.created.v2
```

---

## 📥 5. Consume Messages from a Topic

Use this to see the actual messages `transactions-service` sends to Kafka.

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:29092 \
  --topic transaction.created.v2 \
  --from-beginning
```

- `--from-beginning` tells Kafka to read all messages from offset 0 (full history).

While this command is running, send a transaction from outside (e.g. with `curl`):

```bash
curl -X POST "http://localhost:8080/api/v1/transactions" \
  -H "Content-Type: application/json" \
  -d '{
        "userId": "demo-user",
        "amount": 1234.56
      }'
```

You should see a JSON event appear in the consumer terminal.

To stop the consumer, press `Ctrl + C`.

### Show keys as well

`transactions-service` publishes events keyed by `userId`:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:29092 \
  --topic transaction.created.v2 \
  --from-beginning \
  --property print.key=true \
  --property key.separator=" - "
```

---

## 📤 6. Produce Messages Manually (Test fraud-service)

You can send a message directly to the topic to exercise `fraud-service` without going through the
REST API.

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server kafka:29092 \
  --topic transaction.created.v2
```

The console waits for input. The payload must match every field of the `TransactionCreatedEvent`
record (`id`, `userId`, `amount`, `status`, `createdAt`) — a JSON object missing `id` will fail to
deserialize on the consumer side. Type a JSON line and press Enter to send it:

```json
{"id":9001,"userId":"manual-test-user","amount":999.99,"status":"CREATED","createdAt":"2025-11-30T01:23:45Z"}
```

`fraud-service` should consume this message and log the fraud evaluation.

To stop the producer, press `Ctrl + C`.

---

## 🧹 7. Consumer Groups

`fraud-service` uses the consumer group id **`fraud-service-v2`**.

List all consumer groups:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --list
```

Describe the fraud-service group (offsets, lag, partition assignment):

```bash
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 \
  --describe \
  --group fraud-service-v2
```

---

## 🚪 8. Exit the Kafka Container

When you are done:

- Stop any running console consumer/producer with `Ctrl + C`.
- Exit the container shell (if you opened one) with `exit`.

---

This cheat sheet is tailored to the **card-transactions-kafka-demo** setup and the `apache/kafka`
image layout; the underlying commands are standard Kafka CLI tools and translate to other Kafka
distributions with minor path adjustments.
