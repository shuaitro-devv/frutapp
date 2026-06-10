package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import cl.frutapp.app.platform.Imagenes
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.CancellationException

/**
 * Avatar circular con descarga lazy desde URL presignada. Mientras carga o si
 * falla la descarga, muestra una burbuja con la inicial del nombre. Patrón
 * replicado de polizapp `EvidenciaImagen` pero adaptado a avatares (siempre
 * circular, sin click expand).
 *
 * Para futuras apps: este componente vive en commonMain. La descarga usa
 * `Imagenes.descargar(url)` que es expect/actual; el decode con
 * `decodeImagen(bytes)` también lo es. Ambos en `cl.frutapp.app.platform`.
 */
@Composable
fun AvatarImage(
    url: String?,
    initial: String,
    size: Dp = 80.dp,
    background: Color = FrutAppColors.Brand400,
    initialColor: Color = Color.White,
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        runCatching { Imagenes.descargar(url) }
            .onSuccess { bytes -> bitmap = decodeImagen(bytes) }
            .onFailure { e ->
                if (e is CancellationException) throw e
                failed = true
            }
    }

    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center
    ) {
        val img = bitmap
        if (img != null && !failed) {
            Image(
                bitmap = img,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                initial.take(1).uppercase(),
                color = initialColor,
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
