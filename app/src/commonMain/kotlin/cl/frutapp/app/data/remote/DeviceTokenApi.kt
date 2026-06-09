package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.DeleteDeviceTokenRequest
import cl.frutapp.shared.dto.RegisterDeviceTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Wrappers de POST/DELETE /v1/device/token. Auth via interceptor del [ApiClient]. */
class DeviceTokenApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    suspend fun register(req: RegisterDeviceTokenRequest) {
        client.post("$baseUrl/v1/device/token") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
    }

    suspend fun delete(fcmToken: String) {
        client.delete("$baseUrl/v1/device/token") {
            contentType(ContentType.Application.Json)
            setBody(DeleteDeviceTokenRequest(fcmToken = fcmToken))
        }
    }
}
