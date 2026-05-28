# Multi-stage build: 1) compila el jar con Gradle, 2) imagen runtime ligera
# === Stage 1: build ===
FROM gradle:8.5-jdk17-alpine AS builder
WORKDIR /app

# Copiar archivos del monorepo necesarios para resolver dependencias
COPY settings.gradle.kts gradle.properties ./
COPY build.gradle.kts ./
COPY gradle/ ./gradle/
COPY shared/ ./shared/
COPY backend/ ./backend/

# Construir fat jar
RUN gradle :backend:buildFatJar --no-daemon

# === Stage 2: runtime ===
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Usuario no-root por seguridad
RUN addgroup -S frutapp && adduser -S frutapp -G frutapp

COPY --from=builder /app/backend/build/libs/frutapp-backend.jar app.jar

RUN chown -R frutapp:frutapp /app
USER frutapp

EXPOSE 8080
ENV APP_ENV=production

CMD ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
