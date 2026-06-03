package cl.frutapp.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Mensaje breve provisto por la plataforma (Android: Toast). */
expect fun showToast(message: String)

/**
 * Intercepta el back fisico/gesto del sistema en la pantalla que lo declara, redirigiendolo
 * a [onBack] en vez de dejar que la plataforma haga el comportamiento default (Android: pop).
 * Necesario en flujos donde 'volver' no es 'pop simple' sino una transicion semantica
 * (limpiar estado, replaceAll a otra pantalla). En Android usa androidx.activity.compose.BackHandler.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

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
