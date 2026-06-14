package cl.frutapp.backend.modules.media

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.EventContext
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.shared.dto.OrderItemEvidenceDto
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Servicio de evidencia visual del picker.
 *
 * Validaciones de upload alineadas con AvatarService (MIME whitelist, tamaño
 * maximo, magic-number check) — los pickers usan camaras de celus a baja
 * resolucion; 5 MB es ceiling generoso para un solo JPG.
 *
 * Storage key: `evidence/{order_id}/{item_id}/{evidence_id}.jpg` — incluye el
 * evidence_id para que multiples fotos del mismo item NO se sobrescriban (a
 * diferencia del avatar, donde si queremos sobrescribir).
 */
class EvidenceService(
    private val storage: StorageService,
    private val repo: EvidenceRepository,
    private val events: UserEventService
) {
    companion object {
        private const val MAX_BYTES = 5L * 1024 * 1024  // 5 MB
        private const val MAX_COMENTARIO_CHARS = 500
        private val MIME_WHITELIST = setOf("image/jpeg", "image/png")
    }

    /** El picker sube una foto al item del pedido. Valida ownership: el
     *  pedido debe estar EN_PICKING y asignado a este picker. Si el pedido ya
     *  cambio de estado (otro picker rescato, o el picker mismo completo),
     *  el upload se rechaza. */
    suspend fun uploadAsPicker(
        pickerId: UUID,
        orderId: UUID,
        orderItemId: UUID,
        bytes: ByteArray,
        contentType: String,
        comentario: String?,
        context: EventContext
    ): OrderItemEvidenceDto {
        validarImagen(bytes, contentType)
        validarComentario(comentario)
        val pickerOwns = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq "EN_PICKING")
            }.any()
        }
        if (!pickerOwns) throw ValidationException("Este pedido no está en picking o no es tuyo.")
        return insertarYResponder(orderId, orderItemId, pickerId, bytes, contentType, comentario, context, "staff.evidence_uploaded")
    }

    /** Lista las evidencias de un pedido para el cliente (tracking). El
     *  caller (route) ya verifica ownership del pedido por el clienteId. */
    suspend fun listByOrder(orderId: UUID): List<OrderItemEvidenceDto> =
        repo.listByOrder(orderId).map { row ->
            OrderItemEvidenceDto(
                id = row.id.toString(),
                orderItemId = row.orderItemId.toString(),
                url = storage.urlFirmada(row.imageKey),
                comentario = row.comentario,
                uploadedAt = row.uploadedAt
            )
        }

    private suspend fun insertarYResponder(
        orderId: UUID,
        orderItemId: UUID,
        uploadedBy: UUID,
        bytes: ByteArray,
        contentType: String,
        comentario: String?,
        context: EventContext,
        eventType: String
    ): OrderItemEvidenceDto {
        // Reservamos un UUID antes para usarlo en el key del bucket.
        val evidenceId = UUID.randomUUID()
        val key = "evidence/$orderId/$orderItemId/$evidenceId.jpg"
        // Subimos PRIMERO al bucket; si el insert falla, queda un objeto
        // huerfano en MinIO (aceptable; el bucket no es scarce). Si subimos
        // DESPUES del insert y MinIO falla, queda una fila DB que apunta a
        // un key inexistente → el cliente ve "imagen rota" en el tracking,
        // peor experiencia.
        storage.subir(key, bytes, contentType)
        val row = repo.insert(orderId, orderItemId, key, comentario, uploadedBy)
            ?: run {
                // El item no pertenece al pedido — borramos lo subido.
                storage.borrar(key)
                throw NotFoundException("Item no encontrado en este pedido.")
            }
        events.logSafely(
            eventType = eventType,
            userId = uploadedBy,
            entityType = "order_item",
            entityId = orderItemId,
            payload = buildJsonObject {
                put("orderId", JsonPrimitive(orderId.toString()))
                put("evidenceId", JsonPrimitive(row.id.toString()))
                if (comentario != null) put("comentario", JsonPrimitive(comentario))
            },
            context = context
        )
        return OrderItemEvidenceDto(
            id = row.id.toString(),
            orderItemId = row.orderItemId.toString(),
            url = storage.urlFirmada(row.imageKey),
            comentario = row.comentario,
            uploadedAt = row.uploadedAt
        )
    }

    private fun validarImagen(bytes: ByteArray, contentType: String) {
        if (contentType !in MIME_WHITELIST) {
            throw ValidationException("Tipo de imagen no permitido. Solo JPEG o PNG.")
        }
        if (bytes.size.toLong() > MAX_BYTES) {
            throw ValidationException("La imagen pesa más de 5 MB. Sácala con menos resolución o comprímela.")
        }
        if (!verifyMagicNumber(bytes, contentType)) {
            throw ValidationException("La imagen está corrupta o no coincide con el tipo declarado.")
        }
    }

    private fun validarComentario(c: String?) {
        if (c == null) return
        if (c.length > MAX_COMENTARIO_CHARS) {
            throw ValidationException("El comentario no puede tener más de $MAX_COMENTARIO_CHARS caracteres.")
        }
    }

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
