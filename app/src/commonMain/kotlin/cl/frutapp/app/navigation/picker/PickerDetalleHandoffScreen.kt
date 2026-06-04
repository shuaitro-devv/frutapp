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
import androidx.compose.runtime.remember
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
    private val estados: Map<Int, EstadoItem>
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val data = remember(pedidoId) { picklistMock(pedidoId) }

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
                item { ClienteCard() }
                item {
                    Text("Resumen del pedido", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                item { ResumenCard(data = data, estados = estados) }
                item {
                    Text("Items (${data.totalItems})", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                items(data.items, key = { it.numero }) { item ->
                    val estado = estados[item.numero] ?: EstadoItem.PENDIENTE
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
                    onClick = { navigator.push(PickerVoucherScreen(pedidoId, estados)) },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

@Composable
private fun ClienteCard() {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Text("R", color = FrutAppColors.Brand600, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Cliente", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text("Restaurante Verde", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sector Norte · Av. Las Flores 1280", color = FrutAppColors.InkMuted, fontSize = 12.sp)
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
            if (sustituidos > 0) ResumenChip(label = "Sustituidos", valor = sustituidos, color = Color(0xFF3B82F6), modifier = Modifier.weight(1f))
            if (reducidos > 0) ResumenChip(label = "Reducidos", valor = reducidos, color = Color(0xFFD97706), modifier = Modifier.weight(1f))
            if (faltantes > 0) ResumenChip(label = "Faltantes", valor = faltantes, color = Color(0xFFB91C1C), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResumenChip(label: String, valor: Int, color: Color, modifier: Modifier = Modifier) {
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
    val (color, icon, label) = when (estado) {
        EstadoItem.COMPLETADO -> Triple(FrutAppColors.Brand600, Icons.Filled.Check as ImageVector, "Completado")
        EstadoItem.SUSTITUIDO -> Triple(Color(0xFF3B82F6), Icons.Filled.SwapHoriz as ImageVector, "Sustituido")
        EstadoItem.REDUCIDO -> Triple(Color(0xFFD97706), Icons.Filled.Remove as ImageVector, "Reducido")
        EstadoItem.FALTANTE -> Triple(Color(0xFFB91C1C), Icons.Filled.Close as ImageVector, "Faltante")
        EstadoItem.PENDIENTE -> Triple(FrutAppColors.InkSoft, Icons.Filled.Remove as ImageVector, "Pendiente")
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Text(item.emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.numero}. ${item.nombre}", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${formatoCant(item.cantidad)} ${item.unidad}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
        Row(
            modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(11.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EquipoCard() {
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
            Text("Camila R.", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Bodega Norte · Turno 10:00 - 18:00", color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
        Icon(Icons.Filled.QrCode2, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(28.dp))
    }
}
