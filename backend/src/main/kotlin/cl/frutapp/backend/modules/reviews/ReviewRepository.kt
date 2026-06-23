package cl.frutapp.backend.modules.reviews

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.auth.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

/** Acceso a `producto_resena`. JOIN con `app_user` para devolver el nombre
 *  del autor sin que la app tenga que mapear por su cuenta. */
class ReviewRepository {

    /** Upsert manual: si ya existe (productId, userId), update; si no, insert.
     *  Devuelve el row resultante (id, createdAt fijo desde el primer post).
     *  La relectura va en la MISMA dbQuery para no anidar newSuspendedTransaction
     *  (anidarlas con Exposed + suspended transactions tira IllegalStateException). */
    /** Upsert manual. [imageKey] semantica:
     *   - null  → "no tocar" en update (se mantiene la imagen anterior).
     *   - ""    → "borrar" (set NULL en BD).
     *   - "..." → "reemplazar" (set al nuevo key).
     *  Para insert: null/"" = sin imagen; "..." = con imagen.
     *  [forcedId] permite al service reservar el UUID antes de subir bytes
     *  a MinIO; key del bucket usa el mismo id que la fila. */
    suspend fun upsert(
        productId: UUID,
        userId: UUID,
        estrellas: Int,
        texto: String,
        imageKey: String? = null,
        forcedId: UUID? = null,
    ): ResenaRow = dbQuery {
        val now = Clock.System.now()
        val existente = ProductoResenaTable.selectAll().where {
            (ProductoResenaTable.productId eq productId) and (ProductoResenaTable.userId eq userId)
        }.singleOrNull()
        val id = if (existente != null) {
            val existenteId = existente[ProductoResenaTable.id]
            ProductoResenaTable.update({ ProductoResenaTable.id eq existenteId }) {
                it[ProductoResenaTable.estrellas] = estrellas
                it[ProductoResenaTable.texto] = texto
                if (imageKey != null) {
                    // "" → set NULL (borrar foto), no-vacio → set al nuevo key.
                    it[ProductoResenaTable.imageKey] = imageKey.ifEmpty { null }
                }
                it[updatedAt] = now
            }
            existenteId
        } else {
            val nuevoId = forcedId ?: UUID.randomUUID()
            ProductoResenaTable.insert {
                it[ProductoResenaTable.id] = nuevoId
                it[ProductoResenaTable.productId] = productId
                it[ProductoResenaTable.userId] = userId
                it[ProductoResenaTable.estrellas] = estrellas
                it[ProductoResenaTable.texto] = texto
                it[ProductoResenaTable.imageKey] = imageKey?.ifEmpty { null }
                it[ProductoResenaTable.createdAt] = now
                it[ProductoResenaTable.updatedAt] = now
            }
            nuevoId
        }
        // Releer en LA MISMA transaccion para devolver el nombre del autor sin
        // anidar dbQuery — el JOIN va inline.
        ProductoResenaTable.join(
            UsersTable,
            org.jetbrains.exposed.sql.JoinType.INNER,
            onColumn = ProductoResenaTable.userId,
            otherColumn = UsersTable.id,
        )
            .selectAll()
            .where { ProductoResenaTable.id eq id }
            .single()
            .toRow()
    }

    /** Lista resenas del producto ordenadas por created_at DESC. */
    suspend fun listarPorProducto(productId: UUID, limite: Int = 50): List<ResenaRow> = dbQuery {
        ProductoResenaTable.join(
            UsersTable,
            org.jetbrains.exposed.sql.JoinType.INNER,
            onColumn = ProductoResenaTable.userId,
            otherColumn = UsersTable.id,
        )
            .selectAll()
            .where { ProductoResenaTable.productId eq productId }
            .orderBy(ProductoResenaTable.createdAt, SortOrder.DESC)
            .limit(limite)
            .map { it.toRow() }
    }

    /** La resena del usuario para este producto, o null si no existe. */
    suspend fun miResena(productId: UUID, userId: UUID): ResenaRow? = dbQuery {
        ProductoResenaTable.join(
            UsersTable,
            org.jetbrains.exposed.sql.JoinType.INNER,
            onColumn = ProductoResenaTable.userId,
            otherColumn = UsersTable.id,
        )
            .selectAll()
            .where {
                (ProductoResenaTable.productId eq productId) and (ProductoResenaTable.userId eq userId)
            }
            .singleOrNull()
            ?.toRow()
    }

    /** Devuelve solo el imageKey actual (o null) de la resena del usuario para
     *  este producto. Util para borrar el objeto en MinIO al reemplazarlo. */
    suspend fun imageKeyActual(productId: UUID, userId: UUID): String? = dbQuery {
        ProductoResenaTable
            .select(ProductoResenaTable.imageKey)
            .where {
                (ProductoResenaTable.productId eq productId) and (ProductoResenaTable.userId eq userId)
            }
            .singleOrNull()
            ?.get(ProductoResenaTable.imageKey)
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toRow(): ResenaRow = ResenaRow(
        id = this[ProductoResenaTable.id],
        productId = this[ProductoResenaTable.productId],
        userId = this[ProductoResenaTable.userId],
        autorNombre = this[UsersTable.name],
        estrellas = this[ProductoResenaTable.estrellas],
        texto = this[ProductoResenaTable.texto],
        imageKey = this[ProductoResenaTable.imageKey],
        createdAt = this[ProductoResenaTable.createdAt],
        updatedAt = this[ProductoResenaTable.updatedAt],
    )

    data class ResenaRow(
        val id: UUID,
        val productId: UUID,
        val userId: UUID,
        val autorNombre: String,
        val estrellas: Int,
        val texto: String,
        val imageKey: String?,
        val createdAt: Instant,
        val updatedAt: Instant,
    )
}
