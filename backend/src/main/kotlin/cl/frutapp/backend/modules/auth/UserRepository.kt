package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.dto.UserDto
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

/** Fila de usuario en memoria (incluye el hash, que nunca sale al cliente). */
data class UserRow(
    val id: UUID,
    val name: String,
    val email: String,
    val phone: String?,
    val passwordHash: String,
    val role: String,
    val emailVerified: Boolean
) {
    fun toDto() = UserDto(id = id.toString(), name = name, email = email, phone = phone, role = role)
}

class UserRepository {

    suspend fun findByEmail(email: String): UserRow? = dbQuery {
        UsersTable
            .selectAll().where { (UsersTable.email eq email) and UsersTable.deletedAt.isNull() }
            .map(::toRow)
            .singleOrNull()
    }

    suspend fun findById(id: UUID): UserRow? = dbQuery {
        UsersTable
            .selectAll().where { (UsersTable.id eq id) and UsersTable.deletedAt.isNull() }
            .map(::toRow)
            .singleOrNull()
    }

    /** `email` debe venir ya normalizado (trim + lowercase). */
    suspend fun create(
        name: String,
        email: String,
        phone: String?,
        passwordHash: String,
        role: String,
        consentVersion: String? = null
    ): UserRow = dbQuery {
        val newId = UUID.randomUUID()
        UsersTable.insert {
            it[id] = newId
            it[UsersTable.name] = name
            it[UsersTable.email] = email
            it[UsersTable.phone] = phone
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
            it[UsersTable.emailVerified] = false
            if (consentVersion != null) {
                it[UsersTable.consentVersion] = consentVersion
                it[UsersTable.consentAt] = Clock.System.now()
            }
        }
        UserRow(newId, name, email, phone, passwordHash, role, emailVerified = false)
    }

    suspend fun markEmailVerified(userId: UUID) = dbQuery {
        UsersTable.update({ (UsersTable.id eq userId) and UsersTable.deletedAt.isNull() }) {
            it[UsersTable.emailVerified] = true
            it[UsersTable.updatedAt] = Clock.System.now()
        }
        Unit
    }

    suspend fun updatePassword(userId: UUID, passwordHash: String) = dbQuery {
        UsersTable.update({ (UsersTable.id eq userId) and UsersTable.deletedAt.isNull() }) {
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.updatedAt] = Clock.System.now()
        }
        Unit
    }

    private fun toRow(row: ResultRow) = UserRow(
        id = row[UsersTable.id],
        name = row[UsersTable.name],
        email = row[UsersTable.email],
        phone = row[UsersTable.phone],
        passwordHash = row[UsersTable.passwordHash],
        role = row[UsersTable.role],
        emailVerified = row[UsersTable.emailVerified]
    )
}
