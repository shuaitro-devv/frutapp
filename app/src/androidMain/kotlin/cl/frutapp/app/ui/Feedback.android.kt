package cl.frutapp.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private var appCtx: Context? = null

/** Llamar una vez al arrancar (MainActivity) para poder mostrar toasts/compartir desde common. */
fun initToast(context: Context) {
    appCtx = context.applicationContext
}

actual fun showToast(message: String) {
    appCtx?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
}

actual fun shareText(text: String) {
    val ctx = appCtx ?: return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, "Compartir").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // requerido al lanzar desde un Context de aplicación
    }
    ctx.startActivity(chooser)
}

actual suspend fun shareImage(bitmap: ImageBitmap, caption: String, chooserTitle: String) {
    val ctx = appCtx ?: return
    // Escribir el PNG a cache/shared en un hilo IO; FileProvider lo expone con URI seguro.
    val uri = withContext(Dispatchers.IO) {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "frutapp_share_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(chooser)
}
