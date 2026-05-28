package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.error.ConflictException
import cl.frutapp.backend.error.UnauthorizedException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.shared.dto.AuthResponse
import cl.frutapp.shared.dto.LoginRequest
import cl.frutapp.shared.dto.LogoutRequest
import cl.frutapp.shared.dto.RefreshRequest
import cl.frutapp.shared.dto.RegisterRequest
import cl.frutapp.shared.dto.UserDto
import java.util.UUID

class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val tokens: TokenService
) {

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
