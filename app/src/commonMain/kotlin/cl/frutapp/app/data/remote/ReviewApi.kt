package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.CrearResenaRequest
import cl.frutapp.shared.dto.ResenaDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * Endpoints REST de resenas de producto. Auth via interceptor de [ApiClient]
 * (Bearer JWT) en los endpoints autenticados; GET listar es publico (lo
 * usa el detalle del producto sin sesion).
 */
class ReviewApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    /** Upsert mi resena para el producto. Devuelve el dto persistido (con id
     *  estable y autorNombre resuelto). */
    suspend fun guardar(productoId: String, estrellas: Int, texto: String): ResenaDto =
        client.post("$baseUrl/v1/products/$productoId/reviews") {
            contentType(ContentType.Application.Json)
            setBody(CrearResenaRequest(estrellas = estrellas, texto = texto))
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
