package cl.frutapp.app.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.File

actual class SelectorImagenes internal constructor(
    private val onGaleria: () -> Unit,
    private val onCamara: () -> Unit,
) {
    actual fun galeria() = onGaleria()
    actual fun camara() = onCamara()
}

@Composable
actual fun rememberSelectorImagenes(onImagen: (ByteArray) -> Unit): SelectorImagenes {
    val context = LocalContext.current
    val launcherGaleria = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { leerBytes(context, it)?.let(onImagen) } }
    // ActivityResultContracts.TakePicture exige una Uri donde la camara escribe
    // la foto a pixel-full. Usamos FileProvider sobre cache para que la app no
    // necesite permiso WRITE_EXTERNAL_STORAGE. El archivo lo creamos al lanzar
    // y lo leemos al recibir el callback.
    //
    // ultimoCamaraUri VA en remember { mutableStateOf } — sin eso, cada
    // recomposicion crea una var local nueva y el callback termina leyendo una
    // caja distinta a la que escribio onCamara. La foto salia bien pero
    // onImagen jamas se disparaba (bug reportado por el usuario en el celu:
    // "sigue sin verse la preview").
    val ultimoCamaraUri = remember { mutableStateOf<Uri?>(null) }
    val launcherCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        if (ok) {
            ultimoCamaraUri.value?.let { uri ->
                leerBytes(context, uri)?.let(onImagen)
            }
        }
    }
    return remember {
        SelectorImagenes(
            onGaleria = {
                launcherGaleria.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onCamara = {
                val (file, uri) = crearArchivoCamara(context)
                ultimoCamaraUri.value = uri
                launcherCamara.launch(uri)
                // file queda en cache; al proximo `cache` cleanup el sistema lo borra.
            },
        )
    }
}

/** Crea un File temporal en el cache de la app y devuelve su content:// Uri
 *  via FileProvider para que la camara pueda escribir ahi. */
private fun crearArchivoCamara(context: Context): Pair<File, Uri> {
    val dir = File(context.cacheDir, "camara").apply { mkdirs() }
    val file = File(dir, "captura_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    return file to uri
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
