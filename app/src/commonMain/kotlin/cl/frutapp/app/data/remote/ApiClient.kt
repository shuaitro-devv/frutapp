package cl.frutapp.app.data.remote

import cl.frutapp.app.data.TokenStore
import cl.frutapp.shared.dto.AuthResponse
import cl.frutapp.shared.dto.RefreshRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
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

    /** Intenta refrescar el access token con el refresh token. Devuelve el nuevo access o null. */
    private suspend fun tryRefresh(): String? = refreshMutex.withLock {
        val rt = TokenStore.refreshToken ?: return@withLock null
        runCatching {
            val resp = refreshClient.post("$baseUrl/v1/auth/refresh") {
                contentType(ContentType.Application.Json)
                setBody(RefreshRequest(rt))
            }
            if (resp.status.isSuccess()) {
                val auth: AuthResponse = resp.body()
                TokenStore.updateTokens(auth.accessToken, auth.refreshToken)
                auth.accessToken
            } else {
                TokenStore.clear()
                null
            }
        }.getOrNull()
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
        // Adjunta el access token (JWT) en cada request si hay sesión (se lee en tiempo de request).
        defaultRequest {
            TokenStore.accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }.also { c ->
        // Auto-refresh: si un endpoint protegido responde 401, refresca el token y reintenta una vez.
        // Con expectSuccess=true, el 401 llega como ClientRequestException (no como response):
        // catcheamos, refrescamos y reintentamos; si no es 401 o falla refresh, re-lanzamos.
        c.plugin(HttpSend).intercept { request ->
            try {
                execute(request)
            } catch (e: ClientRequestException) {
                val protectedEndpoint = !request.url.buildString().contains("/auth/")
                if (e.response.status == HttpStatusCode.Unauthorized &&
                    TokenStore.refreshToken != null &&
                    protectedEndpoint
                ) {
                    val newAccess = tryRefresh()
                    if (newAccess != null) {
                        request.headers.remove(HttpHeaders.Authorization)
                        request.headers.append(HttpHeaders.Authorization, "Bearer $newAccess")
                        execute(request)
                    } else throw e
                } else throw e
            }
        }
    }
}
