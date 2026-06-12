package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.AppConfigDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

/** Lee la configuración pública del backend (envío, FrutCoins, etc.). Sin auth: el
 *  endpoint solo expone keys `client_visible`. La app cachea localmente vía
 *  [cl.frutapp.app.data.ConfigStore]. */
class ConfigApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    suspend fun fetch(): AppConfigDto =
        client.get("$baseUrl/v1/config").body()
}
