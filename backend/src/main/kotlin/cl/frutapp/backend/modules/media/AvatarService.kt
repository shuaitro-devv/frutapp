package cl.frutapp.backend.modules.media

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.auth.UsersTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * Sube y borra el avatar de perfil del usuario autenticado. Validaciones de
 * seguridad ANTES de tocar el storage:
 *  - MIME whitelist (image/jpeg, image/png).
 *  - Tamaño máximo 2 MB (compresión cliente debería bajarlo a ~150 KB; este
 *    es solo el ceiling de seguridad).
 *  - Magic-number check para que no se cuele un .exe disfrazado con
 *    Content-Type: image/jpeg.
 *
 * Key estable por user: re-subir SOBRESCRIBE en el bucket, no acumula. Asi un
 * usuario que cambia foto 5 veces no deja 4 archivos huerfanos.
 */
class AvatarService(
    private val storage: StorageService
) {
    companion object {
        private const val MAX_BYTES = 2L * 1024 * 1024  // 2 MB
        private val MIME_WHITELIST = setOf("image/jpeg", "image/png")
    }

    /** Sube la foto y devuelve la URL presignada nueva (TTL 1h). */
    suspend fun upload(userId: UUID, bytes: ByteArray, contentType: String): String {
        require(contentType in MIME_WHITELIST) {
            throw ValidationException("Tipo de imagen no permitido. Solo JPEG o PNG.")
        }
        require(bytes.size.toLong() <= MAX_BYTES) {
            throw ValidationException("La imagen pesa más de 2 MB. Probá una más liviana.")
        }
        if (!verifyMagicNumber(bytes, contentType)) {
            throw ValidationException("La imagen está corrupta o no coincide con el tipo declarado.")
        }
        // Key fija por user: "users/{uuid}/avatar.jpg" — sobrescribe al re-subir.
        // Usamos `.jpg` siempre como extensión aunque el contentType sea PNG: la
        // extensión del object_key en MinIO es un detalle de presentación; el
        // contentType real lo persistimos en el metadata del objeto.
        val key = "users/$userId/avatar.jpg"
        storage.subir(key, bytes, contentType)
        dbQuery {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.avatarObjectKey] = key
            }
        }
        return storage.urlFirmada(key)
    }

    /** Borra el avatar (si tiene). Idempotente. */
    suspend fun delete(userId: UUID) {
        val key = dbQuery {
            UsersTable
                .select(UsersTable.avatarObjectKey)
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?.get(UsersTable.avatarObjectKey)
        } ?: return
        storage.borrar(key)
        dbQuery {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[UsersTable.avatarObjectKey] = null
            }
        }
    }

    /** Genera URL presignada para el avatar guardado, o null si no hay. Usado
     *  por `AuthService.me` para devolverla en `UserDto.avatarUrl`. */
    suspend fun urlFor(userId: UUID): String? {
        val key = dbQuery {
            UsersTable
                .select(UsersTable.avatarObjectKey)
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?.get(UsersTable.avatarObjectKey)
        } ?: return null
        return storage.urlFirmada(key)
    }

    /** Magic-number check defensivo: confirma que los primeros bytes correspondan
     *  al tipo declarado para evitar payload-spoofing. */
    private fun verifyMagicNumber(bytes: ByteArray, contentType: String): Boolean {
        if (bytes.size < 8) return false
        return when (contentType) {
            "image/jpeg" -> bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            "image/png" -> bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            else -> false
        }
    }
}
