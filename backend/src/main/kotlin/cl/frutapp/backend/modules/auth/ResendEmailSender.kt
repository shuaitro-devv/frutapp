package cl.frutapp.backend.modules.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Envía correos vía la API de Resend (https://resend.com). Usa el HttpClient del JDK
 * para no sumar otro cliente HTTP al backend. La API key se inyecta por env
 * (RESEND_API_KEY); nunca va en código.
 *
 * Un fallo de Resend NO revienta el flujo (p.ej. forgot-password): se loguea el error.
 * El token de recuperación ya quedó creado; lo único que falla es la entrega del correo.
 */
class ResendEmailSender(
    private val apiKey: String,
    private val from: String
) : EmailSender {
    private val logger = LoggerFactory.getLogger("ResendEmailSender")
    private val http = HttpClient.newHttpClient()
    private val json = Json { encodeDefaults = true }

    override suspend fun send(email: Email) {
        val payload = json.encodeToString(
            ResendPayload(
                from = from,
                to = listOf(email.to),
                subject = email.subject,
                html = email.html,
                text = email.text
            )
        )
        val request = HttpRequest.newBuilder(URI.create(ENDPOINT))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload, Charsets.UTF_8))
            .build()

        // Llamada de red bloqueante: la sacamos del hilo de la request.
        val response = withContext(Dispatchers.IO) {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        }
        if (response.statusCode() in 200..299) {
            logger.info("Correo enviado a {} vía Resend", email.to)
        } else {
            logger.error("Resend rechazó el envío a {}: {} {}", email.to, response.statusCode(), response.body())
        }
    }

    @Serializable
    private data class ResendPayload(
        val from: String,
        val to: List<String>,
        val subject: String,
        val html: String,
        val text: String
    )

    private companion object {
        const val ENDPOINT = "https://api.resend.com/emails"
    }
}
