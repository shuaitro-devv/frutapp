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
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Auth (JWT)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.bcrypt)

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
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}

ktor {
    fatJar {
        archiveFileName.set("frutapp-backend.jar")
    }
}
