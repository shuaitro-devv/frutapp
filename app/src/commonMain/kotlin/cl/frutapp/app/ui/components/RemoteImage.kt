package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cl.frutapp.app.platform.Imagenes
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Descarga + decodifica una imagen desde URL (presignada o publica) y la
 * muestra. Cache en memoria por URL para no redescargar al re-componer o al
 * revisitar la pantalla durante la misma sesion.
 *
 * Usado por la card de resena (foto adjunta) y por el visor fullscreen. El
 * chat tiene su propia version (legado) que se puede migrar despues.
 */
@Composable
fun RemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = RemoteImageCache.get(url), url) {
        if (value != null) return@produceState
        runCatching {
            val bytes = Imagenes.descargar(url)
            decodeImagen(bytes)
        }
            .onSuccess { img ->
                if (img != null) {
                    RemoteImageCache.put(url, img)
                    value = img
                }
            }
    }
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        val img = bitmap
        if (img == null) {
            CircularProgressIndicator(
                color = FrutAppColors.Brand400,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Image(
                bitmap = img,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Cache compartido de bitmaps decodificados por URL. Vive el proceso. */
object RemoteImageCache {
    private val cache = mutableMapOf<String, ImageBitmap>()
    fun get(url: String): ImageBitmap? = cache[url]
    fun put(url: String, bitmap: ImageBitmap) { cache[url] = bitmap }
    fun invalidate(url: String) { cache.remove(url) }
}
