package cl.frutapp.backend.plugins

import cl.frutapp.backend.error.PayloadTooLargeException
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.CallSetup
import io.ktor.server.application.install
import io.ktor.server.request.contentLength
import io.ktor.server.request.httpMethod

/**
 * Rechaza requests con body demasiado grande antes de que lleguen a los
 * handlers. Previene "body bombs" (500 MB de basura a endpoints multipart)
 * que causarian OOM al buffear el body antes del check por endpoint.
 *
 * Reglas:
 *   1. Content-Length > MAX_BODY_BYTES → 413.
 *   2. POST/PUT/PATCH sin Content-Length (chunked transfers) → 413. Cierra
 *      el bypass del Transfer-Encoding: chunked con body infinito.
 *
 * MAX_BODY_BYTES = 8 MB alcanza para los multipart legitimos (foto entrega
 * 5 MB + overhead). Payloads mas grandes → subir la constante o pasar a
 * per-endpoint.
 *
 * Implementacion: el hook `on(CallSetup)` se ejecuta al arranque de cada
 * llamada. TIRAMOS una PayloadTooLargeException que StatusPages mapea al
 * 413 estandar (patron `ApiException`). No usamos call.respond() + finish()
 * porque eso rompia el pipeline downstream de dos formas:
 *   - Antes: intercept(Setup) cortaba antes de que Monitoring seteara
 *     CallStartTime → CallFailed hook lanzaba IllegalStateException.
 *   - Con on(CallSetup) + respond(): el pipeline continuaba y trataba de
 *     setear headers a una respuesta ya cerrada → UnsupportedOperationException.
 * Tirar la excepcion es el patron idiomatico y encaja con el resto de
 * errores del backend.
 */
private const val MAX_BODY_BYTES: Long = 8L * 1024 * 1024  // 8 MB

private val METODOS_CON_BODY = setOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)

private val RequestSizeLimitPlugin = createApplicationPlugin(name = "RequestSizeLimit") {
    on(CallSetup) { call ->
        val length = call.request.contentLength()
        val esBodyMethod = call.request.httpMethod in METODOS_CON_BODY
        val demasiadoGrande = length != null && length > MAX_BODY_BYTES
        val chunkedSinLimite = esBodyMethod && length == null
        if (demasiadoGrande) {
            throw PayloadTooLargeException("Request body excede ${MAX_BODY_BYTES / 1024 / 1024} MB.")
        }
        if (chunkedSinLimite) {
            throw PayloadTooLargeException("Requests POST/PUT/PATCH deben declarar Content-Length. Transfer-Encoding: chunked no está soportado.")
        }
    }
}

fun Application.configureRequestSizeLimit() {
    install(RequestSizeLimitPlugin)
}
