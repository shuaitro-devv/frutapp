@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.huboItems
import cl.frutapp.app.data.reorderIntoCart
import cl.frutapp.app.data.toastMessage
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.navigation.shop.CartScreen
import cl.frutapp.app.navigation.shop.OrderTrackingScreen
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.components.OrderListSkeleton
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.OrderSummaryDto
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.canasta_frutas
import frutapp.app.generated.resources.mascota_cajita
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Mis pedidos (mockup 15): tabs por estado, búsqueda y lista. Datos del backend
 * (GET /v1/orders). Cada pedido abre el seguimiento real.
 */
class MisPedidosScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var pedidos by remember { mutableStateOf<List<OrderSummaryDto>?>(null) }
        var error by remember { mutableStateOf(false) }
        var tabSel by remember { mutableStateOf(0) }
        var query by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            runCatching { OrderApi().list() }
                .onSuccess { pedidos = it }
                .onFailure { e ->
                    cl.frutapp.app.ui.ErrorReporter.report(screen = "MisPedidos", action = "list_orders", error = e)
                    error = true
                }
        }

        val tabs = listOf("Todos", "En curso", "Completados", "Cancelados")
        val visibles = (pedidos ?: emptyList()).filter {
            matchesTab(tabSel, it.status) && it.numero.contains(query.trim(), ignoreCase = true)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mis pedidos", color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navigator.push(cl.frutapp.app.navigation.profile.AyudaScreen()) })
                }

                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabs.forEachIndexed { i, t ->
                        TabChip(label = t, selected = tabSel == i, onClick = { tabSel = i }, modifier = Modifier.weight(1f))
                    }
                }

                SearchBar(query = query, onQuery = { query = it }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

                when {
                    error -> Center("No pudimos cargar tus pedidos.", Modifier.weight(1f))
                    pedidos == null -> OrderListSkeleton(Modifier.weight(1f).padding(top = 6.dp))
                    visibles.isEmpty() -> EmptyPedidos(Modifier.weight(1f))
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                    ) {
                        items(visibles.size) { idx ->
                            val o = visibles[idx]
                            OrderCard(
                                o,
                                onClick = { navigator.push(OrderTrackingScreen(orderId = o.id)) },
                                onReorder = {
                                    // El resumen no trae los ítems; pedimos el detalle para re-armar.
                                    scope.launch {
                                        val detalle = runCatching { OrderApi().get(o.id) }.getOrNull()
                                        if (detalle == null) {
                                            showToast("No pudimos cargar el pedido")
                                            return@launch
                                        }
                                        val r = reorderIntoCart(detalle.items)
                                        showToast(r.toastMessage())
                                        if (r.huboItems()) navigator.push(CartScreen())
                                    }
                                }
                            )
                        }
                    }
                }

                FrutBottomNav(
                    selected = FrutTab.PEDIDOS,
                    onSelect = { tab -> if (tab != FrutTab.PEDIDOS) navigator.popUntilRoot() }
                )
            }
        }
    }
}

private fun matchesTab(tabIndex: Int, status: String): Boolean = when (tabIndex) {
    1 -> status in setOf("CREADO", "PAGADO", "EN_PICKING", "ESPERANDO_AJUSTE_CLIENTE", "STOCK_CONFIRMADO", "FACTURADO", "EN_DESPACHO")
    2 -> status == "ENTREGADO"
    3 -> status in setOf("CANCELADO", "DEVOLUCION")
    else -> true
}

private fun statusLabel(status: String): String = when (status) {
    "CREADO" -> "Creado"
    "PAGADO" -> "Pagado"
    "EN_PICKING" -> "En preparación"
    "ESPERANDO_AJUSTE_CLIENTE" -> "Esperando tu confirmación"
    "STOCK_CONFIRMADO" -> "Stock confirmado"
    "FACTURADO" -> "Facturado"
    "EN_DESPACHO" -> "En camino"
    "ENTREGADO" -> "Entregado"
    "CANCELADO" -> "Cancelado"
    "DEVOLUCION" -> "Devolución"
    else -> status
}

@Composable
private fun Center(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = FrutAppColors.InkMuted, fontSize = 14.sp)
    }
}

@Composable
private fun EmptyPedidos(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(140.dp).background(FrutAppColors.Brand50, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = org.jetbrains.compose.resources.painterResource(frutapp.app.generated.resources.Res.drawable.mascota_cajita),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size(118.dp)
            )
        }
        Text("Aún no tienes pedidos por acá", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text("Cuando pidas algo, te lo llevo aquí mismo.", color = FrutAppColors.InkMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand50, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.White else FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(48.dp).background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = FrutAppColors.InkMuted, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            if (query.isEmpty()) Text("Buscar por número de pedido…", color = FrutAppColors.InkSoft, fontSize = 14.sp)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 14.sp),
                cursorBrush = SolidColor(FrutAppColors.Brand400),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummaryDto, onClick: () -> Unit, onReorder: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(Res.drawable.canasta_frutas), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(44.dp).padding(4.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.numero, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    EstadoChip(order.status, modifier = Modifier.padding(start = 8.dp))
                }
                Text("${order.itemsCount} producto(s)", color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Text(formatClp(order.total), color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(20.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onReorder)
                .padding(start = 68.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Replay, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
            Text("Volver a pedir", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun EstadoChip(status: String, modifier: Modifier = Modifier) {
    val (text, bg) = when {
        status == "ENTREGADO" -> FrutAppColors.Brand600 to FrutAppColors.Brand50
        status == "CANCELADO" || status == "DEVOLUCION" -> FrutAppColors.Error to FrutAppColors.Cream
        else -> FrutAppColors.AmberCoin to FrutAppColors.AmberSoft
    }
    Box(modifier = modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(statusLabel(status), color = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
