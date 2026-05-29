package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.FrutCoinsBalanceDto
import cl.frutapp.shared.dto.OrderDto
import cl.frutapp.shared.dto.OrderSummaryDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Endpoints de pedidos y FrutCoins (protegidos por JWT; el Bearer lo pone ApiClient). */
class OrderApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    suspend fun create(req: CreateOrderRequest): OrderDto =
        client.post("$baseUrl/v1/orders") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun list(): List<OrderSummaryDto> =
        client.get("$baseUrl/v1/orders").body()

    suspend fun get(id: String): OrderDto =
        client.get("$baseUrl/v1/orders/$id").body()

    suspend fun frutCoins(): FrutCoinsBalanceDto =
        client.get("$baseUrl/v1/frutcoins").body()
}
