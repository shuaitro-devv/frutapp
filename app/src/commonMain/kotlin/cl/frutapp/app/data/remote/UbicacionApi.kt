package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.ReportarUbicacionRequest
import cl.frutapp.shared.dto.UbicacionDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/** Endpoints de tracking de ubicacion del repartidor. */
class UbicacionApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    /** Repartidor reporta su posicion para un pedido EN_DESPACHO. 204 No
     *  Content si OK. Lanza ValidationException si el pedido no es suyo. */
    suspend fun reportar(orderId: String, lat: Double, lng: Double) {
        client.post("$baseUrl/v1/staff/dispatches/$orderId/ubicacion") {
            contentType(ContentType.Application.Json)
            setBody(ReportarUbicacionRequest(lat = lat, lng = lng))
        }
    }

    /** Cliente consulta la ubicacion del repartidor de su pedido. Devuelve
     *  null si el repartidor aun no reporto (204 del backend) o el pedido
     *  no tiene tracking todavia. */
    suspend fun paraPedido(orderId: String): UbicacionDto? {
        val resp: HttpResponse = client.get("$baseUrl/v1/orders/$orderId/ubicacion")
        if (resp.status == HttpStatusCode.NoContent) return null
        return resp.body()
    }
}
