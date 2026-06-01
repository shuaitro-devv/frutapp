package cl.frutapp.app.ui

import androidx.compose.ui.graphics.ImageBitmap

/** Mensaje breve provisto por la plataforma (Android: Toast). */
expect fun showToast(message: String)

/** Abre el diálogo nativo de compartir con el texto dado (Android: ACTION_SEND). */
expect fun shareText(text: String)

/**
 * Abre una URL en la app que el sistema asocie (https → navegador, tel: → marcador,
 * mailto: → mail, https://wa.me/... → WhatsApp). En Android: Intent.ACTION_VIEW.
 */
expect fun openUrl(url: String)

/**
 * Comparte una imagen (con caption opcional) usando el menú nativo de compartir.
 * En Android: escribe a cache + FileProvider + Intent.ACTION_SEND con MIME image/png.
 * @param chooserTitle texto que aparece en el header del selector del sistema (default
 *   genérico). Cada caller debería pasar uno descriptivo de SU acción.
 */
expect suspend fun shareImage(bitmap: ImageBitmap, caption: String, chooserTitle: String = "Compartir")

/** Feedback estándar para acciones aún no construidas (evita que se vean "rotas"). */
fun comingSoon() = showToast("Disponible próximamente")
