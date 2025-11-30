# Kafka & Docker Cheat Sheet for Card Transactions Demo

This document summarizes the most useful Docker + Kafka commands for the **card-transactions-kafka-demo** project.

---

## 🧱 1. Start / Stop the Stack with Docker Compose

All commands assume you are in the project root, where `docker-compose.yml` lives.

### Start everything (DB + Kafka + services)

```bash
docker compose up --build
```

Detached mode (does not attach logs to your terminal):

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

## 🔍 2. Enter the Kafka Container

First, ensure Kafka is running:

```bash
docker compose ps
```

You should see `kafka` with state `Up`.

Then open a shell in the Kafka container:

```bash
docker compose exec kafka bash
```

Your prompt should change to something like:

```bash
root@kafka:/#
```

All `kafka-*` commands below are executed **inside** this shell.

---

## 📡 3. List Topics

Inside the Kafka container:

```bash
kafka-topics   --bootstrap-server localhost:9092   --list
```

You should see, among others, the topic where the transaction service publishes events, e.g.:

- `transaction.created.v1` (or the name configured in your code).

---

## ℹ️ 4. Describe a Topic

To inspect topic configuration (partitions, replicas, etc.):

```bash
kafka-topics   --bootstrap-server localhost:9092   --describe   --topic transaction.created.v1
```

---

## 📥 5. Consume Messages from a Topic

Use this to see the actual messages your `transactions-service` is sending to Kafka.

```bash
kafka-console-consumer   --bootstrap-server localhost:9092   --topic transaction.created.v1   --from-beginning
```

- `--from-beginning` tells Kafka to read all messages from offset 0 (full history).

While this command is running, send a transaction from outside (e.g. with `curl`):

```bash
curl -X POST "http://localhost:8080/api/v1/transactions"   -H "Content-Type: application/json"   -d '{
        "userId": "fraud-user",
        "amount": 1234.56
      }'
```

You should see a JSON event appear in the consumer terminal.

To stop the consumer:

```text
Ctrl + C
```

### Show keys as well (if you use them)

```bash
kafka-console-consumer   --bootstrap-server localhost:9092   --topic transaction.created.v1   --from-beginning   --property print.key=true   --property key.separator=" - "
```

---

## 📤 6. Produce Messages Manually (Test fraud-service)

You can send messages manually to the topic to test `fraud-service` even without calling the REST API.

Inside the Kafka container:

```bash
kafka-console-producer   --bootstrap-server localhost:9092   --topic transaction.created.v1
```

The console will wait for input. Type a JSON line and press Enter to send a message, for example:

```json
{"userId":"manual-test","amount":999.99,"status":"CREATED","createdAt":"2025-11-30T01:23:45Z"}
```

(then press Enter)

Your `fraud-service` should consume this message and log the fraud evaluation.

To stop the producer:

```text
Ctrl + C
```

---

## 🧹 7. Consumer Groups (Optional)

To inspect consumer groups (e.g. the group used by `fraud-service`):

List all consumer groups:

```bash
kafka-consumer-groups   --bootstrap-server localhost:9092   --list
```

Describe a specific group (replace `fraud-service` with your actual group id):

```bash
kafka-consumer-groups   --bootstrap-server localhost:9092   --describe   --group fraud-service
```

This shows offsets, lag, and partition assignments for that group.

---

## 🚪 8. Exit the Kafka Container

When you are done:

- Stop any running `kafka-console-consumer` or `kafka-console-producer` with `Ctrl + C`.
- Exit the container shell with:

```bash
exit
```

---

This cheat sheet is tailored to the **card-transactions-kafka-demo** setup, but the commands are standard Kafka CLI tools and can be reused in other projects as well.
