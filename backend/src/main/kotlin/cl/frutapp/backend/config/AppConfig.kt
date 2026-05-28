package cl.frutapp.backend.config

import io.ktor.server.config.ApplicationConfig

/**
 * Config de conexión a Postgres. Se arma desde host/port/name (la convención que
 * inyecta el workflow de deploy: DB_HOST/DB_PORT/DB_NAME), no una URL completa.
 */
data class DbConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int
) {
    val jdbcUrl: String get() = "jdbc:postgresql://$host:$port/$name"

    companion object {
        fun from(config: ApplicationConfig) = DbConfig(
            host = config.property("db.host").getString(),
            port = config.property("db.port").getString().toInt(),
            name = config.property("db.name").getString(),
            user = config.property("db.user").getString(),
            password = config.property("db.password").getString(),
            maxPoolSize = config.property("db.maxPoolSize").getString().toInt()
        )
    }
}

/**
 * Config de correo (Resend). La API key se inyecta por env (RESEND_API_KEY); nunca va
 * en código. Si la key viene vacía, el backend usa LogEmailSender (modo demo, sin envío).
 *
 * `from` por defecto usa el remitente de prueba de Resend (onboarding@resend.dev), que
 * SOLO entrega al correo dueño de la cuenta. Al verificar un dominio se setea MAIL_FROM
 * (p.ej. "FrutApp <no-reply@send.grandline.cl>") y entrega a cualquier destinatario.
 */
data class MailConfig(
    val resendApiKey: String,
    val from: String
) {
    val enabled: Boolean get() = resendApiKey.isNotBlank()

    companion object {
        private const val DEFAULT_FROM = "FrutApp <onboarding@resend.dev>"

        fun from(config: ApplicationConfig) = MailConfig(
            resendApiKey = config.propertyOrNull("mail.resendApiKey")?.getString().orEmpty(),
            from = config.propertyOrNull("mail.from")?.getString()?.ifBlank { DEFAULT_FROM } ?: DEFAULT_FROM
        )
    }
}

/** Config de JWT (firma, claims y TTLs). El secret se inyecta por env en producción. */
data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val accessTtlMinutes: Long,
    val refreshTtlDays: Long
) {
    companion object {
        fun from(config: ApplicationConfig): JwtConfig {
            val secret = config.property("jwt.secret").getString()
            // Fail-fast: si el secret está vacío (env var seteada pero vacía), HMAC256 tira
            // "Empty key" recién al firmar el primer token. Mejor reventar al arrancar con
            // un mensaje claro que devolver 500 por request.
            require(secret.isNotBlank()) {
                "JWT_SECRET vacío: configúralo como env var / GitHub secret antes de desplegar."
            }
            return JwtConfig(
                secret = secret,
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                realm = config.property("jwt.realm").getString(),
                accessTtlMinutes = config.property("jwt.accessTtlMinutes").getString().toLong(),
                refreshTtlDays = config.property("jwt.refreshTtlDays").getString().toLong()
            )
        }
    }
}
