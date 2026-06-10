package cl.frutapp.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes

/** Decodifica bytes de imagen a [ImageBitmap]. Implementación por plataforma (Android: BitmapFactory). */
expect fun decodeImagen(bytes: ByteArray): ImageBitmap?

/**
 * Descarga de imágenes por URL. Cliente sin auth a propósito: las URLs vienen
 * presignadas (S3 SigV4), no llevan Bearer.
 */
object Imagenes {
    private val client = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 10_000 }
    }

    suspend fun descargar(url: String): ByteArray = client.get(url).readBytes()
}

/** Detecta el content-type de una imagen por su firma de bytes (magic number).
 *  Default JPEG si no matchea, ya que el cliente comprime todo a JPEG en upload. */
fun contentTypeImagen(bytes: ByteArray): String = when {
    bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
    bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
    bytes.size >= 12 && bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() -> "image/webp"
    else -> "image/jpeg"
}
