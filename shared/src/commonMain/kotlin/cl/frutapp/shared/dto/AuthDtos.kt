package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Cuerpo de registro de un cliente. */
@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val password: String,
    /** Versión de T&C/Política aceptada al registrarse (para registrar el consentimiento). */
    val consentVersion: String? = null
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

/** Verificar el correo con el código enviado al registrarse. */
@Serializable
data class VerifyEmailRequest(
    val email: String,
    val code: String
)

/** Reenviar el código de verificación de correo. */
@Serializable
data class ResendVerificationRequest(
    val email: String
)

/** Solicitar código de recuperación de contraseña. */
@Serializable
data class ForgotPasswordRequest(
    val email: String
)

/** Restablecer la contraseña con el código recibido por correo. */
@Serializable
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

/** Respuesta simple de operaciones sin payload (ej. forgot-password). */
@Serializable
data class MessageResponse(
    val message: String
)

/** Vista pública de un usuario (nunca expone el hash).
 *  [role] sigue siendo el rol "principal" de la columna app_user.role (CUSTOMER / etc.) por
 *  compatibilidad. [roles] es la lista completa de roles RBAC asignados (cliente, picker,
 *  repartidor, admin...) — esta es la fuente de verdad para decidir routing por perfil en
 *  el cliente. Default vacio para que clientes viejos sigan deserializando. */
@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    val roles: List<String> = emptyList()
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
