package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.NotificationsResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post

/** Cliente del inbox de notificaciones. Auth via interceptor del [ApiClient]. */
class NotificationApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    suspend fun list(): NotificationsResponse =
        client.get("$baseUrl/v1/notifications").body()

    suspend fun markRead(id: String) {
        client.post("$baseUrl/v1/notifications/$id/read")
    }

    suspend fun markAllRead() {
        client.post("$baseUrl/v1/notifications/read-all")
    }
}
