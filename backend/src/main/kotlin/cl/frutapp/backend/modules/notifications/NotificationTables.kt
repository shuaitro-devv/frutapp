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
