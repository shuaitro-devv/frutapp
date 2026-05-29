package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class EmailVerificationTokenRepository {

    suspend fun create(userId: UUID, codeHash: String, expiresAt: Instant) = dbQuery {
        EmailVerificationTokensTable.insert {
            it[id] = UUID.randomUUID()
            it[EmailVerificationTokensTable.userId] = userId
            it[EmailVerificationTokensTable.codeHash] = codeHash
            it[EmailVerificationTokensTable.expiresAt] = expiresAt
        }
        Unit
    }

    /** Token vigente (no usado, no vencido) para ese usuario + hash de código. */
    suspend fun findValid(userId: UUID, codeHash: String): UUID? = dbQuery {
        EmailVerificationTokensTable
            .selectAll().where {
                (EmailVerificationTokensTable.userId eq userId) and
                    (EmailVerificationTokensTable.codeHash eq codeHash) and
                    EmailVerificationTokensTable.usedAt.isNull() and
                    (EmailVerificationTokensTable.expiresAt greater Clock.System.now())
            }
            .map { it[EmailVerificationTokensTable.id] }
            .singleOrNull()
    }

    /** Consume el token de forma atómica: lo marca usado SOLO si seguía sin usar.
     *  Devuelve true si este llamado lo consumió (evita doble uso en carrera). */
    suspend fun consume(id: UUID): Boolean = dbQuery {
        EmailVerificationTokensTable.update({
            (EmailVerificationTokensTable.id eq id) and EmailVerificationTokensTable.usedAt.isNull()
        }) {
            it[usedAt] = Clock.System.now()
        } == 1
    }

    /** Invalida códigos previos del usuario (al reenviar uno nuevo). */
    suspend fun invalidateAllForUser(userId: UUID) = dbQuery {
        EmailVerificationTokensTable.update({
            (EmailVerificationTokensTable.userId eq userId) and EmailVerificationTokensTable.usedAt.isNull()
        }) {
            it[usedAt] = Clock.System.now()
        }
        Unit
    }
}
