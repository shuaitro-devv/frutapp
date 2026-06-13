package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Categoría del catálogo. */
@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val slug: String,
    val sortOrder: Int
)

/** Producto del catálogo. `imageKey` mapea al drawable de la app. */
@Serializable
data class ProductDto(
    val id: String,
    val categoryId: String,
    val name: String,
    val slug: String,
    val description: String,
    val priceClp: Int,
    val unit: String,
    val imageKey: String,
    /** Disponibilidad operacional. false = agotado: card en gris con badge
     *  "Agotado" y boton "Agregar" disabled. Default true para retrocompat
     *  con clientes viejos. */
    val disponible: Boolean = true
)

/** Request del back office para flipear disponibilidad de un producto. */
@Serializable
data class SetProductAvailabilityRequest(
    val disponible: Boolean
)
