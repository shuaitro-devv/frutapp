package cl.frutapp.backend.plugins

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.application.install
import io.ktor.server.request.contentLength
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond

/**
 * Rechaza requests con body demasiado grande antes de que lleguen a los
 * handlers. Previene "body bombs" (500 MB de basura a endpoints multipart)
 * que causarian OOM al buffear el body antes del check por endpoint.
 *
 * Reglas:
 *   1. Content-Length > MAX_BODY_BYTES → 413.
 *   2. POST/PUT/PATCH sin Content-Length (chunked transfers) → 413. Cierra
 *      el bypass del `Transfer-Encoding: chunked` con body infinito que
 *      hoy dejaba pasar el request y readBytes() bufferaba el stream entero.
 *
 * MAX_BODY_BYTES = 8 MB alcanza para los multipart legitimos (foto entrega
 * 5 MB + overhead). Payloads mas grandes → subir la constante o pasar a
 * per-endpoint.
 *
 * Implementacion: usamos el hook `CallSetup` (equivalente a la fase Setup)
 * pero via createApplicationPlugin, que maneja bien la interaccion con los
 * plugins de Monitoring/CallLogging. Antes usabamos `intercept(Setup)` +
 * `finish()` pero eso rompia downstream (CallStartTime attribute nunca se
 * setteaba porque cortabamos el pipeline).
 */
private const val MAX_BODY_BYTES: Long = 8L * 1024 * 1024  // 8 MB

private val METODOS_CON_BODY = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)

private val RequestSizeLimitPlugin = createApplicationPlugin(name = "RequestSizeLimit") {
    on(CallSetup) { call ->
        val length = call.request.contentLength()
        val esBodyMethod = call.request.httpMethod in METODOS_CON_BODY
        val demasiadoGrande = length != null && length > MAX_BODY_BYTES
        val chunkedSinLimite = esBodyMethod && length == null
        if (demasiadoGrande || chunkedSinLimite) {
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                mapOf(
                    "error" to "payload_too_large",
                    "message" to "Request body demasiado grande. Maximo ${MAX_BODY_BYTES / 1024 / 1024} MB.",
                    "requiereContentLength" to chunkedSinLimite,
                ),
            )
        }
    }
}

fun Application.configureRequestSizeLimit() {
    install(RequestSizeLimitPlugin)
}
