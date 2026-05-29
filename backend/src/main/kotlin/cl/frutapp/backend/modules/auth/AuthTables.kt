package cl.frutapp.backend.modules.auth

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/** Tabla de usuarios. El esquema lo crea Flyway; Exposed solo lee/escribe. */
object UsersTable : Table("app_user") {
    val id = uuid("id")
    val name = text("name")
    val email = text("email")
    val phone = text("phone").nullable()
    val passwordHash = text("password_hash")
    val role = text("role")
    val emailVerified = bool("email_verified")
    val consentVersion = text("consent_version").nullable()
    val consentAt = timestamp("consent_at").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

/** Refresh tokens: se guarda solo el hash; soporta rotación (revoked_at) y expiración. */
object RefreshTokensTable : Table("refresh_token") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val tokenHash = text("token_hash")
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

/** Códigos de recuperación de contraseña: se guarda solo el hash del código. */
object PasswordResetTokensTable : Table("password_reset_token") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val codeHash = text("code_hash")
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

/** Códigos de verificación de correo: se guarda solo el hash del código. */
object EmailVerificationTokensTable : Table("email_verification_token") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val codeHash = text("code_hash")
    val expiresAt = timestamp("expires_at")
    val usedAt = timestamp("used_at").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
