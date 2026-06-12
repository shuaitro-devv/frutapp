package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.AjusteResumenDto
import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.FrutCoinsBalanceDto
import cl.frutapp.shared.dto.OrderDto
import cl.frutapp.shared.dto.OrderSummaryDto
import cl.frutapp.shared.dto.PricingChangedDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/** El backend rechazó la orden porque el precio del envío cambió mientras el cliente
 *  armaba el carrito. Lleva los nuevos valores para que la UI pueda mostrar al
 *  cliente el delta y pedirle confirmación sin un round trip extra. */
class PricingChangedAppException(
    val mensaje: String,
    val nuevoCostoEnvio: Int,
    val nuevoEnvioGratisDesde: Int
) : RuntimeException(mensaje)

/** Endpoints de pedidos y FrutCoins (protegidos por JWT; el Bearer lo pone ApiClient). */
class OrderApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    /** Crea la orden. Si el backend responde 409 con código `pricing_changed`,
     *  lanza [PricingChangedAppException] con los nuevos valores para que el caller
     *  pueda mostrar un diálogo amigable en vez del error genérico. */
    suspend fun create(req: CreateOrderRequest): OrderDto = try {
        client.post("$baseUrl/v1/orders") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
    } catch (e: ClientRequestException) {
        if (e.response.status == HttpStatusCode.Conflict) {
            val dto = runCatching { e.response.body<PricingChangedDto>() }.getOrNull()
            if (dto != null) throw PricingChangedAppException(
                mensaje = dto.mensaje,
                nuevoCostoEnvio = dto.nuevoCostoEnvio,
                nuevoEnvioGratisDesde = dto.nuevoEnvioGratisDesde
            )
        }
        throw e
    }

    suspend fun list(): List<OrderSummaryDto> =
        client.get("$baseUrl/v1/orders").body()

    suspend fun get(id: String): OrderDto =
        client.get("$baseUrl/v1/orders/$id").body()

    suspend fun frutCoins(): FrutCoinsBalanceDto =
        client.get("$baseUrl/v1/frutcoins").body()

    /** El cliente abre la pantalla "Hay un ajuste de peso": traemos delta + total ajustado. */
    suspend fun getAjuste(orderId: String): AjusteResumenDto =
        client.get("$baseUrl/v1/orders/$orderId/ajuste").body()

    /** El cliente aprueba el ajuste: el pedido pasa a STOCK_CONFIRMADO con el total nuevo. */
    suspend fun aprobarAjuste(orderId: String): OrderDto =
        client.post("$baseUrl/v1/orders/$orderId/aprobar-ajuste").body()

    /** El cliente rechaza items con delta grande: se descartan, el resto sigue. */
    suspend fun rechazarAjuste(orderId: String): OrderDto =
        client.post("$baseUrl/v1/orders/$orderId/rechazar-ajuste").body()
}
