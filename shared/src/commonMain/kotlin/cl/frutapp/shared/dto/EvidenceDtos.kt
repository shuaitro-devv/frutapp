package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/**
 * Foto de evidencia de un item de pedido. La UI MVP solo soporta 1 foto por
 * item pero la API permite N (la tabla `order_item_evidence` es N:1).
 *
 * `url` viene presignada (TTL 1h por StorageService.urlFirmada). El cliente
 * no guarda la URL local; cada vez que entra al tracking pide de nuevo el
 * detalle y la URL viene fresca.
 */
@Serializable
data class OrderItemEvidenceDto(
    val id: String,
    /** null cuando la foto es del pedido completo (entrega del repartidor). */
    val orderItemId: String? = null,
    val url: String,
    val comentario: String? = null,
    val uploadedAt: String,
)

/** Respuesta del upload: la evidencia recien creada con su URL presignada. */
@Serializable
data class UploadEvidenceResponse(val evidencia: OrderItemEvidenceDto)
