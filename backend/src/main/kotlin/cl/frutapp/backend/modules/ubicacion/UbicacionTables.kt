package cl.frutapp.backend.modules.ubicacion

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/** Tabla [dispatch_ubicacion] (V27): ultima ubicacion del repartidor por pedido. */
internal object DispatchUbicacionTable : Table("dispatch_ubicacion") {
    val orderId = uuid("order_id")
    val repartidorId = uuid("repartidor_id")
    val lat = decimal("lat", precision = 10, scale = 7)
    val lng = decimal("lng", precision = 10, scale = 7)
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(orderId)
}
