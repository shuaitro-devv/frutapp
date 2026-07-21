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

/** Resuelve la IP real del cliente para keying del rate limit.
 *
 *  IMPORTANTE (fix v0.1.18): la implementacion anterior tomaba el PRIMER
 *  valor de X-Forwarded-For, que es TRIVIALMENTE spoofable porque los
 *  proxies APPENDEAN al header sin verificar los valores previos. Un
 *  atacante que manda `X-Forwarded-For: 1.2.3.4` en cada request rota su
 *  key y salta el limite. Afecta TODO el pool "auth": login, register,
 *  refresh, forgot, verify, verificacion de codigos de invitacion.
 *
 *  Estrategia nueva (orden de preferencia, siempre tomando el header
 *  que NO puede ser suplantado por el cliente):
 *   1. `CF-Connecting-IP`: Cloudflare lo pone al recibir el request desde
 *      el cliente y REEMPLAZA cualquier valor previo. Ignora lo que el
 *      cliente manda con ese header. Es el header oficial y confiable en
 *      nuestro setup (Cloudflare -> Traefik -> backend).
 *   2. `X-Real-IP`: convencion nginx/traefik para "IP real del cliente".
 *      Nuestro Traefik la agrega y el cliente no puede suplantarla porque
 *      Traefik la sobrescribe.
 *   3. Ultimo valor de `X-Forwarded-For`: el proxy inmediato al backend
 *      (Traefik) siempre APPENDEA la IP verdadera del cliente al final,
 *      asi que el ultimo es el que Traefik agrego (los previos pueden
 *      venir del cliente). Fallback si por config no llega CF ni X-Real.
 *   4. `origin.remoteHost`: dev/local sin proxy. */
private fun ApplicationRequest.clientIpKey(): String {
    header("CF-Connecting-IP")?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    header("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }?.let { return it }
    header("X-Forwarded-For")
        ?.split(",")
        ?.map { it.trim() }
        ?.lastOrNull { it.isNotBlank() }
        ?.let { return it }
    return origin.remoteHost
}
