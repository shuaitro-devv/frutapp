package cl.frutapp.backend.modules.catalog

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.dto.CategoryDto
import cl.frutapp.shared.dto.ProductDto
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class CatalogRepository {

    suspend fun listCategories(): List<CategoryDto> = dbQuery {
        CategoryTable
            .selectAll()
            .orderBy(CategoryTable.sortOrder)
            .map(::toCategory)
    }

    suspend fun listProducts(categorySlug: String?, query: String?): List<ProductDto> = dbQuery {
        ProductTable
            .join(CategoryTable, JoinType.INNER, onColumn = ProductTable.categoryId, otherColumn = CategoryTable.id)
            .selectAll().where {
                // Construido dentro del lambda: isNull/eq/like son miembros de SqlExpressionBuilder.
                var cond: Op<Boolean> = ProductTable.deletedAt.isNull() and (ProductTable.active eq true)
                if (!categorySlug.isNullOrBlank()) {
                    cond = cond and (CategoryTable.slug eq categorySlug)
                }
                if (!query.isNullOrBlank()) {
                    cond = cond and (ProductTable.name.lowerCase() like "%${query.trim().lowercase()}%")
                }
                cond
            }
            .orderBy(ProductTable.name)
            .map(::toProduct)
    }

    suspend fun findProduct(id: UUID): ProductDto? = dbQuery {
        ProductTable
            .selectAll().where { (ProductTable.id eq id) and ProductTable.deletedAt.isNull() }
            .map(::toProduct)
            .singleOrNull()
    }

    /** Productos similares al dado (misma categoria, disponibles, distintos del original).
     *  Usado por el SustitucionModal del picker cuando ofrece alternativas reales. */
    suspend fun listSimilares(productId: UUID, limit: Int = 10): List<ProductDto> = dbQuery {
        // Sub-select de la category_id del producto original.
        val categoriaId = ProductTable
            .selectAll().where { ProductTable.id eq productId }
            .singleOrNull()
            ?.get(ProductTable.categoryId)
            ?: return@dbQuery emptyList()
        ProductTable
            .selectAll()
            .where {
                (ProductTable.categoryId eq categoriaId) and
                    (ProductTable.id neq productId) and
                    (ProductTable.disponible eq true) and  // Exposed: 'neq' es operator infix builtin
                    (ProductTable.active eq true) and
                    ProductTable.deletedAt.isNull()
            }
            .orderBy(ProductTable.name)
            .limit(limit)
            .map(::toProduct)
    }

    /** Busca por id (UUID) o por slug. Usado en la pre-pasa de validacion de
     *  disponibilidad del create-order para no asumir que la app siempre manda UUID. */
    suspend fun findProductByRef(ref: String): ProductDto? {
        val asUuid = runCatching { UUID.fromString(ref) }.getOrNull()
        if (asUuid != null) return findProduct(asUuid)
        return dbQuery {
            ProductTable
                .selectAll().where { (ProductTable.slug eq ref) and ProductTable.deletedAt.isNull() }
                .map(::toProduct)
                .singleOrNull()
        }
    }

    private fun toCategory(row: ResultRow) = CategoryDto(
        id = row[CategoryTable.id].toString(),
        name = row[CategoryTable.name],
        slug = row[CategoryTable.slug],
        sortOrder = row[CategoryTable.sortOrder]
    )

    private fun toProduct(row: ResultRow) = ProductDto(
        id = row[ProductTable.id].toString(),
        categoryId = row[ProductTable.categoryId].toString(),
        name = row[ProductTable.name],
        slug = row[ProductTable.slug],
        description = row[ProductTable.description],
        priceClp = row[ProductTable.priceClp],
        unit = row[ProductTable.unit],
        imageKey = row[ProductTable.imageKey],
        disponible = row[ProductTable.disponible]
    )

    /** Cambia disponibilidad operacional del producto. Devuelve filas afectadas:
     *  0 = no existe o esta soft-deleted. Endpoint admin lo traduce a 404 si 0. */
    suspend fun setDisponible(id: UUID, disponible: Boolean): Int = dbQuery {
        ProductTable.update({
            (ProductTable.id eq id) and ProductTable.deletedAt.isNull()
        }) {
            it[ProductTable.disponible] = disponible
            it[ProductTable.updatedAt] = kotlinx.datetime.Clock.System.now()
        }
    }

    /** Edita el precio del producto. Devuelve filas afectadas: 0 = no existe / soft-deleted. */
    suspend fun setPrice(id: UUID, priceClp: Int): Int = dbQuery {
        ProductTable.update({
            (ProductTable.id eq id) and ProductTable.deletedAt.isNull()
        }) {
            it[ProductTable.priceClp] = priceClp
            it[ProductTable.updatedAt] = kotlinx.datetime.Clock.System.now()
        }
    }

    /** ¿Existe la categoría? Para validar el alta antes de insertar (evita FK violation). */
    suspend fun categoryExists(id: UUID): Boolean = dbQuery {
        CategoryTable.selectAll().where { CategoryTable.id eq id }.limit(1).any()
    }

    /** ¿Hay un producto con ese slug? Para garantizar slug único al crear. */
    suspend fun slugExists(slug: String): Boolean = dbQuery {
        ProductTable.selectAll().where { ProductTable.slug eq slug }.limit(1).any()
    }

    /** Inserta un producto nuevo (active=true). El `slug` debe venir ya resuelto único. */
    suspend fun createProduct(
        categoryId: UUID,
        name: String,
        slug: String,
        description: String,
        priceClp: Int,
        unit: String,
        imageKey: String,
        disponible: Boolean
    ): ProductDto = dbQuery {
        val newId = UUID.randomUUID()
        val now = kotlinx.datetime.Clock.System.now()
        ProductTable.insert {
            it[id] = newId
            it[ProductTable.categoryId] = categoryId
            it[ProductTable.name] = name
            it[ProductTable.slug] = slug
            it[ProductTable.description] = description
            it[ProductTable.priceClp] = priceClp
            it[ProductTable.unit] = unit
            it[ProductTable.imageKey] = imageKey
            it[active] = true
            it[ProductTable.disponible] = disponible
            it[createdAt] = now
            it[updatedAt] = now
        }
        ProductDto(
            id = newId.toString(),
            categoryId = categoryId.toString(),
            name = name,
            slug = slug,
            description = description,
            priceClp = priceClp,
            unit = unit,
            imageKey = imageKey,
            disponible = disponible
        )
    }
}
