package cl.frutapp.backend.modules.catalog

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
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

    /** El back office flipea disponibilidad operacional. 404 si el producto no
     *  existe o esta soft-deleted (active=false sigue siendo flipeable: un producto
     *  pausado del catalogo igual puede tener stock que entra cuando se reactive). */
    suspend fun setAvailability(idRaw: String, disponible: Boolean): ProductDto {
        val uuid = runCatching { UUID.fromString(idRaw) }.getOrNull()
            ?: throw ValidationException("Id de producto inválido.")
        val updated = repo.setDisponible(uuid, disponible)
        if (updated == 0) throw NotFoundException("Producto no encontrado.")
        return repo.findProduct(uuid) ?: throw NotFoundException("Producto no encontrado.")
    }
}
