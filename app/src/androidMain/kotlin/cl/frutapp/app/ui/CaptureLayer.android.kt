package cl.frutapp.app.ui

import android.graphics.Bitmap
import android.graphics.Picture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación Android usando `android.graphics.Picture`: graba las operaciones de dibujo
 * mientras el Composable se renderiza, después las reproduce en un Bitmap del tamaño grabado.
 */
actual class CaptureLayer(private val picture: Picture) {
    actual val modifier: Modifier = Modifier.drawWithCache {
        val w = size.width.toInt().coerceAtLeast(1)
        val h = size.height.toInt().coerceAtLeast(1)
        onDrawWithContent {
            // Graba en la Picture, luego también dibuja en pantalla para que el usuario
            // vea el preview vivo (y se conserve la grabación para captura posterior).
            val pictureCanvas = Canvas(picture.beginRecording(w, h))
            draw(this, layoutDirection, pictureCanvas, size) {
                this@onDrawWithContent.drawContent()
            }
            picture.endRecording()
            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawPicture(picture) }
        }
    }

    actual suspend fun toImageBitmap(): ImageBitmap = withContext(Dispatchers.Default) {
        val bmp = Bitmap.createBitmap(
            picture.width.coerceAtLeast(1),
            picture.height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        android.graphics.Canvas(bmp).drawPicture(picture)
        bmp.asImageBitmap()
    }
}

@Composable
actual fun rememberCaptureLayer(): CaptureLayer = remember { CaptureLayer(Picture()) }
