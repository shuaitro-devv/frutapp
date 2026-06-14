package cl.frutapp.backend.modules.media

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.orders.OrderItemsTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/** Acceso a `order_item_evidence`. Solo SQL — sin reglas de negocio. */
class EvidenceRepository {

    /** Inserta una evidencia. Resuelve el `orderId` desde el item al insertar
     *  (denormalizacion segura: si el itemId no pertenece al orderId del caller,
     *  el INSERT falla por el WHERE del lookup). */
    suspend fun insert(
        orderId: UUID,
        orderItemId: UUID,
        imageKey: String,
        comentario: String?,
        uploadedBy: UUID
    ): EvidenceRow? = dbQuery {
        // Confirma que el item pertenezca al pedido — anti-mismatch.
        val itemOk = OrderItemsTable
            .selectAll().where { (OrderItemsTable.id eq orderItemId) and (OrderItemsTable.orderId eq orderId) }
            .any()
        if (!itemOk) return@dbQuery null
        val newId = UUID.randomUUID()
        val now = Clock.System.now()
        OrderItemEvidenceTable.insert {
            it[id] = newId
            it[OrderItemEvidenceTable.orderItemId] = orderItemId
            it[OrderItemEvidenceTable.orderId] = orderId
            it[OrderItemEvidenceTable.imageKey] = imageKey
            it[OrderItemEvidenceTable.comentario] = comentario
            it[OrderItemEvidenceTable.uploadedBy] = uploadedBy
            it[OrderItemEvidenceTable.uploadedAt] = now
        }
        EvidenceRow(newId, orderItemId, imageKey, comentario, now.toString())
    }

    /** Todas las evidencias de un pedido (cliente las ve en tracking). Mas
     *  reciente primero por item. */
    suspend fun listByOrder(orderId: UUID): List<EvidenceRow> = dbQuery {
        OrderItemEvidenceTable
            .selectAll().where { OrderItemEvidenceTable.orderId eq orderId }
            .orderBy(OrderItemEvidenceTable.uploadedAt, SortOrder.DESC)
            .map { row ->
                EvidenceRow(
                    id = row[OrderItemEvidenceTable.id],
                    orderItemId = row[OrderItemEvidenceTable.orderItemId],
                    imageKey = row[OrderItemEvidenceTable.imageKey],
                    comentario = row[OrderItemEvidenceTable.comentario],
                    uploadedAt = row[OrderItemEvidenceTable.uploadedAt].toString()
                )
            }
    }

    data class EvidenceRow(
        val id: UUID,
        val orderItemId: UUID,
        val imageKey: String,
        val comentario: String?,
        val uploadedAt: String
    )
}
