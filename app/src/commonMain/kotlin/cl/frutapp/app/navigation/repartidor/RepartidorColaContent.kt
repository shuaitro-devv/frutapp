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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text as M3Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.StaffDispatchApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.delay

/**
 * repartidor-01 — Cola de despachos pendientes.
 *
 * Cableada al backend real (StaffDispatchApi.cola) con polling cada 5s
 * mientras la pantalla esta activa. Mismo patron que PickerColaContent.
 * Primer fetch muestra spinner; los siguientes refrescan en silencio.
 *
 * El backend filtra por la pickup_location del repartidor (mismo modelo
 * que el picker), asi cada repartidor ve solo los despachos de su zona.
 */
private const val DISPATCH_POLLING_MS = 5_000L

@Composable
fun RepartidorColaContent(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    val dispatchApi = remember { StaffDispatchApi() }
    var despachos by remember { mutableStateOf<List<DespachoItem>>(emptyList()) }
    var cargandoInicial by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { dispatchApi.cola().map { it.toDespachoItem() } }
                .onSuccess {
                    despachos = it
                    cargandoInicial = false
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "RepartidorCola", action = "fetch_cola", error = e)
                    if (cargandoInicial) cargandoInicial = false
                }
            delay(DISPATCH_POLLING_MS)
        }
    }

    val urgentes = remember(despachos) { despachos.count { it.urgente } }

    Column(modifier = modifier.fillMaxSize()) {
        Header(total = despachos.size)
        if (urgentes > 0) {
            Spacer(Modifier.height(12.dp))
            Banner(cantidad = urgentes, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Spacer(Modifier.height(14.dp))
        Buscador(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FiltroChip("Prioridad", Icons.Filled.WarningAmber)
            FiltroChip("Zona", Icons.Filled.LocationOn)
            FiltroChip("Distancia", Icons.Filled.Tune)
        }
        when {
            cargandoInicial -> androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxSize()) {
                repeat(5) {
                    cl.frutapp.app.ui.components.SkeletonBox(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(120.dp),
                        androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    )
                }
            }
            despachos.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(FrutAppColors.Brand50, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.NearMe, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                M3Text("No hay despachos por retirar", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                M3Text("Cuando un pedido este listo aparecera aca.", color = FrutAppColors.InkMuted, fontSize = 13.sp)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(despachos, key = { it.backendId ?: it.id }) { d ->
                    DespachoCard(
                        item = d,
                        onClick = { navigator.push(RepartidorDetalleScreen(d.backendId ?: d.id)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Cola de despachos", color = FrutAppColors.Brand800, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Pedidos listos para entregar", color = FrutAppColors.InkMuted, fontSize = 13.sp)
        }
        Column(
            modifier = Modifier.background(FrutAppColors.Brand400, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$total", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(if (total == 1) "despacho" else "despachos", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun Banner(cantidad: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand100, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.WarningAmber, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("$cantidad ${if (cantidad == 1) "entrega vence" else "entregas vencen"} en menos de 20 min", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Revisa y prioriza tus entregas", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Buscador(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text("Buscar pedido o dirección", color = FrutAppColors.InkSoft, fontSize = 14.sp)
    }
}

@Composable
private fun FiltroChip(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(20.dp))
            .clickable { }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DespachoCard(item: DespachoItem, onClick: () -> Unit) {
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
        ) { Icon(Icons.Filled.NearMe, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.id, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                ChipEntrega(texto = item.tiempoEntregaHumano(), urgente = item.urgente)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(item.cliente, color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("${item.sector} · ${item.direccion}", color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Map, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text("${item.kmDistancia} km", color = FrutAppColors.InkMuted, fontSize = 11.sp)
                Spacer(Modifier.width(10.dp))
                ChipPrioridad(prioridad = item.prioridad)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ChipEntrega(texto: String, urgente: Boolean) {
    val bg = if (urgente) Color(0xFFFEE2E2) else FrutAppColors.Brand50
    val fg = if (urgente) Color(0xFFB91C1C) else FrutAppColors.Brand600
    Row(
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AccessTime, null, tint = fg, modifier = Modifier.size(11.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ChipPrioridad(prioridad: PrioridadDespacho) {
    val (bg, fg) = when (prioridad) {
        PrioridadDespacho.PARA_RETIRO -> FrutAppColors.Brand50 to FrutAppColors.Brand600
        PrioridadDespacho.ALTA -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        PrioridadDespacho.MEDIA -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        PrioridadDespacho.BAJA -> FrutAppColors.Brand50 to FrutAppColors.Brand600
    }
    Row(
        modifier = Modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).background(fg, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(prioridad.label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
