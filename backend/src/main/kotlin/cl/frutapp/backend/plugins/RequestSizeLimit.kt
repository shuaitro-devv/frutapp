package cl.frutapp.backend.plugins

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.contentLength
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond

/**
 * Rechaza requests con body demasiado grande antes de que lleguen a los
 * handlers. Previene "body bombs" (500 MB de basura a endpoints multipart)
 * que causarian OOM al buffear el body antes de validar tamaño en el
 * handler (readBytes() lee TODO antes del check por endpoint).
 *
 * Reglas:
 *   1. Si el request tiene Content-Length > MAX_BODY_BYTES → 413.
 *   2. Si es un metodo body-carrying (POST/PUT/PATCH) y NO declara
 *      Content-Length → tambien 413. Esto cubre el bypass via
 *      `Transfer-Encoding: chunked` con body infinito, que sin este check
 *      dejaba pasar el request y readBytes() bufferaba el stream entero.
 *
 * MAX_BODY_BYTES = 8 MB alcanza para nuestros multipart (foto entrega 5 MB
 * + overhead). Si aparece un endpoint con payload legitimo mas grande
 * (video), subir la constante o pasar a per-endpoint.
 */
private const val MAX_BODY_BYTES: Long = 8L * 1024 * 1024  // 8 MB

private val METODOS_CON_BODY = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)

fun Application.configureRequestSizeLimit() {
    intercept(ApplicationCallPipeline.Setup) {
        val length = call.request.contentLength()
        val esBodyMethod = call.request.httpMethod in METODOS_CON_BODY
        val demasiadoGrande = length != null && length > MAX_BODY_BYTES
        val chunkedSinLimite = esBodyMethod && length == null
        if (demasiadoGrande || chunkedSinLimite) {
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                mapOf(
                    "error" to "Request body demasiado grande. Maximo ${MAX_BODY_BYTES / 1024 / 1024} MB.",
                    "requiereContentLength" to chunkedSinLimite,
                ),
            )
            finish()
        }
    }
}
