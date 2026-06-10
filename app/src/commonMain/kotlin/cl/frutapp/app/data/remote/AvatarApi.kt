package cl.frutapp.app.data.remote

import cl.frutapp.app.platform.AvatarDiskCache
import cl.frutapp.app.platform.AvatarMemoryCache
import cl.frutapp.app.platform.contentTypeImagen
import cl.frutapp.app.platform.objectKeyFromUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable

@Serializable
data class AvatarUploadResponse(val avatarUrl: String)

/** Sube/borra el avatar del usuario autenticado. Auth via interceptor de [ApiClient]. */
class AvatarApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    /** Sube los bytes ya comprimidos (JPEG/PNG) como multipart con campo "archivo".
     *  Devuelve la URL presignada nueva (TTL 1h) para mostrar de inmediato. */
    suspend fun upload(bytes: ByteArray): String {
        val ct = contentTypeImagen(bytes)
        val ext = if (ct == "image/png") "png" else "jpg"
        val response = client.post("$baseUrl/v1/me/avatar") {
            setBody(MultiPartFormDataContent(
                formData {
                    append(
                        key = "archivo",
                        value = bytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, ct)
                            append(HttpHeaders.ContentDisposition, "filename=\"avatar.$ext\"")
                        }
                    )
                }
            ))
        }
        val nuevaUrl = response.body<AvatarUploadResponse>().avatarUrl
        // Invalida caches y precarga la imagen nueva: el object key se mantiene
        // (users/<uuid>/avatar.jpg sobrescribe), asi que sin invalidar verias la
        // foto vieja hasta proximo cold start. Guardamos los bytes recien
        // comprimidos por el cliente — son los mismos que el server almaceno.
        objectKeyFromUrl(nuevaUrl)?.let { key ->
            AvatarMemoryCache.invalidate(key)
            runCatching { AvatarDiskCache.put(key, bytes) }
        }
        return nuevaUrl
    }

    suspend fun delete() {
        client.delete("$baseUrl/v1/me/avatar")
    }
}
