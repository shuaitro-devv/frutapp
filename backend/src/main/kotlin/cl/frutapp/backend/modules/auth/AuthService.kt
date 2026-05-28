package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.error.ConflictException
import cl.frutapp.backend.error.UnauthorizedException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.shared.dto.AuthResponse
import cl.frutapp.shared.dto.ForgotPasswordRequest
import cl.frutapp.shared.dto.LoginRequest
import cl.frutapp.shared.dto.LogoutRequest
import cl.frutapp.shared.dto.RefreshRequest
import cl.frutapp.shared.dto.RegisterRequest
import cl.frutapp.shared.dto.ResetPasswordRequest
import cl.frutapp.shared.dto.UserDto
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordResetTokens: PasswordResetTokenRepository,
    private val tokens: TokenService,
    private val emailSender: EmailSender
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /** Envía un correo sin tumbar el flujo principal si el proveedor falla. */
    private suspend fun sendSafely(email: Email) {
        runCatching { emailSender.send(email) }
            .onFailure { logger.error("No se pudo enviar correo a {}", email.to, it) }
    }

    suspend fun register(req: RegisterRequest): AuthResponse {
        if (req.name.isBlank()) throw ValidationException("El nombre es obligatorio.")
        val email = normalizeEmail(req.email)
        validatePassword(req.password)
        if (users.findByEmail(email) != null) {
            throw ConflictException("Ya existe una cuenta con ese correo.")
        }
        val user = users.create(
            name = req.name.trim(),
            email = email,
            phone = req.phone?.trim()?.ifBlank { null },
            passwordHash = PasswordHasher.hash(req.password),
            role = "CUSTOMER"
        )
        sendSafely(EmailTemplates.welcome(to = user.email, name = user.name.substringBefore(' ')))
        return issueFor(user)
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val email = normalizeEmail(req.email)
        val user = users.findByEmail(email) ?: throw UnauthorizedException()
        if (!PasswordHasher.verify(req.password, user.passwordHash)) throw UnauthorizedException()
        return issueFor(user)
    }

    /** Rota el refresh token: revoca el presentado y emite uno nuevo. */
    suspend fun refresh(req: RefreshRequest): AuthResponse {
        val hash = tokens.hashRefreshToken(req.refreshToken)
        val row = refreshTokens.findValid(hash)
            ?: throw UnauthorizedException("Refresh token inválido o expirado.")
        refreshTokens.revoke(row.id)
        val user = users.findById(row.userId) ?: throw UnauthorizedException()
        return issueFor(user)
    }

    suspend fun logout(req: LogoutRequest) {
        refreshTokens.revokeByHash(tokens.hashRefreshToken(req.refreshToken))
    }

    /** Datos del usuario autenticado (a partir del `sub` del JWT). */
    suspend fun me(userId: String): UserDto {
        val uuid = runCatching { UUID.fromString(userId) }.getOrNull() ?: throw UnauthorizedException()
        return (users.findById(uuid) ?: throw UnauthorizedException()).toDto()
    }

    /** Genera y "envía" un código de recuperación. No revela si el correo existe. */
    suspend fun forgotPassword(req: ForgotPasswordRequest) {
        val email = req.email.trim().lowercase()
        val user = users.findByEmail(email) ?: return
        passwordResetTokens.invalidateAllForUser(user.id)
        val code = tokens.generateNumericCode()
        passwordResetTokens.create(user.id, tokens.hashRefreshToken(code), Clock.System.now() + 30.minutes)
        sendSafely(EmailTemplates.passwordReset(to = user.email, code = code))
    }

    /** Cambia la contraseña validando el código; invalida el código y cierra sesiones. */
    suspend fun resetPassword(req: ResetPasswordRequest) {
        val email = normalizeEmail(req.email)
        validatePassword(req.newPassword)
        val user = users.findByEmail(email) ?: throw UnauthorizedException("Código inválido o expirado.")
        val tokenId = passwordResetTokens.findValid(user.id, tokens.hashRefreshToken(req.code))
            ?: throw UnauthorizedException("Código inválido o expirado.")
        users.updatePassword(user.id, PasswordHasher.hash(req.newPassword))
        passwordResetTokens.markUsed(tokenId)
        refreshTokens.revokeAllForUser(user.id)
        sendSafely(EmailTemplates.passwordChanged(to = user.email, name = user.name.substringBefore(' ')))
    }

    private suspend fun issueFor(user: UserRow): AuthResponse {
        val access = tokens.issueAccessToken(user.id, user.role)
        val refresh = tokens.generateRefreshToken()
        refreshTokens.create(user.id, tokens.hashRefreshToken(refresh), tokens.refreshExpiry())
        return AuthResponse(
            user = user.toDto(),
            accessToken = access,
            refreshToken = refresh,
            accessExpiresInSeconds = tokens.accessTtlSeconds
        )
    }

    private fun normalizeEmail(email: String): String {
        val normalized = email.trim().lowercase()
        if (!EMAIL_REGEX.matches(normalized)) throw ValidationException("El correo no es válido.")
        return normalized
    }

    private fun validatePassword(password: String) {
        if (password.length < 6 || password.none { it.isLetter() } || password.none { it.isDigit() }) {
            throw ValidationException("La contraseña debe tener mínimo 6 caracteres e incluir letras y números.")
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
