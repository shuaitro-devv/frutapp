package cl.frutapp.backend.modules.notifications

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Token de FCM por device. Modelo en V13__device_tokens.sql.
 *
 * fcm_token tiene UNIQUE: si Juan logout y Maria login en el mismo celu, FCM
 * devuelve el mismo token y reasignamos la fila a Maria (sino, ella seguiria
 * recibiendo push del pedido de Juan).
 */
object DeviceTokensTable : Table("device_token") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val fcmToken = text("fcm_token")
    val platform = text("platform")
    val appId = text("app_id").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(id)
}

/**
 * Inbox de notificaciones del usuario (V14). Cada push enviado via FCM se
 * persiste primero acá para que la pantalla `NotificacionesScreen` lea de
 * `GET /v1/notifications` y sea consistente entre devices (no del store
 * local que se perdia al kill-cerrar la app).
 *
 * `data` queda como text (no jsonb tipado en Exposed) — basta para guardar
 * un JSON con orderId/deltaCoins/etc. El front lo deserializa segun [type].
 */
object NotificationInboxTable : Table("notification") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val type = text("type")
    val title = text("title")
    val body = text("body")
    val data = text("data").nullable()
    val createdAt = timestamp("created_at")
    val readAt = timestamp("read_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
