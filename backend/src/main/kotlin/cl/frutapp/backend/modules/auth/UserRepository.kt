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
     *  para casos edge donde la migracion no lo genero). Idempotente y safe
     *  bajo concurrencia.
     *
     *  Fix v0.1.18: la version anterior tenia dos bugs:
     *   1. TOCTOU en el UPDATE: no filtraba `codigoInvitacion IS NULL`, asi
     *      que dos calls concurrentes hacian SELECT null, generaban codigos
     *      distintos, y el segundo pisaba al primero. El caller A retornaba
     *      un codigo que ya no era el del user.
     *   2. Colision con codigo existente de otro user tiraba SQLException
     *      de UNIQUE, no `filas == 0`; el retry loop asumia lo segundo y
     *      nunca reintentaba.
     *
     *  Ahora: UPDATE con guard `codigoInvitacion IS NULL` + catch de
     *  ExposedSQLException dentro del retry (colision UNIQUE ~ retry). */
    suspend fun ensureCodigoInvitacion(userId: UUID): String = dbQuery {
        val existente = UsersTable.select(UsersTable.codigoInvitacion)
            .where { UsersTable.id eq userId }
            .singleOrNull()?.get(UsersTable.codigoInvitacion)
        if (existente != null) return@dbQuery existente
        repeat(5) {
            val candidato = generarCodigo()
            val exitoso = try {
                UsersTable.update({
                    (UsersTable.id eq userId) and UsersTable.codigoInvitacion.isNull()
                }) {
                    it[UsersTable.codigoInvitacion] = candidato
                } > 0
            } catch (_: org.jetbrains.exposed.exceptions.ExposedSQLException) {
                // Colision UNIQUE con codigo de otro user. Reintentamos.
                false
            }
            if (exitoso) return@dbQuery candidato
            // Si filas == 0 sin excepcion, otro thread ya nos gano en el
            // race. Releemos el codigo que quedo persistido.
            val quePersisto = UsersTable.select(UsersTable.codigoInvitacion)
                .where { UsersTable.id eq userId }
                .singleOrNull()?.get(UsersTable.codigoInvitacion)
            if (quePersisto != null) return@dbQuery quePersisto
        }
        error("No se pudo generar codigo de invitacion tras 5 intentos")
    }

    /** Comprueba si dos usuarios comparten el mismo `email_base` (todo lo
     *  anterior al `+` del local-part) o el mismo telefono. Sirve para
     *  detectar auto-referrals: alice@gmail.com registra alice+1@gmail.com
     *  con su propio codigo de invitacion. Fix v0.1.18. */
    suspend fun compartenIdentidad(referrerId: UUID, emailNuevo: String, telefonoNuevo: String?): Boolean = dbQuery {
        val referrer = UsersTable.select(UsersTable.email, UsersTable.phone)
            .where { (UsersTable.id eq referrerId) and UsersTable.deletedAt.isNull() }
            .singleOrNull() ?: return@dbQuery false
        val emailReferrer = referrer[UsersTable.email]
        val phoneReferrer = referrer[UsersTable.phone]
        if (emailBase(emailNuevo) == emailBase(emailReferrer)) return@dbQuery true
        if (!telefonoNuevo.isNullOrBlank() && telefonoNuevo == phoneReferrer) return@dbQuery true
        false
    }

    /** `alice+X@gmail.com` -> `alice@gmail.com`. Normaliza para detectar
     *  el truco clasico de "sub-address" que casi todos los MTAs enrutan
     *  al mismo mailbox. No es 100% robusto (Yahoo usa `-` en vez de `+`)
     *  pero cubre Gmail/Outlook/iCloud, que son la mayoria en Chile. */
    private fun emailBase(email: String): String {
        val at = email.indexOf('@')
        if (at < 0) return email.lowercase()
        val local = email.substring(0, at).substringBefore('+')
        return "$local${email.substring(at)}".lowercase()
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
