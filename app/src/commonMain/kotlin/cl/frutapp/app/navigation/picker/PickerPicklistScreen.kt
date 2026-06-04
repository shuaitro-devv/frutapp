package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Picklist del pedido (picker-02). Header con ID + chip estado, stat strip con donut de
 * progreso, lista de items con checkbox grande, botones inferiores para incidencia o
 * marcar como listo. Mock data — los toggles de check son local-state-only por ahora.
 */
class PickerPicklistScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val data = remember(pedidoId) { picklistMock(pedidoId) }
        // State machine por item: Map<numeroItem, EstadoItem>. Cada item siempre tiene un
        // estado; el boton 'listo' se desbloquea cuando ninguno queda en PENDIENTE.
        // Cuando exista el endpoint, esto sera un PATCH al backend por item.
        var estados by remember(pedidoId) {
            mutableStateOf(data.items.associate { it.numero to it.estado })
        }
        var modalAbierto by remember { mutableStateOf<ModalPicklist?>(null) }
        var itemModal by remember { mutableStateOf<ItemPicklist?>(null) }
        var opcionesAbierto by remember { mutableStateOf(false) }

        val resueltos = estados.values.count { it.resuelto() }
        val totalResueltos = data.totalItems

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            TopBar(
                pedidoId = data.pedidoId,
                onBack = { navigator.pop() },
                onMenu = { opcionesAbierto = true }
            )
            StatStrip(
                total = data.totalItems,
                tiempoMin = data.tiempoEstimadoMin,
                sector = data.sector,
                destino = data.destino,
                completos = estados.values.count { it == EstadoItem.COMPLETADO },
                sustituidos = estados.values.count { it == EstadoItem.SUSTITUIDO },
                reducidos = estados.values.count { it == EstadoItem.REDUCIDO },
                faltantes = estados.values.count { it == EstadoItem.FALTANTE }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Picklist del pedido", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Orden sugerido ▾", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(data.items, key = { it.numero }) { item ->
                    val estado = estados[item.numero] ?: EstadoItem.PENDIENTE
                    ItemCard(
                        item = item,
                        estado = estado,
                        onToggle = {
                            // Si esta PENDIENTE: peso variable → modal; sino → COMPLETADO.
                            // Si ya esta resuelto en cualquier estado: tap lo regresa a PENDIENTE
                            // (el picker puede 'deshacer' su decision antes de cerrar el pedido).
                            if (estado == EstadoItem.PENDIENTE) {
                                if (item.pesoVariable) {
                                    itemModal = item
                                    modalAbierto = ModalPicklist.PESO
                                } else {
                                    estados = estados + (item.numero to EstadoItem.COMPLETADO)
                                }
                            } else {
                                estados = estados + (item.numero to EstadoItem.PENDIENTE)
                            }
                        },
                        onSwap = {
                            itemModal = item
                            modalAbierto = ModalPicklist.SUSTITUCION
                        }
                    )
                }
            }
            BotonesInferior(
                onIncidencia = { showToast("Reportar incidencia - Próximamente") },
                onListo = {
                    val pendientes = estados.values.count { it == EstadoItem.PENDIENTE }
                    if (pendientes == 0) {
                        navigator.replace(PickerListoScreen(data.pedidoId, estados))
                    } else {
                        showToast("Aún quedan $pendientes items por resolver")
                    }
                }
            )
        }

        if (modalAbierto == ModalPicklist.PESO && itemModal != null) {
            PesoVariableModal(
                item = itemModal!!,
                onCerrar = { modalAbierto = null; itemModal = null },
                onConfirmar = {
                    estados = estados + (itemModal!!.numero to EstadoItem.COMPLETADO)
                    modalAbierto = null
                    itemModal = null
                }
            )
        }
        if (modalAbierto == ModalPicklist.SUSTITUCION && itemModal != null) {
            SustitucionModal(
                item = itemModal!!,
                onCerrar = { modalAbierto = null; itemModal = null },
                onConfirmar = { nuevoEstado ->
                    estados = estados + (itemModal!!.numero to nuevoEstado)
                    modalAbierto = null
                    itemModal = null
                }
            )
        }
        if (opcionesAbierto) {
            PickerOpcionesSheet(
                onCerrar = { opcionesAbierto = false },
                onElegir = { opcion ->
                    when (opcion) {
                        PickerOpcion.PAUSAR -> {
                            showToast("Pedido pausado - vuelto a la cola")
                            navigator.pop()
                        }
                        PickerOpcion.CANCELAR -> {
                            showToast("Cancelado (mock)")
                            navigator.pop()
                        }
                        else -> showToast("${opcion.titulo} - Próximamente")
                    }
                }
            )
        }
    }
}

internal enum class ModalPicklist { PESO, SUSTITUCION }

@Composable
private fun TopBar(pedidoId: String, onBack: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
        }
        Text(
            text = pedidoId,
            color = FrutAppColors.Brand800,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(FrutAppColors.Brand600, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("En preparación", color = FrutAppColors.Brand800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.MoreVert, "Más", tint = FrutAppColors.Brand800)
        }
    }
}

