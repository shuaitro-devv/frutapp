package cl.frutapp.backend.modules.auth

import org.slf4j.LoggerFactory

data class Email(
    val to: String,
    val subject: String,
    val html: String,
    val text: String
)

interface EmailSender {
    suspend fun send(email: Email)
}

/**
 * Implementación de demo: NO envía, solo loguea (incluye el cuerpo de texto con el
 * código para poder probar el flujo sin proveedor real). Se reemplaza por
 * ResendEmailSender cuando exista la API key.
 */
class LogEmailSender : EmailSender {
    private val logger = LoggerFactory.getLogger("EmailSender")

    override suspend fun send(email: Email) {
        logger.info("[EMAIL SIMULADO] to={} | subject={}\n{}", email.to, email.subject, email.text)
    }
}
