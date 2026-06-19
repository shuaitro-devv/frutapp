package cl.frutapp.backend.modules.catalog

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.shared.dto.CategoryDto
import cl.frutapp.shared.dto.CreateProductRequest
import cl.frutapp.shared.dto.ProductDto
import java.text.Normalizer
import java.util.UUID

class CatalogService(private val repo: CatalogRepository) {

    suspend fun categories(): List<CategoryDto> = repo.listCategories()

    suspend fun products(category: String?, query: String?): List<ProductDto> =
        repo.listProducts(category, query)

    suspend fun product(id: String?): ProductDto? {
        val uuid = id?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return null
        return repo.findProduct(uuid)
    }

    /** Productos similares al dado para el SustitucionModal del picker. Si el
     *  productId es invalido o no existe, devuelve lista vacia (el picker vera
     *  "Sin alternativas") en lugar de error — el modal sigue siendo usable para
     *  marcar FALTANTE. */
    suspend fun similares(productIdRaw: String, limit: Int): List<ProductDto> {
        val uuid = runCatching { UUID.fromString(productIdRaw) }.getOrNull() ?: return emptyList()
        return repo.listSimilares(uuid, limit)
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

    /** El back office edita el precio fijado del producto. Valida > 0; 404 si no existe. */
    suspend fun setPrice(idRaw: String, priceClp: Int): ProductDto {
        if (priceClp <= 0) throw ValidationException("El precio debe ser mayor a 0.")
        val uuid = runCatching { UUID.fromString(idRaw) }.getOrNull()
            ?: throw ValidationException("Id de producto inválido.")
        val updated = repo.setPrice(uuid, priceClp)
        if (updated == 0) throw NotFoundException("Producto no encontrado.")
        return repo.findProduct(uuid) ?: throw NotFoundException("Producto no encontrado.")
    }

    /** Alta de producto desde el back office. Valida campos + categoría y genera slug único. */
    suspend fun createProduct(req: CreateProductRequest): ProductDto {
        if (req.name.isBlank()) throw ValidationException("El nombre es obligatorio.")
        if (req.priceClp <= 0) throw ValidationException("El precio debe ser mayor a 0.")
        if (req.unit.isBlank()) throw ValidationException("La unidad es obligatoria.")
        val categoryId = runCatching { UUID.fromString(req.categoryId) }.getOrNull()
            ?: throw ValidationException("Categoría inválida.")
        if (!repo.categoryExists(categoryId)) throw ValidationException("La categoría no existe.")

        val slug = uniqueSlug(slugify(req.name))
        val imageKey = req.imageKey?.trim()?.ifBlank { null } ?: slug
        return repo.createProduct(
            categoryId = categoryId,
            name = req.name.trim(),
            slug = slug,
            description = req.description?.trim().orEmpty(),
            priceClp = req.priceClp,
            unit = req.unit.trim(),
            imageKey = imageKey,
            disponible = req.disponible
        )
    }

    private suspend fun uniqueSlug(base: String): String {
        var slug = base
        var n = 2
        while (repo.slugExists(slug)) {
            slug = "$base-$n"
            n++
        }
        return slug
    }

}

/** Genera un slug URL-safe desde el nombre: sin acentos, minúsculas, símbolos -> guion. */
internal fun slugify(name: String): String {
    val sinAcentos = Normalizer
        .normalize(name.trim().lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
    return sinAcentos.replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "producto" }
}
