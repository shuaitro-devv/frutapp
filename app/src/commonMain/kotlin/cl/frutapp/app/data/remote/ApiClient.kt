package cl.frutapp.app.data.remote

import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.shared.dto.AuthResponse
import cl.frutapp.shared.dto.RefreshRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Resultado del intento de refrescar el access token. Distinguir entre los tres
 * estados es crítico: tratar una falla transient (timeout, red caida, 5xx) como
 * "sesion expirada" botaba al usuario al login cada vez que el backend tenia un
 * cold start o el celu cambiaba de antena, aunque el refresh token siguiera valido.
 */
private sealed class RefreshOutcome {
    /** El access token nuevo está en TokenStore y se devuelve para reintentos opcionales. */
    data class Success(val accessToken: String) : RefreshOutcome()
    /** El backend rechazó explicitamente el refresh (401 → token revocado/vencido). Limpiar sesion. */
    object SessionExpired : RefreshOutcome()
    /** Falla no determinista (timeout, IO, 5xx). El refresh sigue siendo valido. NO limpiar. */
    object Transient : RefreshOutcome()
}

/** Cliente HTTP único de la app (motor Android via classpath) + JSON + auth. */
object ApiClient {
    val baseUrl: String = apiBaseUrl()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Cliente "desnudo" SOLO para refrescar el token (sin el interceptor → sin recursión).
    // Retry de conexion para sobrevivir cold starts del backend en Contabo: si el SYN
    // del primer attempt se pierde porque el container se acaba de levantar, el 2do
    // reintento (2s despues) suele entrar. Sin esto, el primer hipo de red post-idle
    // tiraba el refresh a Transient y la proxima request del usuario veia el access
    // viejo otra vez. Reintentamos SOLO ConnectTimeout (TCP nunca se establecio →
    // backend no recibio nada → seguro reintentar incluso para POST).
    private val refreshClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
        install(HttpRequestRetry) {
            retryOnExceptionIf(maxRetries = 2) { _, cause -> cause is ConnectTimeoutException }
            exponentialDelay(base = 2.0, maxDelayMs = 4_000)
        }
    }
    private val refreshMutex = Mutex()

    /**
     * Intenta refrescar el access token con el refresh token. Distinguir entre los 3
     * outcomes es lo que evita botar al usuario al login por fallas transient de red
     * (timeout, IO, 5xx, cold start del backend) — el refresh token sigue siendo
     * valido y la proxima request lo va a reintentar.
     *
     * Usa try/catch en vez de runCatching para PROPAGAR CancellationException — sin esto,
     * un screen cancelado durante el refresh swallea la cancelación y el HTTP post puede
     * completar tras el logout, re-escribiendo tokens que el usuario acababa de borrar.
     */
    private suspend fun tryRefresh(): RefreshOutcome = refreshMutex.withLock {
        val rt = TokenStore.refreshToken ?: return@withLock RefreshOutcome.SessionExpired
        try {
            val resp = refreshClient.post("$baseUrl/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(rt))
            }
            when {
                resp.status.isSuccess() -> {
                    val auth: AuthResponse = resp.body()
                    TokenStore.updateTokens(auth.accessToken, auth.refreshToken)
                    RefreshOutcome.Success(auth.accessToken)
                }
                resp.status == HttpStatusCode.Unauthorized -> {
                    // 401 explicito: el refresh token fue revocado o vencio realmente.
                    // Logueamos el motivo para tener traza y devolvemos SessionExpired
                    // — el validator se encarga del clear + markSessionExpired.
                    val body = runCatching { resp.body<String>() }.getOrNull().orEmpty().take(200)
                    ErrorReporter.report(
                        screen = "ApiClient",
                        action = "refresh_token_unauthorized",
                        error = RuntimeException("Refresh 401: $body")
                    )
                    RefreshOutcome.SessionExpired
                }
                else -> {
                    // 5xx, 503, 502, etc.: backend con cold start o problema temporal.
                    // El refresh token sigue siendo valido server-side; la proxima
                    // request lo va a reintentar. NO botar al usuario.
                    val body = runCatching { resp.body<String>() }.getOrNull().orEmpty().take(200)
                    ErrorReporter.report(
                        screen = "ApiClient",
                        action = "refresh_token_transient_${resp.status.value}",
                        error = RuntimeException("Refresh ${resp.status.value}: $body")
                    )
                    RefreshOutcome.Transient
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // estructura de coroutines: re-tirar siempre.
        } catch (e: Throwable) {
            // Timeout, IO, red caida, cambio de antena en el celu. Refresh sigue valido
            // server-side; no botamos al usuario, dejamos que la proxima request reintente.
            ErrorReporter.report(screen = "ApiClient", action = "refresh_token_exception", error = e)
            RefreshOutcome.Transient
        }
    }

    val client: HttpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
        // Sin esto, Ktor intenta deserializar el body de error (ej. 401 con
        // {"error":"Invalid credentials"}) como el DTO esperado y falla con
        // MissingFieldException — que no es legible ni para la heurística de
        // mensajeAmigable. Con expectSuccess, status no-2xx lanza ClientRequestException
        // cuyo .message contiene "401 Unauthorized" / "422 Unprocessable Entity" / etc.
        expectSuccess = true

        // Manejo de 401 en endpoints protegidos. Llamamos a tryRefresh y SOLO
        // limpiamos la sesion cuando el outcome es SessionExpired (refresh respondio
        // 401 → token revocado/vencido). Si fue Transient (timeout, IO, 5xx,
        // backend dormido), NO limpiamos: el refresh sigue valido server-side y la
        // proxima request lo va a reintentar. Antes mezclabamos los dos casos y
        // boteabamos al usuario a login por cualquier hipo de red.
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                if (exception !is ClientRequestException) return@handleResponseExceptionWithRequest
                if (exception.response.status != HttpStatusCode.Unauthorized) return@handleResponseExceptionWithRequest
                val protectedEndpoint = !request.url.toString().contains("/auth/")
                if (!protectedEndpoint) return@handleResponseExceptionWithRequest
                if (TokenStore.refreshToken == null) return@handleResponseExceptionWithRequest

                when (tryRefresh()) {
                    is RefreshOutcome.Success -> { /* nuevo access guardado; proximas requests van con el nuevo */ }
                    RefreshOutcome.SessionExpired -> {
                        if (TokenStore.accessToken != null) {
                            TokenStore.clear()
                            TokenStore.markSessionExpired()
                        }
                    }
                    RefreshOutcome.Transient -> { /* dejamos la sesion, proxima request retira */ }
                }
            }
        }
        // Retry para errores transient — backend con pool de Postgres dormido o Traefik
        // con cold start a veces falla el primer request y el segundo anda. Reintentamos
        // solo errores de red, timeouts y 5xx; NUNCA 4xx (un 401 de login NO se reintenta,
        // el usuario tipeó mal la clave). 2 reintentos con backoff 1s/2s.
        install(HttpRequestRetry) {
            // Solo reintentamos fallos de CONEXIÓN (TCP nunca se estableció → el server
            // no recibió la request, retry es seguro incluso para POSTs no idempotentes).
            // NO reintentamos:
            //   - HttpRequestTimeoutException / SocketTimeoutException: el server pudo
            //     haber procesado la mutación (crear orden, invalidar código de email).
            //   - ClientRequestException (4xx): error del cliente, retry no ayuda.
            //   - ServerResponseException (5xx): mismo motivo de no-retry que timeouts.
            //   - CancellationException: respetar structured concurrency.
            // Trade-off: reintentamos MENOS de lo deseable (algunos 5xx transient serían
            // safe-to-retry), pero ganamos seguridad contra órdenes/emails duplicados.
            // Cubrir más casos requiere idempotency keys en el backend.
            retryOnExceptionIf(maxRetries = 2) { _, cause ->
                cause is ConnectTimeoutException
            }
            exponentialDelay(base = 2.0, maxDelayMs = 4_000)
        }
        // Adjunta el access token (JWT) en cada request si hay sesión (se lee en tiempo de request).
        defaultRequest {
            TokenStore.accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
