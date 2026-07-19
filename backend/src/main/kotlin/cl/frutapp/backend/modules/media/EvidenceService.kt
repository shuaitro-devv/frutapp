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

    /** El repartidor sube UNA foto del paquete entregado. Distinto del picker:
     *  es evidencia del pedido completo, no de un item. Valida que el pedido
     *  este EN_DESPACHO y asignado a este repartidor — sino, se rechaza. */
    suspend fun uploadAsRepartidor(
        repartidorId: UUID,
        orderId: UUID,
        bytes: ByteArray,
        contentType: String,
        comentario: String?,
        context: EventContext
    ): OrderItemEvidenceDto {
        validarImagen(bytes, contentType)
        validarComentario(comentario)
        val puedeSubir = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedRepartidorId eq repartidorId) and
                (OrdersTable.status eq "EN_DESPACHO")
            }.any()
        }
        if (!puedeSubir) throw ValidationException("Este pedido no está en despacho o no es tuyo.")
        val evidenceId = UUID.randomUUID()
        val key = "evidence/$orderId/delivery/$evidenceId.jpg"
        storage.subir(key, bytes, contentType)
        val row = repo.insertOrderLevel(orderId, key, comentario, repartidorId, "DELIVERY_PHOTO")
        events.logSafely(
            eventType = "staff.delivery_evidence_uploaded",
            userId = repartidorId,
            entityType = "customer_order",
            entityId = orderId,
            payload = buildJsonObject {
                put("evidenceId", JsonPrimitive(row.id.toString()))
                if (comentario != null) put("comentario", JsonPrimitive(comentario))
            },
            context = context
        )
        return OrderItemEvidenceDto(
            id = row.id.toString(),
            orderItemId = null,
            url = storage.urlFirmada(row.imageKey),
            comentario = row.comentario,
            uploadedAt = row.uploadedAt,
            tipo = row.tipo,
        )
    }

    /** El repartidor sube UNA firma del receptor (PNG con trazos del cliente).
     *  Mismo gate que la foto de entrega: pedido EN_DESPACHO + assigned al
     *  repartidor. La firma se guarda como PNG (transparencia + trazos negros),
     *  key `evidence/{orderId}/signature/{uuid}.png`. Si ya habia una firma para
     *  este pedido, se pisa por la nueva (una sola firma vigente por entrega). */
    suspend fun uploadSignatureAsRepartidor(
        repartidorId: UUID,
        orderId: UUID,
        bytes: ByteArray,
        context: EventContext,
    ): OrderItemEvidenceDto {
        // Reusamos el validador pero forzando PNG (la firma no es JPG).
        if (bytes.size.toLong() > MAX_BYTES) {
            throw ValidationException("La firma pesa más de 5 MB. Vuelve a firmar con trazos más simples.")
        }
        if (!verifyMagicNumber(bytes, "image/png")) {
            throw ValidationException("La firma no es un PNG válido.")
        }
        val puedeSubir = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedRepartidorId eq repartidorId) and
                (OrdersTable.status eq "EN_DESPACHO")
            }.any()
        }
        if (!puedeSubir) throw ValidationException("Este pedido no está en despacho o no es tuyo.")
        // Cleanup de firmas anteriores del mismo pedido: la ultima gana. Sin
        // esto cada re-firma dejaba una fila huerfana en BD y un objeto en
        // MinIO — con N repartidores re-firmando el bucket y la tabla crecen
        // linealmente sin uso. Best-effort en MinIO: los que fallen quedan
        // huerfanos pero sin bloquear al repartidor.
        val keysHuerfanas = repo.deleteAndReturnKeysByTipo(orderId, "DELIVERY_SIGNATURE")
        keysHuerfanas.forEach { k -> runCatching { storage.borrar(k) } }
        val evidenceId = UUID.randomUUID()
        val key = "evidence/$orderId/signature/$evidenceId.png"
        storage.subir(key, bytes, "image/png")
        val row = repo.insertOrderLevel(orderId, key, comentario = null, uploadedBy = repartidorId, tipo = "DELIVERY_SIGNATURE")
        events.logSafely(
            eventType = "staff.delivery_signature_uploaded",
            userId = repartidorId,
            entityType = "customer_order",
            entityId = orderId,
            payload = buildJsonObject {
                put("evidenceId", JsonPrimitive(row.id.toString()))
            },
            context = context,
        )
        return OrderItemEvidenceDto(
            id = row.id.toString(),
            orderItemId = null,
            url = storage.urlFirmada(row.imageKey),
            comentario = row.comentario,
            uploadedAt = row.uploadedAt,
            tipo = row.tipo,
        )
    }

    /** El repartidor borra una foto de entrega que subio. Restricciones:
     *   - Pedido debe estar EN_DESPACHO y asignado a este repartidor (mismo
     *     gate que subir — no puede borrar despues de confirmar entrega).
     *   - Solo evidencia de ENTREGA (orderItemId=null) — no permitimos borrar
     *     evidencias del picker aca (esas son de otro workflow y otro rol).
     *  El delete de BD lo hace `repo.deleteIfOrderLevel` en UNA transaccion
     *  atomica (evita TOCTOU si el pedido pasa a ENTREGADO entre el check y
     *  el delete). Idempotente: si la fila ya no existe (borrada por otra
     *  sesion o reintento) devolvemos NotFound — el cliente lo trata como
     *  exito y limpia estado local. */
    suspend fun deleteAsRepartidor(
        repartidorId: UUID,
        orderId: UUID,
        evidenceId: UUID,
        context: EventContext,
    ) {
        val puedeBorrar = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedRepartidorId eq repartidorId) and
                (OrdersTable.status eq "EN_DESPACHO")
            }.any()
        }
        if (!puedeBorrar) throw ValidationException("Este pedido no está en despacho o no es tuyo.")
        // Solo permitimos borrar FOTOS de entrega desde este endpoint. La firma
        // se sobrescribe subiendo una nueva; no hay path para borrarla sola.
        val deleted = repo.deleteIfOrderLevel(evidenceId, orderId, tipoEsperado = "DELIVERY_PHOTO")
            ?: throw NotFoundException("Evidencia no encontrada.")
        // BD borrada; ahora limpiamos MinIO. Si falla queda el objeto huerfano
        // en el bucket — aceptable. Al reves seria peor: fila apuntando a nada.
        runCatching { storage.borrar(deleted.imageKey) }
        events.logSafely(
            eventType = "staff.delivery_evidence_deleted",
            userId = repartidorId,
            entityType = "customer_order",
            entityId = orderId,
            payload = buildJsonObject {
                put("evidenceId", JsonPrimitive(evidenceId.toString()))
            },
            context = context,
        )
    }

    /** Lista las evidencias de un pedido para el cliente (tracking). El
     *  caller (route) ya verifica ownership del pedido por el clienteId. */
    suspend fun listByOrder(orderId: UUID): List<OrderItemEvidenceDto> =
        repo.listByOrder(orderId).map { row ->
            OrderItemEvidenceDto(
                id = row.id.toString(),
                orderItemId = row.orderItemId?.toString(),
                url = storage.urlFirmada(row.imageKey),
                comentario = row.comentario,
                uploadedAt = row.uploadedAt,
                tipo = row.tipo,
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
            orderItemId = row.orderItemId?.toString(),
            url = storage.urlFirmada(row.imageKey),
            comentario = row.comentario,
            uploadedAt = row.uploadedAt,
            tipo = row.tipo,
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
