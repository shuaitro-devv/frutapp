package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.CategoryDto
import cl.frutapp.shared.dto.ProductDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CatalogApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    suspend fun categories(): List<CategoryDto> =
        client.get("$baseUrl/v1/categories").body()

    suspend fun products(category: String? = null, query: String? = null): List<ProductDto> =
        client.get("$baseUrl/v1/products") {
            category?.let { parameter("category", it) }
            query?.let { parameter("q", it) }
        }.body()

    suspend fun product(id: String): ProductDto =
        client.get("$baseUrl/v1/products/$id").body()
}
