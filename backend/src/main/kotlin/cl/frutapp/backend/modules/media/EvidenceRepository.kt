package cl.frutapp.backend.modules.media

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.orders.OrderItemsTable
import cl.frutapp.backend.modules.orders.OrdersTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/** Acceso a `order_item_evidence`. Solo SQL — sin reglas de negocio. */
class EvidenceRepository {

    /** Inserta una evidencia por-item (picker). Resuelve el `orderId` desde el
     *  item al insertar (denormalizacion segura: si el itemId no pertenece al
     *  orderId del caller, retorna null). */
    suspend fun insert(
        orderId: UUID,
        orderItemId: UUID,
        imageKey: String,
        comentario: String?,
        uploadedBy: UUID
    ): EvidenceRow? = dbQuery {
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
            it[OrderItemEvidenceTable.tipo] = null // picker item evidence — sin tipo (legacy)
        }
        EvidenceRow(newId, orderItemId, imageKey, comentario, now.toString(), null)
    }

    /** Inserta una evidencia del pedido completo (repartidor). orderItemId = null.
     *  El caller (service) valida que el pedido este EN_DESPACHO y asignado al repartidor.
     *  [tipo] es 'DELIVERY_PHOTO' o 'DELIVERY_SIGNATURE' (V40+). Los inserts nuevos
     *  siempre lo setean; el null solo queda para filas legacy pre-V40. */
    suspend fun insertOrderLevel(
        orderId: UUID,
        imageKey: String,
        comentario: String?,
        uploadedBy: UUID,
        tipo: String,
    ): EvidenceRow = dbQuery {
        val newId = UUID.randomUUID()
        val now = Clock.System.now()
        OrderItemEvidenceTable.insert {
            it[id] = newId
            it[OrderItemEvidenceTable.orderItemId] = null
            it[OrderItemEvidenceTable.orderId] = orderId
            it[OrderItemEvidenceTable.imageKey] = imageKey
            it[OrderItemEvidenceTable.comentario] = comentario
            it[OrderItemEvidenceTable.uploadedBy] = uploadedBy
            it[OrderItemEvidenceTable.uploadedAt] = now
            it[OrderItemEvidenceTable.tipo] = tipo
        }
        EvidenceRow(newId, null, imageKey, comentario, now.toString(), tipo)
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
                    uploadedAt = row[OrderItemEvidenceTable.uploadedAt].toString(),
                    tipo = row[OrderItemEvidenceTable.tipo],
                )
            }
    }

    /** Borra la fila si sigue existiendo Y cumple todas las restricciones
     *  atomicamente: mismo pedido, es foto de entrega (orderItemId=null) y no
     *  hubo transicion de estado entre check y delete. Devuelve la fila
     *  borrada (para que el service borre el objeto en MinIO despues) o null
     *  si algo cambio. Reemplaza el patron find + check + delete anterior que
     *  tenia TOCTOU: si el pedido pasaba a ENTREGADO entre check y delete, la
     *  evidencia se borraba igual. */
    /** [tipoEsperado] restringe QUE tipo de evidencia se puede borrar. Sin esa
     *  restriccion, el endpoint DELETE de fotos permitia borrar la firma
     *  pasandole su UUID (contradice el diseño legal donde la firma es
     *  inmutable pre-entrega).
     *
     *  Chequea el status del pedido dentro del mismo dbQuery (misma transaccion
     *  Postgres, isolation Read Committed): si el pedido paso a ENTREGADO
     *  entre el check del service y aca, no borramos.
     */
    suspend fun deleteIfOrderLevel(id: UUID, orderId: UUID, tipoEsperado: String): EvidenceRow? = dbQuery {
        val row = OrderItemEvidenceTable
            .selectAll()
            .where {
                (OrderItemEvidenceTable.id eq id) and
                    (OrderItemEvidenceTable.orderId eq orderId) and
                    OrderItemEvidenceTable.orderItemId.isNull() and
                    (OrderItemEvidenceTable.tipo eq tipoEsperado)
            }
            .firstOrNull()
            ?: return@dbQuery null
        // Re-verifica el status del pedido en la MISMA transaccion. Si otro
        // proceso lo transiciono a ENTREGADO entre el check del service y
        // este delete, no borramos evidencia legal.
        val sigueEnDespacho = OrdersTable
            .selectAll()
            .where { (OrdersTable.id eq orderId) and (OrdersTable.status eq "EN_DESPACHO") }
            .any()
        if (!sigueEnDespacho) return@dbQuery null
        val affected = OrderItemEvidenceTable.deleteWhere { OrderItemEvidenceTable.id eq id }
        if (affected == 0) return@dbQuery null
        EvidenceRow(
            id = row[OrderItemEvidenceTable.id],
            orderItemId = row[OrderItemEvidenceTable.orderItemId],
            imageKey = row[OrderItemEvidenceTable.imageKey],
            comentario = row[OrderItemEvidenceTable.comentario],
            uploadedAt = row[OrderItemEvidenceTable.uploadedAt].toString(),
            tipo = row[OrderItemEvidenceTable.tipo],
        )
    }

    /** Devuelve las image_keys de todas las evidencias con [tipo] para
     *  [orderId] y las borra en la misma transaccion. Usado por el service
     *  para sobrescribir la firma: la anterior se remueve de BD y sus
     *  objetos MinIO se borran (evita huerfanos acumulados por re-firmas). */
    suspend fun deleteAndReturnKeysByTipo(orderId: UUID, tipo: String): List<String> = dbQuery {
        val keys = OrderItemEvidenceTable
            .select(OrderItemEvidenceTable.imageKey)
            .where {
                (OrderItemEvidenceTable.orderId eq orderId) and
                    OrderItemEvidenceTable.orderItemId.isNull() and
                    (OrderItemEvidenceTable.tipo eq tipo)
            }
            .map { it[OrderItemEvidenceTable.imageKey] }
        if (keys.isNotEmpty()) {
            // deleteWhere no expone el receiver de SqlExpressionBuilder para
            // llamar `.isNull()` directo — usamos Op.build para armar la
            // condicion con los operadores tipicos.
            OrderItemEvidenceTable.deleteWhere {
                Op.build {
                    (OrderItemEvidenceTable.orderId eq orderId) and
                        OrderItemEvidenceTable.orderItemId.isNull() and
                        (OrderItemEvidenceTable.tipo eq tipo)
                }
            }
        }
        keys
    }

    data class EvidenceRow(
        val id: UUID,
        val orderItemId: UUID?,
        val imageKey: String,
        val comentario: String?,
        val uploadedAt: String,
        /** NULL para legacy o filas de picker; 'DELIVERY_PHOTO' o 'DELIVERY_SIGNATURE'. */
        val tipo: String?,
    )
}
