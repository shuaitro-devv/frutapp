package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.StaffOrderDetailDto
import cl.frutapp.shared.dto.StaffOrderSummaryDto
import cl.frutapp.shared.dto.StaffTakeResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

/**
 * Endpoints del staff (picker / repartidor). Protegidos por JWT con permisos
 * order:pick / order:confirm_stock. El Bearer lo pone ApiClient.
 *
 * Soporte para el "Modelo C hibrido" del Plan Sprint: la cola filtra por el
 * `home_location_id` del usuario logueado, asi un picker en Lo Valledor solo
 * ve pedidos de su location y nunca compite con otro feriante asociado.
 */
class StaffOrderApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    /** Pedidos libres en la cola de mi location: status CREADO/PAGADO sin asignar +
     *  EN_PICKING con assigned_at viejo (auto-rescate). */
    suspend fun cola(): List<StaffOrderSummaryDto> =
        client.get("$baseUrl/v1/staff/orders") {
            parameter("status", "cola")
        }.body()

    /** Pedidos que YO tome y aun no completo. */
    suspend fun enCurso(): List<StaffOrderSummaryDto> =
        client.get("$baseUrl/v1/staff/orders") {
            parameter("status", "en_curso")
        }.body()

    /** Detalle del pedido para el picker: cabecera + items reales (lo que pidio el
     *  cliente con sus pesos/cantidades, no el mockup). */
    suspend fun detalle(orderId: String): StaffOrderDetailDto =
        client.get("$baseUrl/v1/staff/orders/$orderId").body()

    /** UPDATE atomico: gano el primero. Si otro picker llego antes, devuelve
     *  ok=false con motivo="ya_tomado_o_no_disponible". */
    suspend fun take(orderId: String): StaffTakeResult =
        client.post("$baseUrl/v1/staff/orders/$orderId/take").body()

    /** Devolver el pedido a la cola libre (se equivoco al tomar). */
    suspend fun release(orderId: String) {
        client.post("$baseUrl/v1/staff/orders/$orderId/release")
    }

    /** Marcar como STOCK_CONFIRMADO — listo para que el repartidor lo retire. */
    suspend fun complete(orderId: String) {
        client.post("$baseUrl/v1/staff/orders/$orderId/complete")
    }
}
