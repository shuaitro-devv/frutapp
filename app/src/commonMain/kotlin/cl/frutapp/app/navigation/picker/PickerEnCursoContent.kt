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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.StaffOrderApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.delay

/**
 * Tab 'En curso' del picker: pedidos en EN_PICKING que YO tome y aun no completo.
 * Polling cada 5s al endpoint `?status=en_curso` (filtra por assigned_picker_id).
 * Tap → vuelve al picklist (el screen detecta el UUID y opera contra el backend).
 *
 * Cuando exista el endpoint de items individuales (Nivel 2), itemsListos reflejara
 * el progreso real; hoy mostramos 0 / N.
 */
private const val EN_CURSO_POLLING_MS = 5_000L

@Composable
fun PickerEnCursoContent(modifier: Modifier = Modifier) {
    val navigator = LocalNavigator.currentOrThrow
    val staffApi = remember { StaffOrderApi() }
    var pedidos by remember { mutableStateOf<List<PedidoEnCurso>>(emptyList()) }
    var cargandoInicial by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { staffApi.enCurso().map { it.toPedidoEnCurso() } }
                .onSuccess {
                    pedidos = it
                    cargandoInicial = false
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "PickerEnCurso", action = "fetch_en_curso", error = e)
                    if (cargandoInicial) cargandoInicial = false
                }
            delay(EN_CURSO_POLLING_MS)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Header(total = pedidos.size)
        when {
            cargandoInicial -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando...", color = FrutAppColors.InkMuted, fontSize = 14.sp)
            }
            pedidos.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No tienes pedidos en preparación", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Toma uno desde la cola para empezar", color = FrutAppColors.InkMuted, fontSize = 13.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pedidos, key = { it.backendId ?: it.id }) { p ->
                    Card(
                        pedido = p,
                        // yaTomado=true: el pedido ya esta asignado a este picker, NO hay
                        // que volver a llamar take (rebotaria con 409). Solo entramos al
                        // picklist a editar items / completar.
                        onClick = {
                            navigator.push(
                                PickerPicklistScreen(
                                    pedidoId = p.backendId ?: p.id,
                                    yaTomado = true,
                                    numero = p.id.takeIf { p.backendId != null },
                                    sector = p.sector.takeIf { p.backendId != null },
                                    cliente = p.destino.removePrefix("Pedido de ").takeIf { p.backendId != null }
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
private fun Header(total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("En curso", color = FrutAppColors.Brand800, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Pedidos que estás preparando ahora", color = FrutAppColors.InkMuted, fontSize = 13.sp)
        }
        Column(
            modifier = Modifier.background(FrutAppColors.Brand400, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$total", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(if (total == 1) "pedido" else "pedidos", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun Card(pedido: PedidoEnCurso, onClick: () -> Unit) {
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
        ) { Icon(Icons.Filled.PlayCircle, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp)) }
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
                    Text("${pedido.tiempoEnPreparacionMin} min", color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("${pedido.sector} · ${pedido.destino}", color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            // Barra de progreso + counter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.weight(1f).height(6.dp).background(FrutAppColors.Brand100, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(pedido.progreso).height(6.dp).background(FrutAppColors.Brand400, RoundedCornerShape(3.dp))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${pedido.itemsListos}/${pedido.itemsTotal}", color = FrutAppColors.Brand800, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Filled.ChevronRight, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(20.dp))
    }
}
