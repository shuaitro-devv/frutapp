package cl.frutapp.app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast

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
    val chooser = Intent.createChooser(send, "Invitar a un amigo").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // requerido al lanzar desde un Context de aplicación
    }
    ctx.startActivity(chooser)
}
