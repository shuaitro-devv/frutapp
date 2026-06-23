package cl.frutapp.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Abre el selector de imagen nativo de la plataforma (en Android: Photo Picker, sin
 * permisos en Android 13+; PickVisualMedia compatible hacia atrás). Devuelve un estado
 * observable con la imagen elegida (null si todavía no eligió o cerró sin elegir).
 *
 * Para la demo se usa en reseñas: el usuario elige una foto de su galería, la mostramos
 * inline y la guardamos en el [cl.frutapp.app.data.ResenasStore]. NO se sube al backend.
 */
expect class ImagePickerState {
    val imagen: ImageBitmap?
    /** Bytes originales de la imagen elegida (sin recortar/recomprimir). Para
     *  uploads multipart al backend (resenas). Null si todavia no se eligio. */
    val bytes: ByteArray?
    fun pick()
    fun limpiar()
}

@Composable
expect fun rememberImagePickerState(): ImagePickerState
