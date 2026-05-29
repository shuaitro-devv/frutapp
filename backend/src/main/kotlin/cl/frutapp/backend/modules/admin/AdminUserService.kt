package cl.frutapp.backend.modules.admin

import cl.frutapp.backend.error.ConflictException
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.auth.EmailTemplates
import cl.frutapp.backend.modules.auth.EmailSender
import cl.frutapp.backend.modules.auth.PasswordHasher
import cl.frutapp.backend.modules.auth.PasswordResetTokenRepository
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.auth.UserRow
import cl.frutapp.backend.modules.rbac.RbacRepository
import cl.frutapp.shared.dto.AdminCreateUserRequest
import cl.frutapp.shared.dto.AdminUserDto
import cl.frutapp.shared.dto.SetRolesRequest
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.days

/**
 * Gestión de usuarios de equipo (back office). Crea staff/proveedor con rol(es) y los
 * INVITA: la cuenta queda verificada pero sin contraseña usable, y se envía un código para
 * que la persona fije su clave con el flujo de reset existente (POST /v1/auth/reset-password).
 * Compone repos existentes; no duplica lógica de auth. El registro público sigue creando
 * solo `cliente`, así que el alta de roles elevados solo pasa por acá (gated por permiso).
 */
class AdminUserService(
    private val users: UserRepository,
    private val rbac: RbacRepository,
    private val passwordResetTokens: PasswordResetTokenRepository,
    private val tokens: TokenService,
    private val emailSender: EmailSender
) {
    private val logger = LoggerFactory.getLogger(AdminUserService::class.java)

    suspend fun createUser(req: AdminCreateUserRequest): AdminUserDto {
        if (req.name.isBlank()) throw ValidationException("El nombre es obligatorio.")
        if (req.roles.isEmpty()) throw ValidationException("Debes asignar al menos un rol.")
        val email = normalizeEmail(req.email)
        validateRoles(req.roles)
        if (users.findByEmail(email) != null) throw ConflictException("Ya existe una cuenta con ese correo.")

        // Contraseña aleatoria inutilizable: la persona la fija vía invitación (reset).
        val user = users.create(
            name = req.name.trim(),
            email = email,
            phone = req.phone?.trim()?.ifBlank { null },
            passwordHash = PasswordHasher.hash(tokens.generateRefreshToken()),
            role = req.roles.first()
        )
        users.markEmailVerified(user.id) // el admin es el que da fe; no requiere verificar correo
        req.roles.forEach { rbac.assignRole(user.id, it) }
        sendInvitation(user)
        return dto(user.id, user.name, user.email, user.phone)
    }

    suspend fun setRoles(idStr: String, req: SetRolesRequest): AdminUserDto {
        val id = runCatching { UUID.fromString(idStr) }.getOrNull() ?: throw ValidationException("Id inválido.")
        val user = users.findById(id) ?: throw NotFoundException("Usuario no encontrado.")
        validateRoles(req.add + req.remove)
        req.add.forEach { rbac.assignRole(id, it) }
        req.remove.forEach { rbac.revokeRole(id, it) }
        return dto(user.id, user.name, user.email, user.phone)
    }

    private suspend fun dto(id: UUID, name: String, email: String, phone: String?): AdminUserDto =
        AdminUserDto(id.toString(), name, email, phone, rbac.rolesOf(id))

    private suspend fun validateRoles(roles: List<String>) {
        if (roles.isEmpty()) return
        val valid = rbac.allRoleCodes()
        val unknown = roles.filter { it !in valid }
        if (unknown.isNotEmpty()) throw ValidationException("Rol(es) inválido(s): ${unknown.joinToString()}")
    }

    private suspend fun sendInvitation(user: UserRow) {
        passwordResetTokens.invalidateAllForUser(user.id)
        val code = tokens.generateNumericCode()
        passwordResetTokens.create(user.id, tokens.hashCode(code), Clock.System.now() + 7.days)
        runCatching { emailSender.send(EmailTemplates.invitation(user.email, user.name.substringBefore(' '), code)) }
            .onFailure { logger.error("No se pudo enviar invitación a {}", user.email, it) }
    }

    private fun normalizeEmail(email: String): String {
        val normalized = email.trim().lowercase()
        if (!EMAIL_REGEX.matches(normalized)) throw ValidationException("El correo no es válido.")
        return normalized
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
