package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

data class RefreshTokenRow(val id: UUID, val userId: UUID)

class RefreshTokenRepository {

    suspend fun create(userId: UUID, tokenHash: String, expiresAt: Instant) = dbQuery {
        RefreshTokensTable.insert {
            it[id] = UUID.randomUUID()
            it[RefreshTokensTable.userId] = userId
            it[RefreshTokensTable.tokenHash] = tokenHash
            it[RefreshTokensTable.expiresAt] = expiresAt
        }
        Unit
    }

    /** Devuelve el token si existe, no está revocado y no venció. */
    suspend fun findValid(tokenHash: String): RefreshTokenRow? = dbQuery {
        RefreshTokensTable
            .selectAll().where {
                (RefreshTokensTable.tokenHash eq tokenHash) and
                    RefreshTokensTable.revokedAt.isNull() and
                    (RefreshTokensTable.expiresAt greater Clock.System.now())
            }
            .map { RefreshTokenRow(it[RefreshTokensTable.id], it[RefreshTokensTable.userId]) }
            .singleOrNull()
    }

    suspend fun revoke(id: UUID) = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.id eq id }) {
            it[revokedAt] = Clock.System.now()
        }
        Unit
    }

    /** Revoca por hash (logout). Devuelve cuántos se revocaron. */
    suspend fun revokeByHash(tokenHash: String): Int = dbQuery {
        RefreshTokensTable.update({
            (RefreshTokensTable.tokenHash eq tokenHash) and RefreshTokensTable.revokedAt.isNull()
        }) {
            it[revokedAt] = Clock.System.now()
        }
    }

    /** Revoca todas las sesiones del usuario (ej. tras cambiar la contraseña). */
    suspend fun revokeAllForUser(userId: UUID): Int = dbQuery {
        RefreshTokensTable.update({
            (RefreshTokensTable.userId eq userId) and RefreshTokensTable.revokedAt.isNull()
        }) {
            it[revokedAt] = Clock.System.now()
        }
    }
}
