package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.config.JwtConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * Emite y verifica tokens.
 * - Access token: JWT firmado (HMAC256), corto, stateless, validado por el plugin.
 * - Refresh token: string opaco aleatorio; en DB se guarda solo su hash SHA-256
 *   (determinista para poder buscarlo). Permite rotación y revocación (logout).
 */
class TokenService(private val config: JwtConfig) {

    private val algorithm = Algorithm.HMAC256(config.secret)

    fun buildVerifier(): JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .build()

    val accessTtlSeconds: Long get() = config.accessTtlMinutes * 60

    fun issueAccessToken(userId: UUID, role: String): String {
        val now = java.time.Instant.now()
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withClaim("role", role)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(config.accessTtlMinutes.minutes.inWholeSeconds)))
            .sign(algorithm)
    }

    /** Genera un refresh token opaco (256 bits, base64url). */
    fun generateRefreshToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /** Hash determinista (SHA-256 hex) de un refresh token (256 bits aleatorios). */
    fun hashRefreshToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hash con pepper (HMAC-SHA256 con el secreto del servidor) para códigos CORTOS
     * (6 dígitos). Sin el pepper, un SHA-256 simple de 10^6 combinaciones es
     * pre-computable si se filtra la tabla; con HMAC el atacante necesita además el secreto.
     */
    fun hashCode(code: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(config.secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal(code.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    /** Código numérico aleatorio (para recuperación de contraseña / verificación). */
    fun generateNumericCode(length: Int = 6): String {
        val rnd = SecureRandom()
        return buildString { repeat(length) { append(rnd.nextInt(10)) } }
    }

    fun refreshExpiry(): Instant = Clock.System.now() + config.refreshTtlDays.days
}
