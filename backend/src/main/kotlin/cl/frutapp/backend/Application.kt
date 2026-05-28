package cl.frutapp.backend

import cl.frutapp.backend.plugins.configureMonitoring
import cl.frutapp.backend.plugins.configureRouting
import cl.frutapp.backend.plugins.configureSerialization
import cl.frutapp.backend.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

/**
 * Entrada principal del backend FrutApp.
 *
 * El método `main` lo provee Ktor's EngineMain — lee `application.yaml` y arranca el server.
 * Cada `Application.configureXxx()` es un plugin/feature; se mantienen separados para
 * legibilidad y testing.
 */
fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    configureSerialization()
    configureStatusPages()
    configureMonitoring()
    configureRouting()
}
