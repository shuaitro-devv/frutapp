package cl.frutapp.backend.modules.notifications

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.dto.NotificationDto
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class NotificationInboxRepository {

    /** Inserta una notif en el inbox del user. Llamada desde
     *  [NotificationDispatcher] ANTES de enviar el push: si el push falla,
     *  igual queda traza para que el user la vea al abrir la app. */
    suspend fun create(
        userId: UUID,
        type: String,
        title: String,
        body: String,
        data: String?
    ): UUID = dbQuery {
        val newId = UUID.randomUUID()
        val now = Clock.System.now()
        NotificationInboxTable.insert {
            it[NotificationInboxTable.id] = newId
            it[NotificationInboxTable.userId] = userId
            it[NotificationInboxTable.type] = type
            it[NotificationInboxTable.title] = title
            it[NotificationInboxTable.body] = body
            it[NotificationInboxTable.data] = data
            it[NotificationInboxTable.createdAt] = now
        }
        newId
    }

    /** Las N mas recientes del user (no leidas primero, luego leidas) ordenadas
     *  DESC por created_at. Limite default razonable para no tirar todo de
     *  golpe; cliente puede paginar despues si hace falta. */
    suspend fun listByUser(userId: UUID, limit: Int = 50): List<NotificationDto> = dbQuery {
        NotificationInboxTable
            .selectAll()
            .where { NotificationInboxTable.userId eq userId }
            .orderBy(NotificationInboxTable.createdAt to SortOrder.DESC)
            .limit(limit)
            .map(::toDto)
    }

    suspend fun countUnread(userId: UUID): Int = dbQuery {
        NotificationInboxTable
            .selectAll()
            .where { (NotificationInboxTable.userId eq userId) and NotificationInboxTable.readAt.isNull() }
            .count()
            .toInt()
    }

    /** Marca como leida solo si pertenece al user (evita read-cruzado entre users). */
    suspend fun markRead(notificationId: UUID, userId: UUID): Boolean = dbQuery {
        val now = Clock.System.now()
        val updated = NotificationInboxTable.update({
            (NotificationInboxTable.id eq notificationId) and
            (NotificationInboxTable.userId eq userId) and
            NotificationInboxTable.readAt.isNull()
        }) {
            it[NotificationInboxTable.readAt] = now
        }
        updated > 0
    }

    suspend fun markAllReadFor(userId: UUID): Int = dbQuery {
        val now = Clock.System.now()
        NotificationInboxTable.update({
            (NotificationInboxTable.userId eq userId) and NotificationInboxTable.readAt.isNull()
        }) {
            it[NotificationInboxTable.readAt] = now
        }
    }

    private fun toDto(r: org.jetbrains.exposed.sql.ResultRow) = NotificationDto(
        id = r[NotificationInboxTable.id].toString(),
        type = r[NotificationInboxTable.type],
        title = r[NotificationInboxTable.title],
        body = r[NotificationInboxTable.body],
        data = r[NotificationInboxTable.data],
        createdAt = r[NotificationInboxTable.createdAt].toString(),
        readAt = r[NotificationInboxTable.readAt]?.toString()
    )
}
