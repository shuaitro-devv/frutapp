package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Una resena de producto tal como la ve la app. */
@Serializable
data class ResenaDto(
    val id: String,
    val productoId: String,
    val autorNombre: String,
    val autorUserId: String,
    val estrellas: Int,    // 1..5
    val texto: String,
    /** URL presignada de la imagen adjunta, o null si la resena es solo texto.
     *  El backend la firma cada vez que la devuelve (no se persiste). */
    val imagenUrl: String? = null,
    val createdAt: String, // ISO
    val updatedAt: String, // ISO
)

/** Body para POST /v1/products/{id}/reviews. Upsert: si ya existe la del
 *  usuario para ese producto, se actualiza; si no, se crea. */
@Serializable
data class CrearResenaRequest(
    val estrellas: Int,     // 1..5, validado tambien en backend
    val texto: String = "", // opcional, max 500 chars
)
