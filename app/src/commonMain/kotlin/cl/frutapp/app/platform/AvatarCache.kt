package cl.frutapp.app.platform

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Cache de avatares en dos niveles:
 *  - [AvatarMemoryCache]: ImageBitmap ya decodificado, vive el proceso.
 *  - [AvatarDiskCache]: bytes en filesystem del celu, sobrevive force-stop.
 *
 * La key estable es el **object key** del bucket (ej. `users/<uuid>/avatar.jpg`),
 * extraido con [objectKeyFromUrl]. La URL presignada cambia cada login porque
 * incluye X-Amz-Date y X-Amz-Signature distintas, pero el objeto detras es el
 * mismo. Cachear por object key permite que la app reuse la foto descargada
 * aunque vengan URLs nuevas.
 */
object AvatarMemoryCache {
    private val cache = mutableMapOf<String, ImageBitmap>()
    fun get(key: String): ImageBitmap? = cache[key]
    fun put(key: String, bitmap: ImageBitmap) { cache[key] = bitmap }
    fun invalidate(key: String) { cache.remove(key) }
}

/** Disk cache de bytes de imagen. Implementacion por plataforma usa cacheDir del app. */
expect object AvatarDiskCache {
    suspend fun get(key: String): ByteArray?
    suspend fun put(key: String, bytes: ByteArray)
    suspend fun invalidate(key: String)
}

/** Extrae el path estable del objeto desde una URL presignada de MinIO/S3.
 *  Ej: `https://frutapp-storage.grandline.cl/frutapp/users/X/avatar.jpg?X-Amz...`
 *      → `users/X/avatar.jpg`
 *  Devuelve null si la URL no calza con el patron esperado. */
fun objectKeyFromUrl(url: String): String? {
    val withoutQuery = url.substringBefore('?')
    // Buscamos "/users/" como ancla — todas nuestras keys empiezan con eso.
    val idx = withoutQuery.indexOf("/users/")
    if (idx < 0) return null
    val key = withoutQuery.substring(idx + 1) // saltea el "/" inicial
    return key.ifBlank { null }
}
