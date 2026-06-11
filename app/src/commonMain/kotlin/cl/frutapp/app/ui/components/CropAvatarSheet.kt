package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.platform.dimensionesImagen
import cl.frutapp.app.platform.recortarAvatar
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Dialog fullscreen que muestra la foto recien elegida y deja al usuario
 * encuadrar el avatar (pan + zoom) dentro de un circulo guia. Al confirmar,
 * recorta solo esa region y dispara [onListo] con los bytes JPEG ya
 * comprimidos (1024px, calidad 85).
 *
 * Patron: el caller llama [selectorImg.galeria()] y al recibir bytes, en vez
 * de llamar al backend directo, muestra este sheet. El upload va al
 * `onListo(bytesRecortados)`.
 */
@Composable
fun CropAvatarSheet(
    bytes: ByteArray,
    onDismiss: () -> Unit,
    onListo: (ByteArray) -> Unit
) {
    val scope = rememberCoroutineScope()
    var bitmap by remember(bytes) { mutableStateOf<ImageBitmap?>(null) }
    var dimensiones by remember(bytes) { mutableStateOf<Pair<Int, Int>?>(null) }
    var procesando by remember { mutableStateOf(false) }

    LaunchedEffect(bytes) {
        bitmap = decodeImagen(bytes)
        dimensiones = dimensionesImagen(bytes)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !procesando)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val img = bitmap
            val dims = dimensiones
            // No usar return@Box: el Composer rastrea slots por arbol estructural y un
            // return temprano rompe el conteo en el siguiente frame (Stack.pop crash).
            // Patron seguro: if/else con dos sub-arboles distintos.
            if (img == null || dims == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                CropContent(
                    bytes = bytes,
                    img = img,
                    imgWPx = dims.first,
                    imgHPx = dims.second,
                    procesando = procesando,
                    setProcesando = { procesando = it },
                    onDismiss = onDismiss,
                    onListo = onListo
                )
            }
        }
    }
}

@Composable
private fun CropContent(
    bytes: ByteArray,
    img: ImageBitmap,
    imgWPx: Int,
    imgHPx: Int,
    procesando: Boolean,
    setProcesando: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onListo: (ByteArray) -> Unit
) {
    val scope = rememberCoroutineScope()

            // Estado de la transformacion: scale + offset que aplica el usuario.
            var scaleUser by remember(bytes) { mutableStateOf(1f) }
            var offset by remember(bytes) { mutableStateOf(Offset.Zero) }
            // Medidas del viewport, llenadas por el BoxWithConstraints al medirse.
            // Las necesitamos visibles en el onClick del boton "Listo" para mapear
            // las coords de la pantalla a pixeles del bitmap original.
            var viewportW by remember { mutableStateOf(0f) }
            var viewportH by remember { mutableStateOf(0f) }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 36.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clickable(enabled = !procesando) { onDismiss() }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                    Text(
                        "Ajustá tu foto",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                // Viewport del crop. fillMaxWidth+weight(1f) para que el bottom CTA
                // quede siempre visible y el resto sea zona de gesture.
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp).weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val measuredW = with(density) { maxWidth.toPx() }
                    val measuredH = with(density) { maxHeight.toPx() }
                    // Sincronizar al outer scope para que el onClick del boton acceda.
                    LaunchedEffect(measuredW, measuredH) {
                        viewportW = measuredW
                        viewportH = measuredH
                    }
                    // baseScale: la imagen entera cabe centrada (fit).
                    val baseScale = minOf(measuredW / imgWPx, measuredH / imgHPx)
                    val effectiveScale = baseScale * scaleUser
                    // Radio del circulo: 42% del lado corto del viewport (deja margen).
                    val circleRadiusPx = minOf(measuredW, measuredH) * 0.42f

                    Image(
                        bitmap = img,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = effectiveScale
                                scaleY = effectiveScale
                                translationX = offset.x
                                translationY = offset.y
                            }
                            .pointerInput(bytes) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scaleUser = (scaleUser * zoom).coerceIn(1f, 5f)
                                    offset += pan
                                }
                            }
                    )

                    // Overlay con cutout circular. Dibujamos un rect negro semi-transparente
                    // y borramos el circulo central con BlendMode.Clear (necesita layer).
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        // Capa transparente para que Clear funcione como agujero real.
                        drawIntoCanvas { c ->
                            val paint = androidx.compose.ui.graphics.Paint()
                            c.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), paint)
                            drawRect(color = Color.Black.copy(alpha = 0.65f), size = size)
                            drawCircle(
                                color = Color.Transparent,
                                radius = circleRadiusPx,
                                center = Offset(cx, cy),
                                blendMode = BlendMode.Clear
                            )
                            c.restore()
                        }
                        // Borde blanco fino del circulo para dejar claro el area de recorte.
                        drawCircle(
                            color = Color.White,
                            radius = circleRadiusPx,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp)
                ) {
                    FrutButtonPrimary(
                        text = if (procesando) "Subiendo..." else "Listo",
                        enabled = !procesando,
                        onClick = {
                            if (procesando) return@FrutButtonPrimary
                            setProcesando(true)
                            scope.launch {
                                runCatching {
                                    // Mapeo viewport → bitmap original. La imagen se renderiza
                                    // centrada en el viewport con baseScale * scaleUser, despues
                                    // se aplica offset (en pixeles del viewport). El circulo
                                    // guia esta fijo en el centro del viewport.
                                    val baseScale = minOf(viewportW / imgWPx, viewportH / imgHPx)
                                    val effectiveScale = baseScale * scaleUser
                                    val circleRadiusPx = minOf(viewportW, viewportH) * 0.42f
                                    val centerImgX = imgWPx / 2f + (-offset.x) / effectiveScale
                                    val centerImgY = imgHPx / 2f + (-offset.y) / effectiveScale
                                    val radiusImg = circleRadiusPx / effectiveScale
                                    val srcX = (centerImgX - radiusImg).toInt()
                                    val srcY = (centerImgY - radiusImg).toInt()
                                    val srcSize = (radiusImg * 2f).toInt()
                                    recortarAvatar(
                                        srcBytes = bytes,
                                        srcX = srcX,
                                        srcY = srcY,
                                        srcSize = srcSize
                                    )
                                }
                                    .onSuccess { onListo(it) }
                                    .onFailure { e ->
                                        if (e is CancellationException) throw e
                                        setProcesando(false)
                                        // Fallback: subir la imagen original sin recortar para que el
                                        // usuario no quede atrapado. Errores del crop son raros (clamp
                                        // del actual ya cubre coords fuera de rango).
                                        onListo(bytes)
                                    }
                            }
                        }
                    )
                }
            }
}

/** Helper para drawIntoCanvas — Compose Multiplatform lo expone via DrawScope.drawContext */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoCanvas(
    block: (androidx.compose.ui.graphics.Canvas) -> Unit
) {
    drawContext.canvas.let(block)
}
