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
    val consentVersion: String? = null,
    /** V42: código de invitación opcional. Si es válido, el nuevo usuario
     *  queda linkeado al que lo refirió; al completar su primer pedido
     *  ENTREGADO ambos reciben FrutCoins. Si el código no existe se ignora
     *  silenciosamente — no bloqueamos el registro por un typo. */
    val codigoInvitacion: String? = null,
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
    val roles: List<String> = emptyList(),
    /** URL presignada de la foto de perfil. Null si el user no subió foto. Tiene
     *  TTL de 1h — el cliente refresca llamando a `/v1/auth/me`. Default null para
     *  retro-compatibilidad con clientes viejos. */
    val avatarUrl: String? = null,
    /** V42: código de invitación del propio usuario (8 chars). Lo comparte
     *  desde FrutCoinsScreen para invitar amigos y ganar FrutCoins cuando
     *  ellos completen su primer pedido. Puede ser null en usuarios legacy
     *  que aún no lo tienen generado (se genera lazy la primera vez que
     *  el user lo mire en su perfil). */
    val codigoInvitacion: String? = null,
)

/** Respuesta del endpoint publico `GET /v1/referrals/verify/{codigo}`. Se usa
 *  desde la landing (page `/invita/[codigo]`) para renderizar OG tags
 *  personalizados y validar el codigo antes de mostrar el CTA. Nunca expone
 *  email, id ni apellido — solo el primer nombre del referidor.
 *
 *  bonoReferido / bonoReferidor: valores actuales del programa (fuente:
 *  ReferralConfig del shared). Se envian aca para que la landing NO tenga
 *  que hardcodearlos y quede siempre sincronizada con el backend. */
@Serializable
data class ReferralVerifyResponse(
    val codigo: String,
    val referrerFirstName: String,
    val bonoReferido: Int,
    val bonoReferidor: Int,
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
