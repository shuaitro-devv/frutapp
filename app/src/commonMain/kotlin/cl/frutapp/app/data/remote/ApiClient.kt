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

/** Cliente HTTP único de la app (motor Android via classpath) + JSON + auth. */
object ApiClient {
    val baseUrl: String = apiBaseUrl()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // Cliente "desnudo" SOLO para refrescar el token (sin el interceptor → sin recursión).
    private val refreshClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
    }
    private val refreshMutex = Mutex()

    /**
     * Intenta refrescar el access token con el refresh token. Devuelve el nuevo access o null.
     * Usa try/catch en vez de runCatching para PROPAGAR CancellationException — sin esto,
     * un screen cancelado durante el refresh swallea la cancelación y el HTTP post puede
     * completar tras el logout, re-escribiendo tokens que el usuario acababa de borrar.
     */
    private suspend fun tryRefresh(): String? = refreshMutex.withLock {
        val rt = TokenStore.refreshToken ?: return@withLock null
        try {
            val resp = refreshClient.post("$baseUrl/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(rt))
            }
            if (resp.status.isSuccess()) {
                val auth: AuthResponse = resp.body()
                TokenStore.updateTokens(auth.accessToken, auth.refreshToken)
                auth.accessToken
            } else {
                // Loggear el motivo del refresh fallido — antes solo cleareabamos el
                // TokenStore en silencio y el LaunchedEffect global pateaba al login,
                // dejando al usuario sin contexto y a nosotros sin pistas de POR QUE
                // (refresh token vencido, invalidado server-side, backend caido, etc.).
                val body = runCatching { resp.body<String>() }.getOrNull().orEmpty().take(200)
                ErrorReporter.report(
                    screen = "ApiClient",
                    action = "refresh_token_http_${resp.status.value}",
                    error = RuntimeException("Refresh respondio ${resp.status.value} ${resp.status.description}: $body")
                )
                TokenStore.clear()
                // Senal explicita para que App.kt patee a Login. Antes confiabamos en
                // que el LaunchedEffect global detectara accessToken=null, pero ese
                // patron dependia de un flag hadSession con timing fragil. Esto es
                // determinista: ApiClient sabe que la expiracion es real (refresh
                // respondio 401) y lo comunica directo.
                TokenStore.markSessionExpired()
                null
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // estructura de coroutines: re-tirar siempre.
        } catch (e: Throwable) {
            // Excepcion no controlada durante el refresh (timeout, IO, etc.). NO limpiamos
            // tokens: si fue red caida momentanea, el proximo intento puede funcionar.
            ErrorReporter.report(screen = "ApiClient", action = "refresh_token_exception", error = e)
            null
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

        // Limpieza de sesion cuando llega un 401 a un endpoint protegido. Esto se
        // ejecuta DENTRO del response pipeline, antes de que la excepcion del
        // ResponseValidator (con expectSuccess=true) llegue al caller — el lugar
        // correcto en Ktor 2.x para manejar 401, porque HttpSend.intercept NO
        // captura excepciones lanzadas por el ResponseValidator.
        //
        // Si el access token vencio y NO podemos refrescarlo (refresh tambien 401
        // o refreshToken null), limpiamos TokenStore para que el LaunchedEffect de
        // App.kt detecte accessToken=null y patee al Login. El refresh + reintento
        // automatico del mismo request es complejo (requiere Auth plugin); por
        // ahora solo manejamos la limpieza de sesion zombie. Cuando hagamos el
        // refactor a Auth plugin, agregamos el auto-retry transparente.
        HttpResponseValidator {
            handleResponseExceptionWithRequest { exception, request ->
                if (exception !is ClientRequestException) return@handleResponseExceptionWithRequest
                if (exception.response.status != HttpStatusCode.Unauthorized) return@handleResponseExceptionWithRequest
                val protectedEndpoint = !request.url.toString().contains("/auth/")
                if (!protectedEndpoint) return@handleResponseExceptionWithRequest

                // Intentamos refresh una vez. Si funciono, el polling siguiente va
                // con el token nuevo. Si no funciono, limpiamos TokenStore para
                // disparar el guard global.
                val newAccess = if (TokenStore.refreshToken != null) tryRefresh() else null
                if (newAccess == null && TokenStore.accessToken != null) {
                    TokenStore.clear()
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
