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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Tab 'Listos' del picker. Historial del turno: pedidos terminados.
 *
 * **Pendiente**: cuando exista el endpoint `GET /v1/staff/orders?status=completados_hoy`
 * (que filtre por assigned_picker_id + completados en ultimas 24h), cableamos como
 * hicimos con `cola` y `en_curso`. Por ahora mostramos lista vacia para no
 * confundir el demo con datos mockup que no concuerdan con el flujo real (los
 * pedidos completados quedaban como si fueran de otro picker, en otros sectores).
 */
@Composable
fun PickerListosContent(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    val pedidos = remember { emptyList<PedidoListo>() }
    Column(modifier = modifier.fillMaxSize()) {
        Header(total = pedidos.size, hoyEntregados = pedidos.size)
        if (pedidos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No has completado pedidos hoy", color = FrutAppColors.InkMuted, fontSize = 14.sp)
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(pedidos, key = { it.id }) { p ->
                Card(pedido = p, onClick = { navigator.push(PickerListoScreen(p.id)) })
            }
        }
    }
}

@Composable
private fun Header(total: Int, hoyEntregados: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Listos hoy", color = FrutAppColors.Brand800, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Pedidos completados de este turno", color = FrutAppColors.InkMuted, fontSize = 13.sp)
        }
        Column(
            modifier = Modifier.background(FrutAppColors.Brand400, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$hoyEntregados", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("entregados", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun Card(pedido: PedidoListo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.CheckCircle, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pedido.id, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.background(FrutAppColors.Brand50, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccessTime, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("hace ${pedido.terminadoHaceMin} min", color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("${pedido.items} items completados", color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("${pedido.sector} · ${pedido.destino}", color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            if (pedido.incidencias > 0) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.WarningAmber, null, tint = Color(0xFF92400E), modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${pedido.incidencias} ${if (pedido.incidencias == 1) "incidencia" else "incidencias"}", color = Color(0xFF92400E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(20.dp))
    }
}
