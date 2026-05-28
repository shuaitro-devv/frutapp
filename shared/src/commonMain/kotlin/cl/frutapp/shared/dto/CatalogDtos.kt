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
    val imageKey: String
)
