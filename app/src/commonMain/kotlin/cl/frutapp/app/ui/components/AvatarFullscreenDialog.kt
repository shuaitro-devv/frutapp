package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog fullscreen para ver el avatar en grande. Tap en cualquier lado o en la
 * X cierra. La imagen ocupa el ancho de la pantalla con esquinas redondeadas,
 * fondo negro semi-translucido tipo overlay de galeria.
 */
@Composable
fun AvatarFullscreenDialog(bitmap: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Indicacion null + interactionSource nuevo para que tap en el fondo NO
        // dibuje el ripple sobre la imagen.
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(interactionSource = interaction, indication = null) { onDismiss() }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Avatar en grande",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable { onDismiss() }
                    .padding(10.dp)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
