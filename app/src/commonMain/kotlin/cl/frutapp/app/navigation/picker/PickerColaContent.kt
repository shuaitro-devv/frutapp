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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
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
import cl.frutapp.app.data.remote.StaffOrderApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.delay

/**
 * Pantalla "Cola de pedidos" del picker (tab 'cola').
 *
 * Cableada al backend real ([StaffOrderApi.cola]) con polling cada
 * [POLLING_MS] ms mientras la pantalla esta activa. El primer fetch muestra
 * spinner; los siguientes refrescan en silencio sin parpadeo. Errores se
 * reportan via [ErrorReporter] + toast amigable y la lista anterior se conserva
 * (no se vacia ante un fallo transitorio de red).
 *
 * El backend filtra implicitamente por la `pickup_location` del picker, asi que
 * cada usuario ve solo sus pedidos. La conversion DTO -> UI vive en
 * [toPedidoColaItem] (calcula minutosEspera/prioridad localmente).
 */
private const val POLLING_MS = 5_000L

@Composable
fun PickerColaContent(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    val staffApi = remember { StaffOrderApi() }
    var pedidos by remember { mutableStateOf<List<PedidoColaItem>>(emptyList()) }
    var cargandoInicial by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { staffApi.cola().map { it.toPedidoColaItem() } }
                .onSuccess {
                    pedidos = it
                    cargandoInicial = false
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "PickerCola", action = "fetch_cola", error = e)
                    if (cargandoInicial) {
                        // Primer fetch fallido: avisamos. Refresh silencioso si ya habia data.
                        showToast(mensajeAmigable(e, "cargar la cola"))
                        cargandoInicial = false
                    }
                }
            delay(POLLING_MS)
        }
    }

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

        when {
            cargandoInicial -> ColaLoading()
            pedidos.isEmpty() -> ColaVacia()
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pedidos, key = { it.backendId ?: it.id }) { pedido ->
                    PedidoCard(
                        pedido = pedido,
                        onClick = {
                            // Si vino del backend, navegamos con el UUID real para que el detalle
                            // pueda hacer take/complete contra el endpoint. Para fixture mock,
                            // pasamos el id legible (el detalle usa picklistMock en ese caso).
                            // Propagamos numero/sector/cliente para que la pantalla "Listo"
                            // final no tenga que mostrar el UUID feo ni "Sector Norte/Camila R."
                            // hardcoded del mock.
                            navigator.push(
                                PickerPicklistScreen(
                                    pedidoId = pedido.backendId ?: pedido.id,
                                    numero = pedido.id.takeIf { pedido.backendId != null },
                                    sector = pedido.sector.takeIf { pedido.backendId != null },
                                    cliente = pedido.destino.removePrefix("Pedido de ").takeIf { pedido.backendId != null }
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColaLoading() {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 40.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        CircularProgressIndicator(color = FrutAppColors.Brand400)
    }
}

@Composable
private fun ColaVacia() {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                tint = FrutAppColors.Brand400,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "No hay pedidos por preparar",
            color = FrutAppColors.Brand800,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Cuando llegue uno nuevo aparecera aca automaticamente.",
            color = FrutAppColors.InkMuted,
            fontSize = 13.sp
        )
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
