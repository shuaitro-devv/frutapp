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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Pantalla "Cola de pedidos" del picker (tab 'cola'). Replica el mockup picker-01-cola:
 * header con conteo total, banner de urgencia si hay pedidos antiguos, buscador, chips
 * de filtro y lista de cards de pedido tappeables.
 *
 * Hoy todo es mock data ([pedidosColaMock]). Cuando exista PickerApi se reemplaza el
 * `remember` por un `produceState` que llama al endpoint y maneja loading/error.
 */
@Composable
fun PickerColaContent(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    val pedidos = remember { pedidosColaMock() }
    val urgentes = remember(pedidos) { pedidos.count { it.urgente } }

    Column(modifier = modifier.fillMaxSize()) {
        ColaHeader(total = pedidos.size)
        if (urgentes > 0) {
            Spacer(Modifier.height(12.dp))
            BannerUrgencia(
                cantidad = urgentes,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        BuscadorYFiltros(modifier = Modifier.padding(horizontal = 16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pedidos, key = { it.id }) { pedido ->
                PedidoCard(
                    pedido = pedido,
                    onClick = { navigator.push(PickerPicklistScreen(pedidoId = pedido.id)) }
                )
            }
        }
    }
}

@Composable
private fun ColaHeader(total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cola de pedidos",
                color = FrutAppColors.Brand800,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pedidos pendientes por preparar",
                color = FrutAppColors.InkMuted,
                fontSize = 13.sp
            )
        }
        BadgeTotal(total = total)
    }
}

@Composable
private fun BadgeTotal(total: Int) {
    Column(
        modifier = Modifier
            .background(FrutAppColors.Brand400, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$total",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (total == 1) "pedido" else "pedidos",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BannerUrgencia(cantidad: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(FrutAppColors.Brand100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = FrutAppColors.Brand600,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$cantidad ${if (cantidad == 1) "pedido supera" else "pedidos superan"} 15 min",
                color = FrutAppColors.Brand800,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Revisa y prioriza para evitar demoras",
                color = FrutAppColors.InkSoft,
                fontSize = 12.sp
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = FrutAppColors.Brand600,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun BuscadorYFiltros(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        // Buscador (decorativo por ahora; cuando haya >20 pedidos cableamos a filter local).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = FrutAppColors.InkSoft,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Buscar pedido o cliente",
                color = FrutAppColors.InkSoft,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroChip(label = "Prioridad", icon = Icons.Filled.TrendingUp)
            FiltroChip(label = "Antigüedad", icon = Icons.Filled.AccessTime)
            FiltroChip(label = "Items", icon = Icons.Filled.Tune)
        }
    }
}

@Composable
private fun FiltroChip(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(20.dp))
            .clickable { /* TODO: abrir sheet de filtros */ }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PedidoCard(pedido: PedidoColaItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono cuadrado verde con la inicial del sector — refuerza la columna izquierda.
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                tint = FrutAppColors.Brand600,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pedido.id,
                    color = FrutAppColors.Brand800,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ChipAntiguedad(texto = pedido.antiguedadHumano(), urgente = pedido.urgente)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${pedido.items} items · ${formatoPeso(pedido.pesoKg)} kg",
                color = FrutAppColors.Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = FrutAppColors.InkSoft,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${pedido.sector} · ${pedido.destino}",
                    color = FrutAppColors.InkMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            ChipPrioridad(prioridad = pedido.prioridad)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = FrutAppColors.InkSoft,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ChipAntiguedad(texto: String, urgente: Boolean) {
    val bg = if (urgente) Color(0xFFFEE2E2) else FrutAppColors.Brand50
    val fg = if (urgente) Color(0xFFB91C1C) else FrutAppColors.Brand600
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = fg, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChipPrioridad(prioridad: PrioridadCola) {
    val (bg, fg) = when (prioridad) {
        PrioridadCola.ALTA -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        PrioridadCola.MEDIA -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        PrioridadCola.BAJA -> FrutAppColors.Brand50 to FrutAppColors.Brand600
    }
    Row(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(7.dp).background(fg, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Prioridad ${prioridad.label}",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatoPeso(kg: Double): String {
    // Sin libs de format avanzado en commonMain: redondeo a 1 decimal manual.
    val rounded = (kg * 10).toInt() / 10.0
    val entero = rounded.toInt()
    val decimal = ((rounded - entero) * 10).toInt()
    return if (decimal == 0) "$entero" else "$entero.$decimal"
}
