package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.ChatMensajeDto
import cl.frutapp.shared.dto.EnviarMensajeRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Endpoints REST del chat in-app por pedido. Auth via interceptor de
 * [ApiClient] (Bearer JWT). El push realtime de mensajes nuevos viene por
 * WebSocket (ver [ChatWsClient]).
 */
class ChatApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    /** Envia un mensaje. Para staff, [destinatarioRol] se ignora (siempre va
     *  al cliente del pedido). Para cliente, debe ser "picker" o "repartidor". */
    suspend fun enviar(orderId: String, destinatarioRol: String, cuerpo: String): ChatMensajeDto =
        client.post("$baseUrl/v1/orders/$orderId/chat") {
            contentType(ContentType.Application.Json)
            setBody(EnviarMensajeRequest(destinatarioRol = destinatarioRol, cuerpo = cuerpo))
        }.body()

    /** Historial cronologico ASC. Si [desde] != null (ISO 8601), devuelve
     *  solo los posteriores — para fallback cuando el WS no se conecta. */
    suspend fun historial(orderId: String, desde: String? = null): List<ChatMensajeDto> =
        client.get("$baseUrl/v1/orders/$orderId/chat") {
            if (desde != null) parameter("desde", desde)
        }.body()

    /** El destinatario marca todos sus mensajes como leidos. */
    suspend fun marcarLeidos(orderId: String) {
        client.post("$baseUrl/v1/orders/$orderId/chat/leer")
    }
}
