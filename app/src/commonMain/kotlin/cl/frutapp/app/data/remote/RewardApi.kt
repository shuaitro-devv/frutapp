package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.CanjearFrutCoinsRequest
import cl.frutapp.shared.dto.CuponDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Endpoints REST de canje de FrutCoins. Auth via interceptor de [ApiClient].
 *
 *  - canjear(): debita FrutCoins del usuario y crea cupon. Idempotente: si
 *    repetis el POST con la misma [idempotencyKey], el backend devuelve el
 *    mismo cupon sin debitar de nuevo.
 *  - listarCupones(): historial de cupones del usuario.
 *  - usar(): marca cupon como USADO (futuro: lo invoca el checkout cuando
 *    se aplica el descuento).
 */
class RewardApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    suspend fun canjear(monto: Int, recompensa: String, idempotencyKey: String): CuponDto =
        client.post("$baseUrl/v1/frutcoins/redeem") {
            contentType(ContentType.Application.Json)
            setBody(CanjearFrutCoinsRequest(monto = monto, recompensa = recompensa, idempotencyKey = idempotencyKey))
        }.body()

    suspend fun listarCupones(): List<CuponDto> =
        client.get("$baseUrl/v1/frutcoins/cupones").body()

    suspend fun usar(cuponId: String) {
        client.post("$baseUrl/v1/frutcoins/cupones/$cuponId/usar")
    }
}
