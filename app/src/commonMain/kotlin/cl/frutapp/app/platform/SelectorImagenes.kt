package cl.frutapp.app.platform

import androidx.compose.runtime.Composable

/**
 * Selector de imágenes desde la galería del device. Devuelve los bytes ya
 * comprimidos a JPEG (max 1280px lado mayor, calidad 80%) — la app no se
 * preocupa por el tamaño original; el cliente comprime antes de subir.
 *
 * Patrón replicado de polizapp. Para sumar cámara, expandir `actual` con
 * `TakePicture` + FileProvider. Ver `docs/00-tecnico/Standard_Subida_Imagenes.md`.
 */
expect class SelectorImagenes {
    fun galeria()
}

/** Crea un selector ligado al ciclo de vida de la pantalla; [onImagen] recibe los bytes elegidos. */
@Composable
expect fun rememberSelectorImagenes(onImagen: (ByteArray) -> Unit): SelectorImagenes
