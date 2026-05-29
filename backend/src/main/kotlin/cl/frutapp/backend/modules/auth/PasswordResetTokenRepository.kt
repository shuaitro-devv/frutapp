package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.util.UUID

class PasswordResetTokenRepository {

    suspend fun create(userId: UUID, codeHash: String, expiresAt: Instant) = dbQuery {
        PasswordResetTokensTable.insert {
            it[id] = UUID.randomUUID()
            it[PasswordResetTokensTable.userId] = userId
            it[PasswordResetTokensTable.codeHash] = codeHash
            it[PasswordResetTokensTable.expiresAt] = expiresAt
        }
        Unit
    }

    /** Token vigente (no usado, no vencido) para ese usuario + hash de código. */
    suspend fun findValid(userId: UUID, codeHash: String): UUID? = dbQuery {
        PasswordResetTokensTable
            .select {
                (PasswordResetTokensTable.userId eq userId) and
                    (PasswordResetTokensTable.codeHash eq codeHash) and
                    PasswordResetTokensTable.usedAt.isNull() and
                    (PasswordResetTokensTable.expiresAt greater Clock.System.now())
            }
            .map { it[PasswordResetTokensTable.id] }
            .singleOrNull()
    }

    /** Consume el token de forma atómica: lo marca usado SOLO si seguía sin usar.
     *  Devuelve true si este llamado lo consumió (evita doble uso en carrera). */
    suspend fun consume(id: UUID): Boolean = dbQuery {
        PasswordResetTokensTable.update({
            (PasswordResetTokensTable.id eq id) and PasswordResetTokensTable.usedAt.isNull()
        }) {
            it[usedAt] = Clock.System.now()
        } == 1
    }

    /** Invalida códigos previos del usuario (al pedir uno nuevo). */
    suspend fun invalidateAllForUser(userId: UUID) = dbQuery {
        PasswordResetTokensTable.update({
            (PasswordResetTokensTable.userId eq userId) and PasswordResetTokensTable.usedAt.isNull()
        }) {
            it[usedAt] = Clock.System.now()
        }
        Unit
    }
}
