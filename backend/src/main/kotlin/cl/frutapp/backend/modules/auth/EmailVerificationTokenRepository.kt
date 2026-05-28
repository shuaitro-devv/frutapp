package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
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
            .select {
                (EmailVerificationTokensTable.userId eq userId) and
                    (EmailVerificationTokensTable.codeHash eq codeHash) and
                    EmailVerificationTokensTable.usedAt.isNull() and
                    (EmailVerificationTokensTable.expiresAt greater Clock.System.now())
            }
            .map { it[EmailVerificationTokensTable.id] }
            .singleOrNull()
    }

    suspend fun markUsed(id: UUID) = dbQuery {
        EmailVerificationTokensTable.update({ EmailVerificationTokensTable.id eq id }) {
            it[usedAt] = Clock.System.now()
        }
        Unit
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