@Composable
private fun StatStrip(
    total: Int,
    tiempoMin: Int,
    sector: String,
    destino: String,
    completos: Int,
    sustituidos: Int,
    reducidos: Int,
    faltantes: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(Icons.Filled.Inventory2, "$total", "Total", modifier = Modifier.weight(1f))
        Divider()
        StatCell(Icons.Filled.AccessTime, "$tiempoMin min", "Estimado", modifier = Modifier.weight(1f))
        Divider()
        StatCell(Icons.Filled.LocationOn, sector, destino, modifier = Modifier.weight(1.4f))
        Divider()
        DonutSegmentado(
            total = total,
            completos = completos,
            sustituidos = sustituidos,
            reducidos = reducidos,
            faltantes = faltantes,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun StatCell(icon: androidx.compose.ui.graphics.vector.ImageVector, valor: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(2.dp))
        Text(valor, color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = FrutAppColors.InkMuted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(FrutAppColors.Brand100))
}

@Composable
private fun DonutSegmentado(
    total: Int,
    completos: Int,
    sustituidos: Int,
    reducidos: Int,
    faltantes: Int,
    modifier: Modifier = Modifier
) {
    val resueltos = completos + sustituidos + reducidos + faltantes
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(48.dp)) {
            val stroke = 5.dp.toPx()
            // Anillo base (gris claro) — representa el 100%, los items sin resolver quedan aqui.
            drawArc(
                color = FrutAppColors.Brand100,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            if (total == 0) return@Canvas
            // Arcos por tipo de resolucion, consecutivos desde el top (-90°). Asi el donut
            // se llena de izquierda a derecha mostrando la composicion real del progreso.
            val perItem = 360f / total
            var start = -90f
            fun arc(count: Int, color: Color) {
                if (count <= 0) return
                val sweep = perItem * count
                drawArc(
                    color = color,
                    startAngle = start, sweepAngle = sweep, useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                start += sweep
            }
            arc(completos, FrutAppColors.Brand400)
            arc(sustituidos, Color(0xFF3B82F6))
            arc(reducidos, Color(0xFFD97706))
            arc(faltantes, Color(0xFFB91C1C))
        }
        Text("$resueltos/$total", color = FrutAppColors.Brand800, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItemCard(item: ItemPicklist, estado: EstadoItem, onToggle: () -> Unit, onSwap: () -> Unit) {
    // Toda la card es tappeable para marcar/desmarcar. El borde refleja el estado:
    // verde fuerte si esta resuelto, gris si pendiente.
    val resuelto = estado.resuelto()
    val borde = if (resuelto) bordeColorPorEstado(estado) else FrutAppColors.Brand100
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(
                width = if (resuelto) 2.dp else 1.dp,
                color = borde,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(FrutAppColors.Brand50, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text(item.emoji, fontSize = 26.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.numero}. ${item.nombre}", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${formatoCant(item.cantidad)} ${item.unidad}",
                color = FrutAppColors.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (item.pesoVariable) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .background(FrutAppColors.Brand50, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Scale, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Peso variable", color = FrutAppColors.Brand600, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            // Chip de resolucion cuando el estado es distinto de COMPLETADO (que ya
            // se nota con el check verde): sustituido / reducido / faltante.
            ChipResolucion(estado = estado)
            Spacer(Modifier.height(4.dp))
            Text("Pasillo ${item.pasillo} · Estante ${item.estante}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            EstadoBoxGrande(estado = estado, onClick = onToggle)
            IconButton(onClick = onSwap, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.SwapHoriz, "Sustituir", tint = FrutAppColors.InkSoft, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Color de borde fuerte segun el tipo de resolucion. */
private fun bordeColorPorEstado(estado: EstadoItem): Color = when (estado) {
    EstadoItem.COMPLETADO -> FrutAppColors.Brand400
    EstadoItem.SUSTITUIDO -> Color(0xFF3B82F6) // azul
    EstadoItem.REDUCIDO -> Color(0xFFD97706)   // ambar
    EstadoItem.FALTANTE -> Color(0xFFB91C1C)   // rojo
    EstadoItem.PENDIENTE -> FrutAppColors.Brand100
}

@Composable
private fun ChipResolucion(estado: EstadoItem) {
    if (estado == EstadoItem.PENDIENTE || estado == EstadoItem.COMPLETADO) return
    val (bg, fg, label) = when (estado) {
        EstadoItem.SUSTITUIDO -> Triple(Color(0xFFDBEAFE), Color(0xFF1E40AF), "Sustituido")
        EstadoItem.REDUCIDO -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "Cantidad reducida")
        EstadoItem.FALTANTE -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Faltante reportado")
        else -> return
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EstadoBoxGrande(estado: EstadoItem, onClick: () -> Unit) {
    val (bg, fg, icon, desc) = when (estado) {
        EstadoItem.PENDIENTE -> EstadoBoxStyle(Color.White, FrutAppColors.Brand100, null, "Marcar")
        EstadoItem.COMPLETADO -> EstadoBoxStyle(FrutAppColors.Brand400, FrutAppColors.Brand400, Icons.Filled.Check, "Completado")
        EstadoItem.SUSTITUIDO -> EstadoBoxStyle(Color(0xFF3B82F6), Color(0xFF3B82F6), Icons.Filled.SwapHoriz, "Sustituido")
        EstadoItem.REDUCIDO -> EstadoBoxStyle(Color(0xFFD97706), Color(0xFFD97706), Icons.Filled.Remove, "Reducido")
        EstadoItem.FALTANTE -> EstadoBoxStyle(Color(0xFFB91C1C), Color(0xFFB91C1C), Icons.Filled.Close, "Faltante")
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = bg, shape = RoundedCornerShape(10.dp))
            .border(width = 2.dp, color = fg, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, desc, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

private data class EstadoBoxStyle(
    val bg: Color,
    val fg: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val desc: String
)

@Composable
private fun BotonesInferior(onIncidencia: () -> Unit, onListo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FrutButtonOutline(text = "Reportar", onClick = onIncidencia, modifier = Modifier.weight(1f))
        FrutButtonPrimary(text = "Marcar como listo", onClick = onListo, modifier = Modifier.weight(1.4f))
    }
}

internal fun formatoCant(v: Double): String {
    val r = (v * 10).toInt() / 10.0
    val e = r.toInt()
    val d = ((r - e) * 10).toInt()
    return if (d == 0) "$e" else "$e.$d"
}
