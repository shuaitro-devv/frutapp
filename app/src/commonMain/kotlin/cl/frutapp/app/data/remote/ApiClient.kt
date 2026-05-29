package cl.frutapp.app.data.remote

import cl.frutapp.app.data.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Cliente HTTP único de la app (motor Android via classpath) + JSON. */
object ApiClient {
    val baseUrl: String = apiBaseUrl()

    val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
        // Adjunta el access token (JWT) en cada request si hay sesión. Se lee en tiempo
        // de request, así toma el token vigente. Los endpoints públicos lo ignoran.
        defaultRequest {
            TokenStore.accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
