package cl.frutapp.app.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Disk cache real para Android: bytes JPEG dentro de `cacheDir/avatars/`.
 *
 * `cacheDir` es el directorio que el sistema operativo puede limpiar si necesita
 * espacio — perfecto para imagenes (si se borra, se vuelve a bajar). El nombre
 * del archivo es la object key con `/` reemplazado por `_` para evitar paths
 * anidados.
 *
 * Hay que llamar [init] una vez al arrancar (MainActivity) para colgar el
 * applicationContext. Sin init, los get/put son no-op silenciosos para que el
 * app siga funcionando como antes (descarga cada vez).
 */
actual object AvatarDiskCache {
    private var dir: File? = null

    fun init(context: Context) {
        if (dir != null) return
        val cacheDir = context.applicationContext.cacheDir
        dir = File(cacheDir, "avatars").apply { mkdirs() }
    }

    private fun fileFor(key: String): File? {
        val d = dir ?: return null
        val safe = key.replace('/', '_').replace('\\', '_')
        return File(d, safe)
    }

    actual suspend fun get(key: String): ByteArray? = withContext(Dispatchers.IO) {
        val f = fileFor(key) ?: return@withContext null
        if (f.exists() && f.length() > 0) runCatching { f.readBytes() }.getOrNull() else null
    }

    actual suspend fun put(key: String, bytes: ByteArray) {
        withContext(Dispatchers.IO) {
            val f = fileFor(key) ?: return@withContext
            runCatching { f.writeBytes(bytes) }
        }
    }

    actual suspend fun invalidate(key: String) {
        withContext(Dispatchers.IO) {
            fileFor(key)?.let { runCatching { it.delete() } }
        }
    }
}
