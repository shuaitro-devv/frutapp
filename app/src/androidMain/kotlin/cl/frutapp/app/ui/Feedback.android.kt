package cl.frutapp.app.ui

import android.content.Context
import android.widget.Toast

private var appCtx: Context? = null

/** Llamar una vez al arrancar (MainActivity) para poder mostrar toasts desde common. */
fun initToast(context: Context) {
    appCtx = context.applicationContext
}

actual fun showToast(message: String) {
    appCtx?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT).show() }
}
