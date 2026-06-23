package cl.frutapp.app.data.remote

import cl.frutapp.app.platform.contentTypeImagen
import cl.frutapp.shared.dto.ResenaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

/**
 * Endpoints REST de resenas de producto. Auth via interceptor de [ApiClient]
 * (Bearer JWT) en los endpoints autenticados; GET listar es publico (lo
 * usa el detalle del producto sin sesion).
 *
 * El POST es multipart (mismo patron que chat/evidencia) para soportar
 * imagen adjunta opcional. Sin imagen: campos estrellas+texto+removerImagen.
 * Con imagen: agrega FileItem `archivo`. Para borrar la foto sin tocar el
 * texto, removerImagen=true sin archivo.
 */
class ReviewApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    /** Upsert mi resena. [imagen] != null sube foto nueva; [removerImagen]=true
     *  borra la actual. Si ambos son default (null/false), la foto previa se
     *  mantiene. */
    suspend fun guardar(
        productoId: String,
        estrellas: Int,
        texto: String,
        imagen: ByteArray? = null,
        removerImagen: Boolean = false,
    ): ResenaDto =
        client.post("$baseUrl/v1/products/$productoId/reviews") {
            setBody(MultiPartFormDataContent(
                formData {
                    append("estrellas", estrellas.toString())
                    append("texto", texto)
                    if (removerImagen) append("removerImagen", "true")
                    if (imagen != null && imagen.isNotEmpty()) {
                        val ct = contentTypeImagen(imagen)
                        val ext = if (ct == "image/png") "png" else "jpg"
                        append(
                            key = "archivo",
                            value = imagen,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, ct)
                                append(HttpHeaders.ContentDisposition, "filename=\"resena.$ext\"")
                            }
                        )
                    }
                }
            ))
        }.body()

    /** Listar resenas de un producto, las mas nuevas arriba. Publico. */
    suspend fun listar(productoId: String): List<ResenaDto> =
        client.get("$baseUrl/v1/products/$productoId/reviews").body()

    /** Mi resena para este producto, o null si todavia no la escribi. */
    suspend fun mia(productoId: String): ResenaDto? = try {
        client.get("$baseUrl/v1/products/$productoId/reviews/mine").body<ResenaDto>()
    } catch (e: ResponseException) {
        if (e.response.status == HttpStatusCode.NotFound) null else throw e
    }
}
