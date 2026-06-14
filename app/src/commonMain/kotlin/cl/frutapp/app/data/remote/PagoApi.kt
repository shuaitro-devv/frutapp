package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.EstadoPagoResponse
import cl.frutapp.shared.dto.IniciarPagoRequest
import cl.frutapp.shared.dto.IniciarPagoResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Endpoints de pago Webpay. Auth via interceptor de [ApiClient] (Bearer JWT). */
class PagoApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    /** El cliente inicia el flujo. Devuelve token + url para abrir la WebView
     *  con form-POST. Lanza si el pedido no esta en CREADO o hay tx reciente. */
    suspend fun iniciar(orderId: String): IniciarPagoResponse =
        client.post("$baseUrl/v1/pagos/iniciar") {
            contentType(ContentType.Application.Json)
            setBody(IniciarPagoRequest(orderId = orderId))
        }.body()

    /** Consulta el estado de una transaccion (la app llama al cerrar la
     *  WebView). Distingue INICIADA (aun confirmando) de RECHAZADA/PAGADA/ERROR. */
    suspend fun estado(token: String): EstadoPagoResponse =
        client.get("$baseUrl/v1/pagos/estado/$token").body()
}
