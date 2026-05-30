package cl.frutapp.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Abstracción para capturar el subtree visible de un Composable a [ImageBitmap].
 * En Android usa `Picture` + `drawWithCache`. En otras plataformas se implementará luego.
 *
 * Uso típico:
 * ```
 * val capture = rememberCaptureLayer()
 * Box(modifier = capture.modifier) { ShareCard(...) }
 * // En un coroutine: val bmp = capture.toImageBitmap()
 * ```
 */
expect class CaptureLayer {
    /** Modifier que se aplica al Composable a capturar. */
    val modifier: Modifier

    /** Captura el contenido renderizado a un [ImageBitmap]. Suspend porque hace IO. */
    suspend fun toImageBitmap(): ImageBitmap
}

/** Recuerda un [CaptureLayer] para capturar el Composable wrapeado por su [modifier]. */
@Composable
expect fun rememberCaptureLayer(): CaptureLayer
