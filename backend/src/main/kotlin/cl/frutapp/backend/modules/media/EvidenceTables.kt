package cl.frutapp.backend.modules.media

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Tabla [order_item_evidence] (V25). Foto + comentario opcional asociado a un
 * item de pedido. N a 1 con `order_item` aunque la UI MVP solo permita 1
 * foto por item; el backend acepta N desde el dia 1 para que el frontend
 * pueda escalar sin tocar BD.
 *
 * `orderId` esta DENORMALIZADO (no derivado por JOIN) para que la query del
 * tracking del cliente ("dame todas las evidencias de este pedido agrupadas
 * por item") sea barata. Se setea al insertar y no se actualiza nunca.
 *
 * El default de uploaded_at lo pone la BD (`DEFAULT now()` en la migration),
 * por eso aca NO especificamos clientDefault.
 */
internal object OrderItemEvidenceTable : Table("order_item_evidence") {
    val id = uuid("id")
    // Nullable desde V38: null significa "foto del pedido completo" (entrega
    // del repartidor). NOT NULL sigue siendo foto del picker asociada a item.
    val orderItemId = uuid("order_item_id").nullable()
    val orderId = uuid("order_id")
    val imageKey = text("image_key")
    val comentario = text("comentario").nullable()
    val uploadedBy = uuid("uploaded_by")
    val uploadedAt = timestamp("uploaded_at")
    // V40: distingue foto de entrega vs firma. NULL = legacy (pre-V40).
    val tipo = text("tipo").nullable()

    override val primaryKey = PrimaryKey(id)
}
