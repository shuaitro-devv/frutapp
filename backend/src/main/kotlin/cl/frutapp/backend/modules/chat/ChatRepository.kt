package cl.frutapp.backend.modules.chat

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

/** Acceso a `chat_mensaje`. SQL puro — sin reglas de negocio. */
class ChatRepository {

    /**
     * Inserta un mensaje. [cuerpo] puede ser vacio si [imageKey] no es null
     * (mensaje solo-imagen). El check de la V31 garantiza que al menos uno
     * de los dos tenga contenido — si ambos estan vacios la DB rechaza.
     *
     * [forcedId] permite al caller (ChatService) reservar el UUID antes de
     * subir bytes al bucket: asi el key del bucket usa el mismo UUID que
     * la fila de DB y mensaje<->objeto matchean siempre.
     */
    suspend fun insert(
        orderId: UUID,
        autorUserId: UUID,
        autorRol: String,
        destinatarioRol: String,
        cuerpo: String,
        imageKey: String? = null,
        forcedId: UUID? = null,
    ): MensajeRow = dbQuery {
        val id = forcedId ?: UUID.randomUUID()
        val now = Clock.System.now()
        ChatMensajeTable.insert {
            it[ChatMensajeTable.id] = id
            it[ChatMensajeTable.orderId] = orderId
            it[ChatMensajeTable.autorUserId] = autorUserId
            it[ChatMensajeTable.autorRol] = autorRol
            it[ChatMensajeTable.destinatarioRol] = destinatarioRol
            it[ChatMensajeTable.cuerpo] = cuerpo
            it[ChatMensajeTable.imageKey] = imageKey
            it[ChatMensajeTable.createdAt] = now
        }
        MensajeRow(id, orderId, autorUserId, autorRol, destinatarioRol, cuerpo, imageKey, leidoEn = null, createdAt = now)
    }

    /** Historial del pedido ordenado del mas viejo al mas nuevo (cronologico,
     *  como WhatsApp). Si [desde] != null, devuelve solo los posteriores —
     *  para polling incremental o reconexion del WS. */
    suspend fun historial(orderId: UUID, desde: Instant? = null, limite: Int = 200): List<MensajeRow> = dbQuery {
        val baseQuery = ChatMensajeTable
            .selectAll()
            .where {
                if (desde != null) {
                    (ChatMensajeTable.orderId eq orderId) and
                        (ChatMensajeTable.createdAt.greater(desde))
                } else {
                    ChatMensajeTable.orderId eq orderId
                }
            }
            .orderBy(ChatMensajeTable.createdAt, SortOrder.ASC)
            .limit(limite)
        baseQuery.map { row ->
            MensajeRow(
                id = row[ChatMensajeTable.id],
                orderId = row[ChatMensajeTable.orderId],
                autorUserId = row[ChatMensajeTable.autorUserId],
                autorRol = row[ChatMensajeTable.autorRol],
                destinatarioRol = row[ChatMensajeTable.destinatarioRol],
                cuerpo = row[ChatMensajeTable.cuerpo],
                imageKey = row[ChatMensajeTable.imageKey],
                leidoEn = row[ChatMensajeTable.leidoEn],
                createdAt = row[ChatMensajeTable.createdAt],
            )
        }
    }

    /** El destinatario marca todos los mensajes de su lado como leidos.
     *  No-op si ya estaban marcados. */
    suspend fun marcarLeidos(orderId: UUID, destinatarioRol: String): Int = dbQuery {
        val now = Clock.System.now()
        ChatMensajeTable.update({
            (ChatMensajeTable.orderId eq orderId) and
            (ChatMensajeTable.destinatarioRol eq destinatarioRol) and
            ChatMensajeTable.leidoEn.isNull()
        }) {
            it[leidoEn] = now
        }
    }

    data class MensajeRow(
        val id: UUID,
        val orderId: UUID,
        val autorUserId: UUID,
        val autorRol: String,
        val destinatarioRol: String,
        val cuerpo: String,
        val imageKey: String?,
        val leidoEn: Instant?,
        val createdAt: Instant,
    )
}
