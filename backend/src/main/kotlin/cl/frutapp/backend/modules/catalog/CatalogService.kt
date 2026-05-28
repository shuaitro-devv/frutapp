package cl.frutapp.backend.modules.catalog

import cl.frutapp.shared.dto.CategoryDto
import cl.frutapp.shared.dto.ProductDto
import java.util.UUID

class CatalogService(private val repo: CatalogRepository) {

    suspend fun categories(): List<CategoryDto> = repo.listCategories()

    suspend fun products(category: String?, query: String?): List<ProductDto> =
        repo.listProducts(category, query)

    suspend fun product(id: String?): ProductDto? {
        val uuid = id?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return null
        return repo.findProduct(uuid)
    }
}
