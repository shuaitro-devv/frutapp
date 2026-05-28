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

    /** Hash determinista (SHA-256 hex) del refresh token, lo único que se persiste. */
    fun hashRefreshToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun refreshExpiry(): Instant = Clock.System.now() + config.refreshTtlDays.days
}
