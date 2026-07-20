package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.ConfirmarEntregaRequest
import cl.frutapp.shared.dto.StaffDispatchDetailDto
import cl.frutapp.shared.dto.StaffDispatchSummaryDto
import cl.frutapp.shared.dto.StaffTakeResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Endpoints del repartidor (Nivel 3). Protegidos por JWT con permisos
 * order:dispatch / order:deliver. El Bearer lo pone ApiClient.
 *
 * Mismo "Modelo C híbrido" que el picker: el repartidor solo ve pedidos de
 * su `home_location`. Cuando se sume Sofruco como location aparte, los
 * repartidores asignados ahi ven solo sus pedidos.
 */
class StaffDispatchApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    /** Cola libre: pedidos STOCK_CONFIRMADO listos para retiro + EN_DESPACHO
     *  con assigned_at > 60min (auto-rescate). */
    suspend fun cola(): List<StaffDispatchSummaryDto> =
        client.get("$baseUrl/v1/staff/orders/dispatch") {
            parameter("status", "cola")
        }.body()

    /** Mis despachos EN_DESPACHO. */
    suspend fun enRuta(): List<StaffDispatchSummaryDto> =
        client.get("$baseUrl/v1/staff/orders/dispatch") {
            parameter("status", "en_ruta")
        }.body()

    /** Tab "Entregados hoy": despachos que YO lleve a destino en las ultimas 24h. */
    suspend fun entregadosHoy(): List<StaffDispatchSummaryDto> =
        client.get("$baseUrl/v1/staff/orders/dispatch") {
            parameter("status", "entregados_hoy")
        }.body()

    /** Detalle del despacho con direccion + telefono del cliente + items. */
    suspend fun detalle(orderId: String): StaffDispatchDetailDto =
        client.get("$baseUrl/v1/staff/orders/dispatch/$orderId").body()

    /** Tomar despacho (UPDATE atomico). Si otro repartidor llego antes, 409. */
    suspend fun take(orderId: String): StaffTakeResult =
        client.post("$baseUrl/v1/staff/orders/dispatch/$orderId/take").body()

    /** Marcar como ENTREGADO. El [codigo] de 4 digitos lo dice el cliente
     *  cara a cara al repartidor; el backend valida match con el delivery_code
     *  guardado al EN_DESPACHO. Si no coincide → ValidationException con
     *  mensaje "Codigo incorrecto" (el repartidor reintenta sin perder estado). */
    suspend fun delivered(orderId: String, codigo: String) {
        client.post("$baseUrl/v1/staff/orders/dispatch/$orderId/delivered") {
            contentType(ContentType.Application.Json)
            setBody(ConfirmarEntregaRequest(codigo = codigo))
        }
    }

    /** Pausa o reanuda el despacho. Un solo endpoint toggle: pausar=true con
     *  reason, pausar=false para reanudar (reason ignorado). */
    suspend fun togglePause(orderId: String, pausar: Boolean, reason: String? = null) {
        client.post("$baseUrl/v1/staff/orders/dispatch/$orderId/pause") {
            contentType(ContentType.Application.Json)
            setBody(cl.frutapp.shared.dto.PausarDespachoRequest(pausar = pausar, reason = reason))
        }
    }
}
