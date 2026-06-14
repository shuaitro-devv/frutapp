package cl.frutapp.backend.modules.ubicacion

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/** Acceso a `dispatch_ubicacion`. SQL puro. */
class UbicacionRepository {

    /** Upsert: una sola fila por pedido. Insert si no existe; UPDATE si si.
     *  Mas simple que ON CONFLICT y suficientemente performante para la
     *  cadencia esperada (un POST cada 10s por pedido EN_DESPACHO). */
    suspend fun upsert(orderId: UUID, repartidorId: UUID, lat: Double, lng: Double) = dbQuery {
        val now = Clock.System.now()
        val latBd = lat.toBigDecimal().setScale(7, RoundingMode.HALF_UP)
        val lngBd = lng.toBigDecimal().setScale(7, RoundingMode.HALF_UP)
        val updated = DispatchUbicacionTable.update({ DispatchUbicacionTable.orderId eq orderId }) {
            it[DispatchUbicacionTable.repartidorId] = repartidorId
            it[DispatchUbicacionTable.lat] = latBd
            it[DispatchUbicacionTable.lng] = lngBd
            it[DispatchUbicacionTable.updatedAt] = now
        }
        if (updated == 0) {
            DispatchUbicacionTable.insert {
                it[DispatchUbicacionTable.orderId] = orderId
                it[DispatchUbicacionTable.repartidorId] = repartidorId
                it[DispatchUbicacionTable.lat] = latBd
                it[DispatchUbicacionTable.lng] = lngBd
                it[DispatchUbicacionTable.updatedAt] = now
            }
        }
    }

    suspend fun findByOrder(orderId: UUID): UbicacionRow? = dbQuery {
        DispatchUbicacionTable
            .selectAll().where { DispatchUbicacionTable.orderId eq orderId }
            .singleOrNull()?.let {
                UbicacionRow(
                    orderId = it[DispatchUbicacionTable.orderId],
                    repartidorId = it[DispatchUbicacionTable.repartidorId],
                    lat = it[DispatchUbicacionTable.lat].toDouble(),
                    lng = it[DispatchUbicacionTable.lng].toDouble(),
                    updatedAt = it[DispatchUbicacionTable.updatedAt],
                )
            }
    }

    data class UbicacionRow(
        val orderId: UUID,
        val repartidorId: UUID,
        val lat: Double,
        val lng: Double,
        val updatedAt: Instant,
    )
}
