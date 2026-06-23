package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.ActualizarCanastaRequest
import cl.frutapp.shared.dto.CanastaDto
import cl.frutapp.shared.dto.CrearCanastaRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Endpoints REST de canastas guardadas. JWT requerido (todas). */
class BasketApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    suspend fun listar(): List<CanastaDto> =
        client.get("$baseUrl/v1/baskets").body()

    suspend fun crear(req: CrearCanastaRequest): CanastaDto =
        client.post("$baseUrl/v1/baskets") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun cargar(id: String): CanastaDto =
        client.get("$baseUrl/v1/baskets/$id").body()

    suspend fun actualizar(id: String, req: ActualizarCanastaRequest): CanastaDto =
        client.put("$baseUrl/v1/baskets/$id") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun eliminar(id: String) {
        client.delete("$baseUrl/v1/baskets/$id")
    }
}
