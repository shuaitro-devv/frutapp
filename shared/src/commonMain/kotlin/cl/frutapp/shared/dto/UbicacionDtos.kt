package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Posicion del repartidor para un pedido. lat/lng como Double porque la app
 *  los maneja asi al pintar el marker (Google Maps SDK usa Double). */
@Serializable
data class UbicacionDto(
    val lat: Double,
    val lng: Double,
    /** ISO timestamp del ultimo reporte. El cliente puede mostrar "actualizado
     *  hace X seg" para que se sienta vivo. */
    val updatedAt: String,
)

/** Body que postea el repartidor desde su app. */
@Serializable
data class ReportarUbicacionRequest(
    val lat: Double,
    val lng: Double,
)
