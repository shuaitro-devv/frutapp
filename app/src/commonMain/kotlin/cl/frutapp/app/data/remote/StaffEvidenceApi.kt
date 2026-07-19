package cl.frutapp.app.data.remote

import cl.frutapp.app.platform.contentTypeImagen
import cl.frutapp.shared.dto.OrderItemEvidenceDto
import cl.frutapp.shared.dto.UploadEvidenceResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

/**
 * Endpoints de evidencia visual:
 *  - POST staff: el picker sube foto + comentario opcional al completar item.
 *  - GET cliente: lista todas las evidencias del pedido para el tracking.
 *
 * Auth via interceptor de [ApiClient] (Bearer JWT).
 */
class StaffEvidenceApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    /** Picker sube una foto al item del pedido. Multipart con FileItem "archivo"
     *  + FormItem opcional "comentario". Devuelve la evidencia recien creada
     *  (incluye URL presignada). */
    suspend fun subir(orderId: String, itemId: String, bytes: ByteArray, comentario: String?): OrderItemEvidenceDto {
        val ct = contentTypeImagen(bytes)
        val ext = if (ct == "image/png") "png" else "jpg"
        val response = client.post("$baseUrl/v1/staff/orders/$orderId/items/$itemId/evidence") {
            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        key = "archivo",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, ct)
                            append(HttpHeaders.ContentDisposition, "filename=\"evidencia.$ext\"")
                        }
                    )
                    if (!comentario.isNullOrBlank()) {
                        append("comentario", comentario)
                    }
                }
            ))
        }
        return response.body<UploadEvidenceResponse>().evidencia
    }

    /** Repartidor borra una foto de entrega que subio (para reemplazarla). El
     *  backend valida ownership + estado EN_DESPACHO antes de borrar. 204 sin
     *  body si ok; 404 si la evidencia ya no existe (idempotente). */
    suspend fun eliminarEntrega(orderId: String, evidenceId: String) {
        client.delete("$baseUrl/v1/staff/dispatches/$orderId/evidence/$evidenceId")
    }

    /** Repartidor sube la firma del receptor (PNG con los trazos capturados en
     *  el canvas). Endpoint distinto de la foto para que el cliente en su
     *  tracking la muestre bajo una card 'Firma del receptor' separada. */
    suspend fun subirFirma(orderId: String, pngBytes: ByteArray): OrderItemEvidenceDto {
        val response = client.post("$baseUrl/v1/staff/dispatches/$orderId/signature") {
            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        key = "archivo",
                        value = pngBytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/png")
                            append(HttpHeaders.ContentDisposition, "filename=\"firma.png\"")
                        }
                    )
                }
            ))
        }
        return response.body<UploadEvidenceResponse>().evidencia
    }

    /** Repartidor sube UNA foto del paquete entregado. La evidencia queda
     *  ligada al pedido completo (orderItemId = null en el DTO de respuesta). */
    suspend fun subirEntrega(orderId: String, bytes: ByteArray, comentario: String?): OrderItemEvidenceDto {
        val ct = contentTypeImagen(bytes)
        val ext = if (ct == "image/png") "png" else "jpg"
        val response = client.post("$baseUrl/v1/staff/dispatches/$orderId/evidence") {
            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        key = "archivo",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, ct)
                            append(HttpHeaders.ContentDisposition, "filename=\"entrega.$ext\"")
                        }
                    )
                    if (!comentario.isNullOrBlank()) {
                        append("comentario", comentario)
                    }
                }
            ))
        }
        return response.body<UploadEvidenceResponse>().evidencia
    }
}

/** Endpoint cliente: lista evidencias del propio pedido. Va en otro client
 *  para mantener separados los endpoints staff (gated por order:pick) de los
 *  del cliente. */
class OrderEvidenceApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    suspend fun listar(orderId: String): List<OrderItemEvidenceDto> =
        client.get("$baseUrl/v1/orders/$orderId/evidence").body()
}
