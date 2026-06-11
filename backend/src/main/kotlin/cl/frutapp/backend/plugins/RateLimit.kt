package cl.frutapp.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.origin
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.header
import kotlin.time.Duration.Companion.seconds

/**
 * Rate limiting in-memory por IP para endpoints de auth.
 *
 * Configuracion conservadora para QA + demo: tope alto suficiente para flows
 * legitimos (incluyendo retries de red), suficientemente bajo para que un
 * atacante no pueda barrer passwords ni codigos OTP en minutos.
 *
 * Para escalar a multi-replica usar Redis-backed; en single-node como hoy el
 * mapa en memoria alcanza. Se reinicia con cada deploy del backend.
 *
 * Aplicacion en rutas: envolver el bloque con `rateLimit(RateLimitName("auth"))`.
 * Endpoints SIN gate (carga normal del cliente) no llevan rate limit.
 */
fun Application.configureRateLimit() {
    install(RateLimit) {
        // Pool "auth": login, register, refresh, forgot, verify. 10 req / 60s por IP.
        register(RateLimitName("auth")) {
            rateLimiter(limit = 10, refillPeriod = 60.seconds)
            requestKey { call -> call.request.clientIpKey() }
        }
        // Pool "auth-slow": forgot-password y verify-email (mandan mail). Mas estricto
        // para no spammear el inbox del usuario ni quemar la cuota de Resend.
        register(RateLimitName("auth-slow")) {
            rateLimiter(limit = 3, refillPeriod = 60.seconds)
            requestKey { call -> call.request.clientIpKey() }
        }
    }
}

/** Prefiere X-Forwarded-For (Traefik) sobre el remoteHost local. Cae al remote si
 *  el header no esta. Usamos solo la primera IP de la cadena para que un proxy
 *  intermedio no quede como key compartida entre clientes. */
private fun ApplicationRequest.clientIpKey(): String {
    val xff = header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
    if (!xff.isNullOrBlank()) return xff
    return origin.remoteHost
}
