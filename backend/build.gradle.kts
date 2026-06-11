plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

group = "cl.frutapp"
version = "0.1.0"

application {
    mainClass.set("cl.frutapp.backend.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":shared"))

    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // Auth (JWT)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.bcrypt)

    // Storage S3-compatible (MinIO) para avatars/media
    implementation(libs.minio)

    // Base de datos: Exposed + Postgres + HikariCP + Flyway
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)

    // Coroutines (transacciones suspendidas de Exposed)
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.logback.classic)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Tests
    testImplementation(libs.ktor.server.tests)
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // BD efímera real (Postgres en Docker, descartable) para tests de integración.
    testImplementation("org.testcontainers:postgresql:1.19.7")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    // Local (Windows): se usa un Postgres efímero por CLI vía TEST_DB_* (ver
    // scripts/run-backend-tests.ps1), porque Testcontainers no autodetecta Docker Desktop.
    // CI (Linux): Testcontainers funciona solo. Ryuk off evita el reaper sobre npipe.
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")
}

ktor {
    fatJar {
        archiveFileName.set("frutapp-backend.jar")
    }
}

// El fat jar de Ktor usa Shadow. Sin esto, Shadow PISA los META-INF/services en vez
// de concatenarlos y Flyway pierde su resolver de migraciones SQL (el .sql "se detecta
// pero no corre"). mergeServiceFiles() concatena los SPI de flyway-core + postgresql.
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
}
