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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
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
        // Estado local del check de cada item — espejo de EstadoItem.COMPLETADO. Cuando
        // exista el endpoint, esto sera un PATCH al backend y el estado vendra del servidor.
        var marcados by remember(pedidoId) {
            mutableStateOf(data.items.filter { it.estado != EstadoItem.PENDIENTE }.map { it.numero }.toSet())
        }
        // Modal abierto: null = ninguno; ej. PesoVariable o Sustitucion
        var modalAbierto by remember { mutableStateOf<ModalPicklist?>(null) }
        var itemModal by remember { mutableStateOf<ItemPicklist?>(null) }

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background)) {
            TopBar(
                pedidoId = data.pedidoId,
                onBack = { navigator.pop() },
                onMenu = { showToast("Más opciones - Próximamente") }
            )
            StatStrip(
                total = data.totalItems,
                tiempoMin = data.tiempoEstimadoMin,
                sector = data.sector,
                destino = data.destino,
                progreso = marcados.size.toFloat() / data.totalItems
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
                    val checkeado = item.numero in marcados
                    ItemCard(
                        item = item,
                        checkeado = checkeado,
                        onToggle = {
                            // Si es peso variable y aun no esta confirmado, abrimos el modal
                            // de peso real (picker-03). Si no, toggle simple.
                            if (item.pesoVariable && !checkeado) {
                                itemModal = item
                                modalAbierto = ModalPicklist.PESO
                            } else {
                                marcados = if (checkeado) marcados - item.numero else marcados + item.numero
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
                    if (marcados.size == data.totalItems) {
                        navigator.replace(PickerListoScreen(data.pedidoId))
                    } else {
                        showToast("Aún quedan ${data.totalItems - marcados.size} items por completar")
                    }
                }
            )
        }

        // Modales (picker-03 y picker-04). Los renderizamos aca con visibilidad condicional;
        // el contenido vive en sus propios composables reusables.
        if (modalAbierto == ModalPicklist.PESO && itemModal != null) {
            PesoVariableModal(
                item = itemModal!!,
                onCerrar = { modalAbierto = null; itemModal = null },
                onConfirmar = {
                    marcados = marcados + itemModal!!.numero
                    modalAbierto = null
                    itemModal = null
                }
            )
        }
        if (modalAbierto == ModalPicklist.SUSTITUCION && itemModal != null) {
            SustitucionModal(
                item = itemModal!!,
                onCerrar = { modalAbierto = null; itemModal = null },
                onConfirmar = {
                    marcados = marcados + itemModal!!.numero
                    modalAbierto = null
                    itemModal = null
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
private fun StatStrip(total: Int, tiempoMin: Int, sector: String, destino: String, progreso: Float) {
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
        DonutProgreso(progreso = progreso, total = total, modifier = Modifier.padding(start = 8.dp))
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
private fun DonutProgreso(progreso: Float, total: Int, modifier: Modifier = Modifier) {
    val completados = (progreso * total).toInt()
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(48.dp)) {
            val stroke = 5.dp.toPx()
            drawArc(
                color = FrutAppColors.Brand100,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            drawArc(
                color = FrutAppColors.Brand400,
                startAngle = -90f, sweepAngle = 360f * progreso, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Text("$completados/$total", color = FrutAppColors.Brand800, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItemCard(item: ItemPicklist, checkeado: Boolean, onToggle: () -> Unit, onSwap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(
                width = if (checkeado) 2.dp else 1.dp,
                color = if (checkeado) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = RoundedCornerShape(14.dp)
            )
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
            Spacer(Modifier.height(4.dp))
            Text("Pasillo ${item.pasillo} · Estante ${item.estante}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CheckBoxGrande(checked = checkeado, onClick = onToggle)
            IconButton(onClick = onSwap, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.SwapHoriz, "Sustituir", tint = FrutAppColors.InkSoft, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CheckBoxGrande(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (checked) FrutAppColors.Brand400 else Color.White,
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 2.dp,
                color = if (checked) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(Icons.Filled.Check, "Completado", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun BotonesInferior(onIncidencia: () -> Unit, onListo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
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
