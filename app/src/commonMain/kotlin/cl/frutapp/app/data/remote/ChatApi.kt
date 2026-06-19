package cl.frutapp.app.data.remote

import cl.frutapp.app.platform.contentTypeImagen
import cl.frutapp.shared.dto.ChatMensajeDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * Endpoints REST del chat in-app por pedido. Auth via interceptor de
 * [ApiClient] (Bearer JWT). El push realtime de mensajes nuevos viene por
 * WebSocket (ver [ChatWsClient]).
 *
 * El POST usa multipart (mismo patron que [StaffEvidenceApi]) para soportar
 * imagen adjunta opcional sin tener un segundo endpoint.
 */
class ChatApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    /** Envia un mensaje. Para staff, [destinatarioRol] se ignora (siempre va
     *  al cliente del pedido). Para cliente, debe ser "picker" o "repartidor".
     *  Si [imagen] != null, va como archivo adjunto JPEG/PNG; cuerpo puede ser
     *  vacio en ese caso (mensaje solo-imagen). */
    suspend fun enviar(
        orderId: String,
        destinatarioRol: String,
        cuerpo: String,
        imagen: ByteArray? = null,
    ): ChatMensajeDto =
        client.post("$baseUrl/v1/orders/$orderId/chat") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("destinatarioRol", destinatarioRol)
                    append("cuerpo", cuerpo)
                    if (imagen != null && imagen.isNotEmpty()) {
                        val ct = contentTypeImagen(imagen)
                        val ext = if (ct == "image/png") "png" else "jpg"
                        append(
                            key = "archivo",
                            value = imagen,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, ct)
                                append(HttpHeaders.ContentDisposition, "filename=\"chat.$ext\"")
                            }
                        )
                    }
                }
            ))
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
