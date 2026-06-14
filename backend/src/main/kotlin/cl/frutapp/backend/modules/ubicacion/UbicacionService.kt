package cl.frutapp.backend.modules.ubicacion

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.shared.dto.UbicacionDto
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Tracking de la ubicacion del repartidor para un pedido en curso.
 *
 * Diseno:
 *  - El repartidor REPORTA con ownership: solo el repartidor asignado al
 *    pedido y mientras esta EN_DESPACHO. Cierra el caso de un repartidor
 *    posteando ubicacion en pedidos que no son suyos.
 *  - El cliente CONSULTA con ownership: solo el dueño del pedido. Sin
 *    esto un atacante con cualquier orderId podria seguir a otros
 *    repartidores en tiempo real.
 *  - Sin guardarrails de privacidad mas alla: el cliente necesita ver al
 *    repartidor venir; el repartidor sabe que esta en delivery.
 *
 *  Cadencia esperada: la app del repartidor postea cada 10s mientras
 *  EN_DESPACHO. No hay rate limit explicito porque el ownership ya filtra
 *  abuso; si en el futuro hay flotas grandes, agregamos token bucket.
 */
class UbicacionService(private val repo: UbicacionRepository) {

    /** El repartidor reporta su posicion actual. Valida que el pedido este
     *  EN_DESPACHO y que sea su repartidor asignado. */
    suspend fun reportar(repartidorId: UUID, orderId: UUID, lat: Double, lng: Double) {
        if (lat.isNaN() || lat < -90 || lat > 90) throw ValidationException("Latitud invalida.")
        if (lng.isNaN() || lng < -180 || lng > 180) throw ValidationException("Longitud invalida.")
        val owner = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedRepartidorId eq repartidorId) and
                (OrdersTable.status eq "EN_DESPACHO")
            }.any()
        }
        if (!owner) throw ValidationException("Este pedido no esta en despacho o no es tuyo.")
        repo.upsert(orderId, repartidorId, lat, lng)
    }

    /** El cliente consulta la posicion del repartidor para SU pedido.
     *  Devuelve null si no hay reporte todavia (repartidor recien tomo o no
     *  ha posteado). El caller (route) ya valida ownership por user_id. */
    suspend fun paraCliente(orderId: UUID): UbicacionDto? {
        val row = repo.findByOrder(orderId) ?: return null
        return UbicacionDto(
            lat = row.lat,
            lng = row.lng,
            updatedAt = row.updatedAt.toString(),
        )
    }
}
