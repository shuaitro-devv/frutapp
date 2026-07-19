package cl.frutapp.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import cl.frutapp.app.platform.renderizarFirmaPng
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Overlay fullscreen para capturar la firma del receptor.
 *
 * Diseño de estado:
 *  - Guardamos los trazos en TWO listas paralelas: una `List<Path>` para
 *    render (mutamos con lineTo directo, sin state) y una `List<List<Offset>>`
 *    para exportar al PNG (necesitamos los puntos para el encoder de plataforma).
 *  - Un `mutableIntStateOf` sirve como signal de invalidacion: incrementamos en
 *    cada punto agregado y Compose recompone solo el Canvas.
 *
 * Sin esto (version anterior con SnapshotStateList por trazo), cada punto
 * mutaba una snapshot list y disparaba recomposiciones del overlay entero;
 * en celulares gama baja la firma quedaba entrecortada / con puntos perdidos.
 *
 * awaitEachGesture cubre tanto drags como taps simples (para poner un
 * puntito de la 'i' o una firma corta), cosa que detectDragGestures no
 * hacía sin cruzar el touch-slop.
 */
@Composable
fun FirmaCaptureOverlay(
    onCancelar: () -> Unit,
    onGuardar: (pngBytes: ByteArray) -> Unit,
) {
    val paths = remember { mutableListOf<Path>() }
    val puntosPorTrazo = remember { mutableListOf<MutableList<Offset>>() }
    // Signal de invalidacion: cambia cada vez que agregamos un punto/trazo o
    // borramos todo. Solo se lee dentro del Canvas para restringir el scope.
    var tick by remember { mutableIntStateOf(0) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize(0, 0)) }
    // Requerimos canvasSize > 0 para habilitar Guardar: si la Box no midio
    // todavia (celu lento + doble tap muy rapido), renderizarFirmaPng con 0x0
    // devolvia un PNG 1x1 que pasaba todos los checks y quedaba como "firma
    // legal" del receptor.
    val hayFirma = tick > 0 && paths.isNotEmpty() &&
        canvasSize.width > 0 && canvasSize.height > 0
    // Fondo del overlay: opaco solido para que el bottom bar del
    // RepartidorEntrega (Problema / Confirmar entrega) NO se transparente y
    // confunda al repartidor sobre que botones estan activos. Blanco para
    // que los FrutButtons (outline verde + primary) se lean bien sin
    // personalizacion extra. Bloquea 100% de los clicks del layer inferior.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FrutAppColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Firma del receptor",
                color = FrutAppColors.Brand800,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pídele al cliente que firme aquí abajo con el dedo.",
                color = FrutAppColors.InkSoft,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
                    .onGloballyPositioned { coord -> canvasSize = coord.size }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            // Trackeamos SOLO el dedo que hizo el down inicial:
                            // multi-touch (dos dedos + palma apoyada) queda
                            // ignorado, evitando trazos con saltos raros o
                            // firmas cortadas por el otro pointer.
                            val downId = down.id
                            val path = Path().apply { moveTo(down.position.x, down.position.y) }
                            val puntos = mutableListOf(down.position)
                            paths.add(path)
                            puntosPorTrazo.add(puntos)
                            tick += 1
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == downId } ?: break
                                if (!change.pressed) break // dedo original levantado
                                if (change.positionChange() != Offset.Zero) {
                                    path.lineTo(change.position.x, change.position.y)
                                    puntos.add(change.position)
                                    tick += 1
                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Referencia al tick para que Compose invalide este Canvas
                    // cuando cambia. Los paths se mutan por fuera del snapshot.
                    val invalidacion = tick
                    if (invalidacion == 0 && paths.isEmpty()) return@Canvas
                    val paint = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    paths.forEachIndexed { i, path ->
                        val puntos = puntosPorTrazo.getOrNull(i)
                        if (puntos != null && puntos.size == 1) {
                            // Un solo punto: circulito para que la 'i' tenga su punto.
                            drawPoints(
                                points = listOf(puntos[0]),
                                pointMode = PointMode.Points,
                                color = FrutAppColors.Ink,
                                strokeWidth = 8f,
                                cap = StrokeCap.Round,
                            )
                        } else {
                            drawPath(path = path, color = FrutAppColors.Ink, style = paint)
                        }
                    }
                }
                if (!hayFirma) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Firma aquí",
                            color = FrutAppColors.InkSoft,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FrutButtonOutline(
                    text = "Cancelar",
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f),
                )
                FrutButtonOutline(
                    text = "Borrar",
                    onClick = {
                        paths.clear()
                        puntosPorTrazo.clear()
                        tick += 1
                    },
                    enabled = hayFirma,
                    modifier = Modifier.weight(1f),
                )
                FrutButtonPrimary(
                    text = "Guardar",
                    enabled = hayFirma,
                    onClick = {
                        val snapshot = puntosPorTrazo.map { it.toList() }
                        val bytes = renderizarFirmaPng(
                            trazos = snapshot,
                            ancho = canvasSize.width,
                            alto = canvasSize.height,
                        )
                        onGuardar(bytes)
                    },
                    modifier = Modifier.weight(1.2f),
                )
            }
        }
    }
}
