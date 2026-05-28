package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Cuerpo de registro de un cliente. */
@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String
)

/** Cuerpo de login. */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

/** Cuerpo para renovar el par de tokens. */
@Serializable
data class RefreshRequest(
    val refreshToken: String
)

/** Cuerpo para cerrar sesión (revoca el refresh token). */
@Serializable
data class LogoutRequest(
    val refreshToken: String
)

/** Vista pública de un usuario (nunca expone el hash). */
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String
)

/** Respuesta de auth: usuario + par de tokens. */
@Serializable
data class AuthResponse(
    val user: UserDto,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val accessExpiresInSeconds: Long
)
