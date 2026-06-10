package cl.frutapp.app.navigation.repartidor

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
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Tab 'Entregados' del repartidor. Historial del turno con monto ganado por entrega y
 * conteo de incidencias. Mock con [despachosEntregadosMock]. Tap es no-op por ahora —
 * cuando se construya el detalle del entregado se cablea a esa pantalla.
 */
@Composable
fun RepartidorEntregadosContent(modifier: Modifier = Modifier) {
    val despachos = remember { despachosEntregadosMock() }
    val totalGanado = remember(despachos) { despachos.sumOf { it.gananciaCLP } }
    Column(modifier = modifier.fillMaxSize()) {
        Header(totalEntregas = despachos.size, totalGanado = totalGanado)
        // OJO: NUNCA usar `return@Column` dentro de un Composable; Compose construye
        // un arbol de grupos y el salto deja grupos sin cerrar → IndexOutOfBoundsException
        // en Stack.pop del Composer al recomponer. Siempre usar if/else.
        if (despachos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aún no entregas pedidos hoy", color = FrutAppColors.InkMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(despachos, key = { it.id }) { d ->
                    Card(item = d, onClick = { })
                }
            }
        }
    }
}

@Composable
private fun Header(totalEntregas: Int, totalGanado: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Entregados hoy", color = FrutAppColors.Brand800, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Historial de entregas del turno", color = FrutAppColors.InkMuted, fontSize = 13.sp)
        }
        Column(
            modifier = Modifier.background(FrutAppColors.Brand400, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$totalEntregas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(formatoClp(totalGanado), color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun Card(item: DespachoEntregado, onClick: () -> Unit) {
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
                Text(item.id, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.background(FrutAppColors.Brand50, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccessTime, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(tiempoHumano(item.entregadoHaceMin), color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(item.cliente, color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("${item.sector} · ${item.direccion}", color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+${formatoClp(item.gananciaCLP)}", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (item.incidencias > 0) {
                    Spacer(Modifier.width(10.dp))
                    Row(
                        modifier = Modifier.background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.WarningAmber, null, tint = Color(0xFF92400E), modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${item.incidencias} ${if (item.incidencias == 1) "incidencia" else "incidencias"}", color = Color(0xFF92400E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun tiempoHumano(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h > 0 && m > 0 -> "hace ${h} h ${m} min"
        h > 0 -> "hace ${h} h"
        else -> "hace ${m} min"
    }
}
