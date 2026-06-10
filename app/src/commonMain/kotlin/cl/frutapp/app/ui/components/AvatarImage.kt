package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import cl.frutapp.app.platform.Imagenes
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.CancellationException

/**
 * Avatar circular con descarga lazy desde URL presignada.
 *
 * Estados: loading (spinner), success (imagen), failed o sin URL (inicial).
 * Con `expandible = true` y URL valida, el tap abre [AvatarFullscreenDialog].
 *
 * Patron replicado de polizapp `EvidenciaImagen` y unificado para los 3 roles
 * de FrutApp (cliente / picker / repartidor).
 */
@Composable
fun AvatarImage(
    url: String?,
    initial: String,
    size: Dp = 80.dp,
    background: Color = FrutAppColors.Brand400,
    initialColor: Color = Color.White,
    expandible: Boolean = true,
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }
    var cargando by remember(url) { mutableStateOf(!url.isNullOrBlank()) }
    var mostrarGrande by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) { cargando = false; return@LaunchedEffect }
        runCatching { Imagenes.descargar(url) }
            .onSuccess { bytes ->
                bitmap = decodeImagen(bytes)
                cargando = false
                if (bitmap == null) failed = true
            }
            .onFailure { e ->
                if (e is CancellationException) throw e
                failed = true
                cargando = false
            }
    }

    val img = bitmap
    val puedeExpandir = expandible && img != null && !failed
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(background)
            .then(if (puedeExpandir) Modifier.clickable { mostrarGrande = true } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            img != null && !failed -> Image(
                bitmap = img,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            cargando -> CircularProgressIndicator(
                color = initialColor,
                modifier = Modifier.size(size * 0.4f),
                strokeWidth = (size.value * 0.04f).dp,
            )
            else -> Text(
                initial.take(1).uppercase(),
                color = initialColor,
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    if (puedeExpandir && mostrarGrande) {
        AvatarFullscreenDialog(img!!, onDismiss = { mostrarGrande = false })
    }
}
