package cl.frutapp.app.platform

import androidx.compose.runtime.Composable

/**
 * Selector de imágenes del device. Devuelve los bytes ya comprimidos a JPEG
 * (max 1280px lado mayor, calidad 80%) — la app no se preocupa por el
 * tamaño original; el cliente comprime antes de subir.
 *
 * Soporta:
 *  - [galeria]: abre el picker visual del sistema.
 *  - [camara]: abre la camara para sacar una foto (Android: ActivityResultContracts.TakePicture + FileProvider).
 *
 * Patrón base replicado de polizapp.
 */
expect class SelectorImagenes {
    fun galeria()
    fun camara()
}

/** Crea un selector ligado al ciclo de vida de la pantalla; [onImagen] recibe los bytes elegidos. */
@Composable
expect fun rememberSelectorImagenes(onImagen: (ByteArray) -> Unit): SelectorImagenes
