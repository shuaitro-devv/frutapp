package cl.frutapp.app.ui

import androidx.compose.ui.graphics.ImageBitmap

/** Mensaje breve provisto por la plataforma (Android: Toast). */
expect fun showToast(message: String)

/** Abre el diálogo nativo de compartir con el texto dado (Android: ACTION_SEND). */
expect fun shareText(text: String)

/**
 * Comparte una imagen (con caption opcional) usando el menú nativo de compartir.
 * En Android: escribe a cache + FileProvider + Intent.ACTION_SEND con MIME image/png.
 */
expect suspend fun shareImage(bitmap: ImageBitmap, caption: String)

/** Feedback estándar para acciones aún no construidas (evita que se vean "rotas"). */
fun comingSoon() = showToast("Disponible próximamente")
