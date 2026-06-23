package cl.frutapp.backend.modules.baskets

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID

/**
 * Acceso a `canasta` + `canasta_item`. Las operaciones que tocan ambas tablas
 * van en la misma transaccion (crear/actualizar reemplaza los items inline).
 */
class BasketRepository {

    suspend fun listarPorUsuario(userId: UUID): List<CanastaConItems> = dbQuery {
        val cabeceras = CanastaTable.selectAll()
            .where { CanastaTable.userId eq userId }
            .orderBy(CanastaTable.createdAt, SortOrder.DESC)
            .map { it.toRowCabecera() }
        if (cabeceras.isEmpty()) return@dbQuery emptyList<CanastaConItems>()
        val ids = cabeceras.map { it.id }
        val items = CanastaItemTable.selectAll()
            .where { CanastaItemTable.canastaId.inList(ids) }
            .orderBy(CanastaItemTable.posicion, SortOrder.ASC)
            .map { it.toRowItem() }
            .groupBy { it.canastaId }
        cabeceras.map { CanastaConItems(it, items[it.id].orEmpty()) }
    }

    suspend fun cargar(canastaId: UUID, userId: UUID): CanastaConItems? = dbQuery {
        val cabecera = CanastaTable.selectAll()
            .where { (CanastaTable.id eq canastaId) and (CanastaTable.userId eq userId) }
            .singleOrNull()
            ?.toRowCabecera() ?: return@dbQuery null
        val items = CanastaItemTable.selectAll()
            .where { CanastaItemTable.canastaId eq canastaId }
            .orderBy(CanastaItemTable.posicion, SortOrder.ASC)
            .map { it.toRowItem() }
        CanastaConItems(cabecera, items)
    }

    suspend fun crear(
        userId: UUID,
        nombre: String,
        emoji: String,
        recordatorioMensual: Boolean,
        items: List<NuevoItem>,
    ): CanastaConItems = dbQuery {
        val now = Clock.System.now()
        val canastaId = UUID.randomUUID()
        CanastaTable.insert {
            it[CanastaTable.id] = canastaId
            it[CanastaTable.userId] = userId
            it[CanastaTable.nombre] = nombre
            it[CanastaTable.emoji] = emoji
            it[CanastaTable.recordatorioMensual] = recordatorioMensual
            it[CanastaTable.createdAt] = now
            it[CanastaTable.updatedAt] = now
        }
        items.forEachIndexed { idx, item ->
            CanastaItemTable.insert {
                it[CanastaItemTable.id] = UUID.randomUUID()
                it[CanastaItemTable.canastaId] = canastaId
                it[CanastaItemTable.productId] = item.productId
                it[CanastaItemTable.cantidad] = item.cantidad
                it[CanastaItemTable.gramos] = item.gramos
                it[CanastaItemTable.posicion] = idx
                it[CanastaItemTable.createdAt] = now
            }
        }
        // Releer en la misma transaccion para devolver el objeto consistente.
        cargarInline(canastaId, userId)
            ?: error("Canasta recien creada no encontrada.")
    }

    /** Actualiza header + reemplaza items (delete-all + insert). Si el cliente
     *  no manda [items], se mantiene la lista actual y solo se updatea header. */
    suspend fun actualizar(
        canastaId: UUID,
        userId: UUID,
        nombre: String?,
        emoji: String?,
        recordatorioMensual: Boolean?,
        items: List<NuevoItem>?,
    ): CanastaConItems? = dbQuery {
        val existe = CanastaTable.selectAll()
            .where { (CanastaTable.id eq canastaId) and (CanastaTable.userId eq userId) }
            .singleOrNull() ?: return@dbQuery null
        val now = Clock.System.now()
        CanastaTable.update({ CanastaTable.id eq canastaId }) {
            if (nombre != null) it[CanastaTable.nombre] = nombre
            if (emoji != null) it[CanastaTable.emoji] = emoji
            if (recordatorioMensual != null) it[CanastaTable.recordatorioMensual] = recordatorioMensual
            it[updatedAt] = now
        }
        if (items != null) {
            CanastaItemTable.deleteWhere { CanastaItemTable.canastaId eq canastaId }
            items.forEachIndexed { idx, item ->
                CanastaItemTable.insert {
                    it[CanastaItemTable.id] = UUID.randomUUID()
                    it[CanastaItemTable.canastaId] = canastaId
                    it[CanastaItemTable.productId] = item.productId
                    it[CanastaItemTable.cantidad] = item.cantidad
                    it[CanastaItemTable.gramos] = item.gramos
                    it[CanastaItemTable.posicion] = idx
                    it[CanastaItemTable.createdAt] = now
                }
            }
        }
        cargarInline(canastaId, userId)
    }

    suspend fun eliminar(canastaId: UUID, userId: UUID): Boolean = dbQuery {
        val n = CanastaTable.deleteWhere {
            (CanastaTable.id eq canastaId) and (CanastaTable.userId eq userId)
        }
        n > 0
    }

    /** Recarga inline (sin abrir nueva dbQuery). Llamado desde otra dbQuery. */
    private fun cargarInline(canastaId: UUID, userId: UUID): CanastaConItems? {
        val cabecera = CanastaTable.selectAll()
            .where { (CanastaTable.id eq canastaId) and (CanastaTable.userId eq userId) }
            .singleOrNull()
            ?.toRowCabecera() ?: return null
        val items = CanastaItemTable.selectAll()
            .where { CanastaItemTable.canastaId eq canastaId }
            .orderBy(CanastaItemTable.posicion, SortOrder.ASC)
            .map { it.toRowItem() }
        return CanastaConItems(cabecera, items)
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toRowCabecera(): CanastaCabecera = CanastaCabecera(
        id = this[CanastaTable.id],
        userId = this[CanastaTable.userId],
        nombre = this[CanastaTable.nombre],
        emoji = this[CanastaTable.emoji],
        recordatorioMensual = this[CanastaTable.recordatorioMensual],
        createdAt = this[CanastaTable.createdAt],
        updatedAt = this[CanastaTable.updatedAt],
    )

    private fun org.jetbrains.exposed.sql.ResultRow.toRowItem(): CanastaItemRow = CanastaItemRow(
        id = this[CanastaItemTable.id],
        canastaId = this[CanastaItemTable.canastaId],
        productId = this[CanastaItemTable.productId],
        cantidad = this[CanastaItemTable.cantidad],
        gramos = this[CanastaItemTable.gramos],
        posicion = this[CanastaItemTable.posicion],
    )

    data class CanastaCabecera(
        val id: UUID,
        val userId: UUID,
        val nombre: String,
        val emoji: String,
        val recordatorioMensual: Boolean,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    data class CanastaItemRow(
        val id: UUID,
        val canastaId: UUID,
        val productId: UUID,
        val cantidad: Int,
        val gramos: Int?,
        val posicion: Int,
    )

    data class CanastaConItems(
        val cabecera: CanastaCabecera,
        val items: List<CanastaItemRow>,
    )

    /** Input simple para crear/actualizar items. */
    data class NuevoItem(val productId: UUID, val cantidad: Int, val gramos: Int?)
}
