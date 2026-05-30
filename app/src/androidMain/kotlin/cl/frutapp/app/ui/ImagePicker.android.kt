package cl.frutapp.app.ui

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

actual class ImagePickerState internal constructor(
    private val imagenState: MutableState<ImageBitmap?>,
    private val onPick: () -> Unit
) {
    actual val imagen: ImageBitmap? get() = imagenState.value
    actual fun pick() = onPick()
    actual fun limpiar() { imagenState.value = null }
}

@Composable
actual fun rememberImagePickerState(): ImagePickerState {
    val context = LocalContext.current
    val imagenState = remember { mutableStateOf<ImageBitmap?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                val bitmap = ImageDecoder.decodeBitmap(source)
                imagenState.value = bitmap.asImageBitmap()
            }
        }
    }
    return remember(launcher) {
        ImagePickerState(imagenState) {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
