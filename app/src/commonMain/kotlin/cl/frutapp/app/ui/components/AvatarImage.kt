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
import cl.frutapp.app.platform.AvatarDiskCache
import cl.frutapp.app.platform.AvatarMemoryCache
import cl.frutapp.app.platform.Imagenes
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.platform.objectKeyFromUrl
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
    val objectKey = remember(url) { url?.let { objectKeyFromUrl(it) } }
    // Memory cache hit es sincronico: lo leemos como valor inicial del state para que
    // la primera composicion ya pinte la imagen, sin parpadeo de spinner.
    var bitmap by remember(objectKey) { mutableStateOf(objectKey?.let { AvatarMemoryCache.get(it) }) }
    var failed by remember(url) { mutableStateOf(false) }
    var cargando by remember(url, bitmap) { mutableStateOf(bitmap == null && !url.isNullOrBlank()) }
    var mostrarGrande by remember { mutableStateOf(false) }

    LaunchedEffect(url, objectKey) {
        if (url.isNullOrBlank()) { cargando = false; return@LaunchedEffect }
        // Si memory cache ya tiene el bitmap, nada que hacer (el state inicial lo cubrio).
        if (objectKey != null && AvatarMemoryCache.get(objectKey) != null) return@LaunchedEffect

        // Disk cache: si tenemos los bytes guardados de una corrida anterior, decodificamos
        // y mostramos al toque. Evita la descarga + el spinner en la mayoria de los casos.
        if (objectKey != null) {
            val cached = runCatching { AvatarDiskCache.get(objectKey) }.getOrNull()
            if (cached != null) {
                val decoded = decodeImagen(cached)
                if (decoded != null) {
                    AvatarMemoryCache.put(objectKey, decoded)
                    bitmap = decoded
                    cargando = false
                    return@LaunchedEffect
                }
            }
        }

        // Sin cache: descarga real desde la URL presignada.
        runCatching { Imagenes.descargar(url) }
            .onSuccess { bytes ->
                val decoded = decodeImagen(bytes)
                bitmap = decoded
                cargando = false
                if (decoded == null) {
                    failed = true
                } else if (objectKey != null) {
                    AvatarMemoryCache.put(objectKey, decoded)
                    // Disk write fire-and-forget — si falla, la proxima vez se vuelve a bajar.
                    runCatching { AvatarDiskCache.put(objectKey, bytes) }
                }
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
