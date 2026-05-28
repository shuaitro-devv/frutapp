# FrutApp

Monorepo Kotlin con:
- **`shared/`** — data classes, sealed events, DTOs compartidos entre app y backend
- **`backend/`** — API Ktor + Postgres (deploy en `frutapp-api.grandline.cl`)
- **`app/`** — App móvil Kotlin Multiplatform + Compose Multiplatform (Android primero, iOS-ready)

## Stack

- **Kotlin** 1.9.22
- **Compose Multiplatform** 1.6.0
- **Ktor** 2.3.9 (server + client)
- **Postgres** 16 (self-hosted en Contabo)
- **MinIO** S3-compatible (self-hosted, próximo sprint)
- **Auth:** JWT 15min + Refresh 60d (bcrypt)
- **DI:** Koin
- **DB cache app:** SQLDelight
- **Navigation:** Voyager

Documento de ingeniería completo en [`c:\others\own\docs\FrutApp_Ingenieria.md`](../docs/FrutApp_Ingenieria.md).

## Estructura

```
frutapp/
├── shared/                   ← código común (Kotlin/JVM y Android)
├── backend/                  ← Ktor server JVM
├── app/                      ← KMP + Compose Multiplatform (Android, iOS-ready)
├── docker-compose.yml        ← Postgres + MinIO local
├── gradle/libs.versions.toml ← catálogo central de dependencias
└── .github/workflows/        ← deploy CI/CD
```

## Desarrollo local

### Prerrequisitos
- JDK 17+ (verifica con `java -version`)
- Android Studio Koala+ (con Android SDK)
- Docker Desktop

### Levantar Postgres local
```bash
docker compose up -d
```

### Correr backend local
```bash
./gradlew :backend:run
# → http://localhost:8080
```

### Correr app
Abre el proyecto en Android Studio → Run → selecciona emulador → ▶️

La app debug apunta a `http://10.0.2.2:8080` (que es el `localhost` del host desde el emulador Android).

### Health check
```bash
curl http://localhost:8080/v1/health
```

## Deploy

El backend se despliega automáticamente a Contabo (`frutapp-api.grandline.cl`) al hacer push a `main` vía GitHub Actions.

## Licencia

© 2026 FrutApp · Confidencial
