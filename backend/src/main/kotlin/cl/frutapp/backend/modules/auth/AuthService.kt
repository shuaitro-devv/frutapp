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
import cl.frutapp.shared.dto.ResendVerificationRequest
import cl.frutapp.shared.dto.ResetPasswordRequest
import cl.frutapp.shared.dto.UserDto
import cl.frutapp.shared.dto.VerifyEmailRequest
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val passwordResetTokens: PasswordResetTokenRepository,
    private val emailVerificationTokens: EmailVerificationTokenRepository,
    private val tokens: TokenService,
    private val emailSender: EmailSender,
    private val rbac: cl.frutapp.backend.modules.rbac.RbacRepository,
    /** Resuelve la URL presignada del avatar del user para devolverla en /me.
     *  Null cuando MinIO no esta configurado — el backend sigue funcionando. */
    private val avatarUrlResolver: (suspend (UUID) -> String?)? = null
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    /** Hash bcrypt fijo para igualar el tiempo de login cuando el usuario NO existe
     *  (evita el oráculo de enumeración por timing). Se calcula una sola vez. */
    private val dummyHash: String by lazy { PasswordHasher.hash("frutapp-timing-dummy") }

    /** Envía un correo sin tumbar el flujo principal si el proveedor falla.
     *  CancellationException se RE-LANZA: si el caller fue cancelado (cliente desconecto,
     *  request timeout), tragarla con runCatching rompe structured concurrency — el
     *  coroutine seguiria con lineas posteriores como si nada y loggeariamos un falso
     *  'no se pudo enviar correo' que en realidad fue un cancel. */
    private suspend fun sendSafely(email: Email) {
        runCatching { emailSender.send(email) }
            .onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.error("No se pudo enviar correo a {}", email.to, e)
            }
    }

    /** Crea la cuenta SIN activar y envía un código de verificación al correo. No
     *  entrega tokens: el usuario inicia sesión recién al verificar (ver [verifyEmail]).
     *
     *  Cuenta no verificada en el medio: si ya existe un usuario CUSTOMER con ese correo
     *  pero NO verificó, NO tiramos 409 — sobrescribimos password/name/phone/consent y
     *  reenviamos código. Caso real (test del hermano de Sebastián): usuario se registra,
     *  no verifica, intenta de nuevo y antes recibía 'Ya está registrado' y quedaba
     *  bloqueado sin forma de recuperar.
     *
     *  Cuentas con rol staff (PICKER, DELIVERY, ADMIN, etc.) NUNCA se sobrescriben aunque
     *  esten no verificadas — protege contra account-takeover si AdminUserService.createUser
     *  fallo mid-flight dejando un row de staff sin verificar. Para esos casos, el path
     *  legitimo es completar la invitacion via resetPassword (que usa el codigo recibido). */
    suspend fun register(req: RegisterRequest) {
        if (req.name.isBlank()) throw ValidationException("El nombre es obligatorio.")
        val email = normalizeEmail(req.email)
        validatePassword(req.password)
        val nombreLimpio = req.name.trim()
        val telefonoLimpio = req.phone?.trim()?.ifBlank { null }
        val existente = users.findByEmail(email)
        if (existente != null) {
            // Solo permitimos overwrite si el row es un CUSTOMER no verificado. Cualquier
            // otra cuenta (verificada o con rol staff) responde 409 para no exponer un
            // vector de toma de cuenta.
            if (existente.emailVerified || existente.role != "CUSTOMER") {
                throw ConflictException("Ya existe una cuenta con ese correo.")
            }
        }
        val user = if (existente != null) {
            // CUSTOMER no verificado → sobrescribir credenciales+perfil y reenviar codigo.
            users.updatePassword(existente.id, PasswordHasher.hash(req.password))
            users.updateProfileFields(
                userId = existente.id,
                name = nombreLimpio,
                phone = telefonoLimpio,
                consentVersion = req.consentVersion
            )
            // assignRole idempotente por si el create previo logro insertar el row pero
            // assignRole fallo (sin transaccion). Sin esto, el reintento dejaria al usuario
            // sin rol 'cliente' aunque su cuenta exista.
            rbac.assignRole(existente.id, "cliente")
            existente.copy(name = nombreLimpio, phone = telefonoLimpio)
        } else {
            val nuevo = users.create(
                name = nombreLimpio,
                email = email,
                phone = telefonoLimpio,
                passwordHash = PasswordHasher.hash(req.password),
                role = "CUSTOMER",
                consentVersion = req.consentVersion
            )
            rbac.assignRole(nuevo.id, "cliente")
            nuevo
        }
        sendVerificationCode(user)
    }

    /** Verifica el correo con el código; al lograrlo activa la cuenta, da la bienvenida
     *  y recién entonces emite los tokens (login efectivo). */
    suspend fun verifyEmail(req: VerifyEmailRequest): AuthResponse {
        val email = normalizeEmail(req.email)
        val user = users.findByEmail(email) ?: throw UnauthorizedException("Código inválido o expirado.")
        // Ya verificado: NO emitir tokens sin código (evita tomar la cuenta sabiendo el correo).
        if (user.emailVerified) throw UnauthorizedException("Código inválido o expirado.")
        val tokenId = emailVerificationTokens.findValid(user.id, tokens.hashCode(req.code))
            ?: throw UnauthorizedException("Código inválido o expirado.")
        // Consumo atómico: si otra request ya lo usó, falla (no doble uso).
        if (!emailVerificationTokens.consume(tokenId)) throw UnauthorizedException("Código inválido o expirado.")
        users.markEmailVerified(user.id)
        sendSafely(EmailTemplates.welcome(to = user.email, name = user.name.substringBefore(' ')))
        return issueFor(user)
    }

    /** Reenvía el código de verificación. No revela si el correo existe. */
    suspend fun resendVerification(req: ResendVerificationRequest) {
        val email = req.email.trim().lowercase()
        val user = users.findByEmail(email) ?: return
        if (user.emailVerified) return
        sendVerificationCode(user)
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val email = normalizeEmail(req.email)
        val user = users.findByEmail(email)
        if (user == null) {
            // Gasta el mismo tiempo de bcrypt aunque el usuario no exista (anti-enumeración).
            PasswordHasher.verify(req.password, dummyHash)
            throw UnauthorizedException()
        }
        if (!PasswordHasher.verify(req.password, user.passwordHash)) throw UnauthorizedException()
        if (!user.emailVerified) throw UnauthorizedException("Verifica tu correo para iniciar sesión.")
        return issueFor(user)
    }

    /** Genera, guarda (hash) y envía un código de verificación de correo (vence 30 min). */
    private suspend fun sendVerificationCode(user: UserRow) {
        emailVerificationTokens.invalidateAllForUser(user.id)
        val code = tokens.generateNumericCode()
        emailVerificationTokens.create(user.id, tokens.hashCode(code), Clock.System.now() + 30.minutes)
        sendSafely(EmailTemplates.emailVerification(to = user.email, name = user.name.substringBefore(' '), code = code))
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
        val row = users.findById(uuid) ?: throw UnauthorizedException()
        val roles = rbac.rolesOf(row.id).ifEmpty { listOf("cliente") }
        val avatarUrl = avatarUrlResolver?.invoke(uuid)
        return row.toDto().copy(roles = roles, avatarUrl = avatarUrl)
    }

    /** Genera y "envía" un código de recuperación. No revela si el correo existe. */
    suspend fun forgotPassword(req: ForgotPasswordRequest) {
        val email = req.email.trim().lowercase()
        val user = users.findByEmail(email) ?: return
        passwordResetTokens.invalidateAllForUser(user.id)
        val code = tokens.generateNumericCode()
        passwordResetTokens.create(user.id, tokens.hashCode(code), Clock.System.now() + 30.minutes)
        sendSafely(EmailTemplates.passwordReset(to = user.email, code = code))
    }

    /** Cambia la contraseña validando el código; invalida el código y cierra sesiones.
     *  Si el usuario aún no verificó el correo, lo marca verificado acá — recibir el
     *  código de recuperación ya prueba acceso al correo, no tiene sentido pedir un
     *  segundo paso de verificación que dejaría la cuenta sin acceso. */
    suspend fun resetPassword(req: ResetPasswordRequest) {
        val email = normalizeEmail(req.email)
        validatePassword(req.newPassword)
        val user = users.findByEmail(email) ?: throw UnauthorizedException("Código inválido o expirado.")
        val tokenId = passwordResetTokens.findValid(user.id, tokens.hashCode(req.code))
            ?: throw UnauthorizedException("Código inválido o expirado.")
        if (!passwordResetTokens.consume(tokenId)) throw UnauthorizedException("Código inválido o expirado.")
        users.updatePassword(user.id, PasswordHasher.hash(req.newPassword))
        if (!user.emailVerified) users.markEmailVerified(user.id)
        refreshTokens.revokeAllForUser(user.id)
        sendSafely(EmailTemplates.passwordChanged(to = user.email, name = user.name.substringBefore(' ')))
    }

    private suspend fun issueFor(user: UserRow): AuthResponse {
        val roles = rbac.rolesOf(user.id).ifEmpty { listOf("cliente") }
        val access = tokens.issueAccessToken(user.id, roles)
        val refresh = tokens.generateRefreshToken()
        refreshTokens.create(user.id, tokens.hashRefreshToken(refresh), tokens.refreshExpiry())
        // Resolver el avatarUrl tambien en login/verifyEmail/refresh (no solo en /me).
        // Antes solo /me lo poblaba, asi que un login sobrescribia el avatarUrl del
        // TokenStore con null → la foto desaparecia hasta el proximo /me. Centralizando
        // aca, todos los puntos que emiten AuthResponse incluyen la URL presignada.
        val avatarUrl = avatarUrlResolver?.invoke(user.id)
        return AuthResponse(
            user = user.toDto().copy(roles = roles, avatarUrl = avatarUrl),
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
