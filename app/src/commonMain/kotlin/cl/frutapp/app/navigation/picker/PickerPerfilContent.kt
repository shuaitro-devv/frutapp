package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Tab 'Perfil' del picker. Resumen del turno: hero verde con info del picker (nombre,
 * sucursal/bodega, turno activo), stats del turno (pedidos listos, items procesados,
 * tiempo promedio, incidencias), lista de pedidos completados (reusa pedidosListosMock)
 * y card de calidad. Cerrar sesion al final.
 *
 * Equivalente conceptual al RepartidorSaldoContent pero adaptado al picker — sin saldo
 * monetario (el picker es staff fijo, no por unidad) y con foco en productividad del turno.
 */
@Composable
fun PickerPerfilContent(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onAyuda: () -> Unit = {}
) {
    val pedidosTurno = remember { pedidosListosMock() }
    val nombrePicker = remember { TokenStore.user?.name?.substringBefore(' ') ?: "Picker" }
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mi turno", color = FrutAppColors.Brand800, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Picker", color = FrutAppColors.InkMuted, fontSize = 13.sp)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onAyuda)
                    .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        // Hero del picker
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(nombrePicker.take(1).uppercase(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(nombrePicker, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Picker · Bodega Norte", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF86EFAC), CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Text("Activo", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MiniBox(label = "Turno", valor = "10:00 → 18:00", sub = "Hace 4 h 30 min", modifier = Modifier.weight(1f))
                MiniBox(label = "Promedio", valor = "12 min", sub = "Por pedido", modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Resumen del turno", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        val totalIncidencias = pedidosTurno.sumOf { it.incidencias }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatBox(icon = Icons.Filled.CheckCircle, valor = "${pedidosTurno.size}", label = "Listos", modifier = Modifier.weight(1f))
            StatBox(icon = Icons.Filled.Inventory2, valor = "${pedidosTurno.sumOf { it.items }}", label = "Items", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatBox(icon = Icons.Filled.Schedule, valor = "12 min", label = "Promedio", modifier = Modifier.weight(1f))
            StatBox(icon = Icons.Filled.WarningAmber, valor = "$totalIncidencias", label = "Incidencias", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Pedidos del turno", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Ver todos", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pedidosTurno.forEach { PedidoTurnoRow(it) }
        }
        Spacer(Modifier.height(14.dp))
        // Card calidad
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand100, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Shield, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Calidad del turno", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Buen ritmo y baja tasa de incidencias.", color = FrutAppColors.InkSoft, fontSize = 11.sp)
            }
            Text("Detalle", color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(20.dp))
        FrutButtonOutline(text = "Cerrar sesión", onClick = onLogout)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MiniBox(label: String, valor: String, sub: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        Text(valor, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun StatBox(icon: ImageVector, valor: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(valor, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PedidoTurnoRow(p: PedidoListo) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(if (p.incidencias > 0) Icons.Filled.WarningAmber else Icons.Filled.CheckCircle, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(p.id, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text("${p.sector} · ${p.destino}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${p.items} items", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccessTime, null, tint = FrutAppColors.InkMuted, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(3.dp))
                Text("hace ${p.terminadoHaceMin} min", color = FrutAppColors.InkMuted, fontSize = 10.sp)
            }
        }
    }
}
