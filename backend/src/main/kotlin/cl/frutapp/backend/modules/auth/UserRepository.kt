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
        consentVersion: String? = null,
        codigoInvitacion: String? = null,
        referredByUserId: UUID? = null,
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
            it[UsersTable.codigoInvitacion] = codigoInvitacion
            it[UsersTable.referredByUserId] = referredByUserId
            it[UsersTable.referralRewardGranted] = false
        }
        UserRow(newId, name, email, phone, passwordHash, role, emailVerified = false)
    }

    /** Busca un usuario por su codigo de invitacion. Case-sensitive porque
     *  el codigo es siempre upper-case + charset restringido. Null si no
     *  matchea (el signup ignora silenciosamente codigos invalidos para no
     *  bloquear el registro por typos). */
    suspend fun findByCodigoInvitacion(codigo: String): UserRow? = dbQuery {
        UsersTable.selectAll()
            .where { (UsersTable.codigoInvitacion eq codigo) and UsersTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let { toRow(it) }
    }

    /** Devuelve el codigo del usuario, generandolo si es null (backfill lazy
     *  para casos edge donde la migracion no lo genero). Idempotente. */
    suspend fun ensureCodigoInvitacion(userId: UUID): String = dbQuery {
        val existente = UsersTable.select(UsersTable.codigoInvitacion)
            .where { UsersTable.id eq userId }
            .singleOrNull()?.get(UsersTable.codigoInvitacion)
        if (existente != null) return@dbQuery existente
        // Retry ~5x en caso de colision (con 31^8 combinaciones, colision es raro).
        repeat(5) {
            val candidato = generarCodigo()
            val filas = UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.codigoInvitacion] = candidato
            }
            if (filas > 0) return@dbQuery candidato
        }
        error("No se pudo generar codigo de invitacion")
    }

    /** Marca la recompensa como otorgada. Idempotente (WHERE-guarded contra
     *  falso true concurrente); devuelve true si esta llamada fue la que la
     *  seteo, false si ya estaba true (caller no debe pagar de nuevo). */
    suspend fun claimReferralReward(userId: UUID): Boolean = dbQuery {
        val filas = UsersTable.update({
            (UsersTable.id eq userId) and (UsersTable.referralRewardGranted eq false)
        }) {
            it[UsersTable.referralRewardGranted] = true
            it[UsersTable.updatedAt] = Clock.System.now()
        }
        filas > 0
    }

    /** Devuelve (referredById, alreadyGranted) para un usuario. Null si el
     *  user no existe. */
    suspend fun referralInfoOf(userId: UUID): Pair<UUID?, Boolean>? = dbQuery {
        UsersTable.select(UsersTable.referredByUserId, UsersTable.referralRewardGranted)
            .where { UsersTable.id eq userId }
            .singleOrNull()
            ?.let { it[UsersTable.referredByUserId] to it[UsersTable.referralRewardGranted] }
    }

    private fun generarCodigo(): String {
        val charset = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..8).map { charset[kotlin.random.Random.nextInt(charset.length)] }.joinToString("")
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

    /** Refresca datos editables del usuario en un solo UPDATE. Cualquier parametro null se
     *  ignora — solo se escriben los campos provistos. Usado al re-registrar una cuenta no
     *  verificada para que el segundo intento de nombre/telefono/consent NO se descarte. */
    suspend fun updateProfileFields(
        userId: UUID,
        name: String? = null,
        phone: String? = null,
        consentVersion: String? = null
    ) = dbQuery {
        UsersTable.update({ (UsersTable.id eq userId) and UsersTable.deletedAt.isNull() }) {
            if (name != null) it[UsersTable.name] = name
            if (phone != null) it[UsersTable.phone] = phone
            if (consentVersion != null) {
                it[UsersTable.consentVersion] = consentVersion
                it[UsersTable.consentAt] = Clock.System.now()
            }
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
