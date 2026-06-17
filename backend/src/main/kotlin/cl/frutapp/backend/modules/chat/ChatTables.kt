package cl.frutapp.backend.modules.chat

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/** V30 `chat_mensaje`: mensajes del chat in-app por pedido. */
internal object ChatMensajeTable : Table("chat_mensaje") {
    val id = uuid("id")
    val orderId = uuid("order_id")
    val autorUserId = uuid("autor_user_id")
    val autorRol = text("autor_rol")
    val destinatarioRol = text("destinatario_rol")
    val cuerpo = text("cuerpo")
    val leidoEn = timestamp("leido_en").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

/** Roles del chat. Mismos strings que las filas en `role` (V8 RBAC). */
object ChatRol {
    const val CLIENTE = "cliente"
    const val PICKER = "picker"
    const val REPARTIDOR = "repartidor"
    val VALIDOS = setOf(CLIENTE, PICKER, REPARTIDOR)
}
