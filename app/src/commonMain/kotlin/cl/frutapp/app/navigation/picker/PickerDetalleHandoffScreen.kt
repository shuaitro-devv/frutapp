package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Detalle del handoff (boton 'Ver detalle' de PickerListoScreen). Muestra cliente, lista
 * completa de items con su resolucion final, resumen del turno y botones para generar
 * el voucher imprimible o volver.
 */
class PickerDetalleHandoffScreen(
    private val pedidoId: String,
    private val estados: Map<Int, EstadoItem>,
    /** UUID del pedido en backend. Si está, cargamos los items REALES via
     *  staffApi.detalle(); si null, fallback al picklistMock. */
    private val backendId: String? = null,
    /** Sector y nombre del cliente para la ClienteCard. Cuando vienen del flow
     *  real, override los hardcoded "Restaurante Verde · Sector Norte". */
    private val sector: String? = null,
    private val cliente: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val staffApi = remember { cl.frutapp.app.data.remote.StaffOrderApi() }
        var picklist by remember(pedidoId, backendId) {
            mutableStateOf<PicklistData?>(if (backendId != null) null else picklistMock(pedidoId))
        }
        LaunchedEffect(backendId) {
            val id = backendId ?: return@LaunchedEffect
            runCatching { staffApi.detalle(id) }
                .onSuccess { picklist = it.toPicklistData() }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    cl.frutapp.app.ui.ErrorReporter.report(screen = "PickerDetalleHandoff", action = "detalle", error = e)
                    picklist = picklistMock(pedidoId)
                }
        }
        val data = picklist ?: run {
            Box(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = FrutAppColors.Brand400)
            }
            return
        }
        // Fix #1: si llegamos con estados vacios (camino del historial via PickerListoScreen
        // sin estados explicitos), sintetizamos 'todos COMPLETADO' para que el detalle sea
        // coherente con el header 'Completado' y no muestre 12 items 'Pendiente'.
        val estadosEfectivos = remember(estados, pedidoId) {
            if (estados.isEmpty()) data.items.associate { it.numero to EstadoItem.COMPLETADO }
            else estados
        }

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Detalle del pedido", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(pedidoId, color = FrutAppColors.InkMuted, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.background(FrutAppColors.Brand400, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Completado", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { ClienteCard(cliente = cliente, sector = sector) }
                item {
                    Text("Resumen del pedido", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                item { ResumenCard(data = data, estados = estadosEfectivos) }
                item {
                    Text("Items (${data.totalItems})", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                items(data.items, key = { it.numero }) { item ->
                    // Fix #13: fallback a COMPLETADO (no PENDIENTE) en una pantalla cuyo
                    // header dice 'Completado'. Asi un caso bug-detect (item sin estado en
                    // un pedido cerrado) ya no se renderiza silenciosamente como 'Pendiente'.
                    val estado = estadosEfectivos[item.numero] ?: EstadoItem.COMPLETADO
                    ItemResumenRow(item = item, estado = estado)
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Equipo", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                item { EquipoCard() }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrutButtonOutline(text = "Volver", onClick = { navigator.pop() }, modifier = Modifier.weight(1f))
                FrutButtonPrimary(
                    text = "Ver voucher",
                    onClick = { navigator.push(PickerVoucherScreen(pedidoId, estadosEfectivos)) },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

@Composable
private fun ClienteCard(cliente: String?, sector: String?) {
    // Privacidad: el picker ve nombre + sector, NO direccion completa.
    val nombre = cliente ?: "Cliente"
    val inicial = nombre.firstOrNull()?.uppercase() ?: "C"
    val sectorTxt = sector ?: "—"
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Text(inicial, color = FrutAppColors.Brand600, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Cliente", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text(nombre, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(sectorTxt, color = FrutAppColors.InkMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ResumenCard(data: PicklistData, estados: Map<Int, EstadoItem>) {
    val completos = estados.values.count { it == EstadoItem.COMPLETADO }
    val sustituidos = estados.values.count { it == EstadoItem.SUSTITUIDO }
    val reducidos = estados.values.count { it == EstadoItem.REDUCIDO }
    val faltantes = estados.values.count { it == EstadoItem.FALTANTE }
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccessTime, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Duración del armado: 18 min", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            ResumenChip(label = "Completos", valor = completos, color = FrutAppColors.Brand600, modifier = Modifier.weight(1f))
            if (sustituidos > 0) ResumenChip(label = "Sustituidos", valor = sustituidos, color = EstadoPaleta.sustituido, modifier = Modifier.weight(1f))
            if (reducidos > 0) ResumenChip(label = "Reducidos", valor = reducidos, color = EstadoPaleta.reducido, modifier = Modifier.weight(1f))
            if (faltantes > 0) ResumenChip(label = "Faltantes", valor = faltantes, color = EstadoPaleta.faltante, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResumenChip(label: String, valor: Int, color: Color, modifier: Modifier = Modifier) {
    // Visual sigue siendo un box vertical con valor grande + label pequeño; el color es
    // el del estado. Centralizado: color/alpha vienen de la convencion de EstadoVisual.
    Column(
        modifier = modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$valor", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ItemResumenRow(item: ItemPicklist, estado: EstadoItem) {
    // Para PENDIENTE forzamos icono Remove + label corto; el resto sale de visual().
    val v = estado.visual()
    val labelCorto = when (estado) {
        EstadoItem.SUSTITUIDO -> "Sustituido"
        EstadoItem.REDUCIDO -> "Reducido"
        EstadoItem.FALTANTE -> "Faltante"
        else -> v.label
    }
    val iconCorto = v.icon ?: Icons.Filled.Remove
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Emoji directo (no IconBubble.initial porque corta el emoji — son 2+ codepoints).
        Box(
            modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text(item.emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.numero}. ${item.nombre}", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${formatoCant(item.cantidad)} ${item.unidad}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
        cl.frutapp.app.ui.components.StatusChip(
            label = labelCorto,
            color = v.color,
            icon = iconCorto,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun EquipoCard() {
    val pickerNombre = cl.frutapp.app.data.TokenStore.user?.name?.substringBefore(' ') ?: "Casero"
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Picker", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text(pickerNombre, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Lo Valledor Centro", color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
        Icon(Icons.Filled.QrCode2, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(28.dp))
    }
}
