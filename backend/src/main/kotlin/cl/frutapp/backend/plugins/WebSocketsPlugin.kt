package cl.frutapp.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets

/**
 * Plugin de WebSockets. Usado por el chat in-app (`/v1/orders/{id}/chat/ws`).
 *
 *  - pingPeriodMillis 30s + timeoutMillis 60s: si el cliente no responde un
 *    ping en 60s, Ktor cierra la conexion automaticamente. Asi se limpian
 *    zombies cuando se cae la red sin que el cliente cierre limpiamente.
 *  - maxFrameSize 1MB: los mensajes de chat son texto puro y nunca llegan a
 *    ese limite; el cap evita que un cliente malicioso intente enviar payload
 *    gigante. No esperamos NUNCA recibir frames del cliente igual.
 */
fun Application.configureWebSocketsPlugin() {
    install(WebSockets) {
        pingPeriodMillis = 30_000L
        timeoutMillis = 60_000L
        maxFrameSize = 1L * 1024 * 1024
        masking = false
    }
}
