package cl.frutapp.app.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

actual class SelectorImagenes internal constructor(
    private val onGaleria: () -> Unit,
) {
    actual fun galeria() = onGaleria()
}

@Composable
actual fun rememberSelectorImagenes(onImagen: (ByteArray) -> Unit): SelectorImagenes {
    val context = LocalContext.current
    val launcherGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { leerBytes(context, it)?.let(onImagen) } }
    return remember {
        SelectorImagenes(
            onGaleria = {
                launcherGaleria.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

private fun leerBytes(context: Context, uri: Uri): ByteArray? {
    val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return comprimirJpeg(raw)
}

/** Baja la imagen a máx ~1280 px de lado y la recomprime a JPEG: subida mucho más liviana/rápida. */
private fun comprimirJpeg(bytes: ByteArray, maxLado: Int = 1280, calidad: Int = 80): ByteArray {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        val mayor = maxOf(bounds.outWidth, bounds.outHeight)
        while (mayor / sample > maxLado) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return bytes
        ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, calidad, out)
            out.toByteArray()
        }
    } catch (e: Throwable) {
        bytes
    }
}
