# ===== STAGE 1: Build =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos los pom y el código
COPY pom.xml .
COPY src ./src

# Empaquetamos la app (sin tests si quieres que sea más rápido)
RUN mvn -DskipTests clean package

# ===== STAGE 2: Runtime =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copiamos el jar generado desde el stage de build
# Usamos un ARG para no depender del nombre exacto
ARG JAR_FILE=target/*.jar
COPY --from=build /app/${JAR_FILE} app.jar

# Puerto donde corre el transactions-service (según tu app)
EXPOSE 8080

# Variables de entorno por defecto (se pueden sobreescribir en docker-compose)
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/transactions
ENV SPRING_DATASOURCE_USERNAME=postgres
ENV SPRING_DATASOURCE_PASSWORD=postgres
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092

ENTRYPOINT ["java", "-jar", "app.jar"]
