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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import cl.frutapp.app.data.Order
import cl.frutapp.app.data.OrderEstado
import cl.frutapp.app.data.OrdersStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.navigation.shop.OrderTrackingScreen
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.canasta_frutas
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Mis pedidos (mockup 15): tabs por estado, búsqueda y lista de pedidos. Datos desde
 * [OrdersStore] (memoria). Tab "Pedidos" del bottom nav; cada pedido abre el seguimiento.
 */
class MisPedidosScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tabs = listOf<Pair<String, OrderEstado?>>(
            "Todos" to null,
            "En curso" to OrderEstado.EN_CURSO,
            "Completados" to OrderEstado.COMPLETADO,
            "Cancelados" to OrderEstado.CANCELADO
        )
        var tabSel by remember { mutableStateOf(0) }
        var query by remember { mutableStateOf("") }

        val visibles = OrdersStore.pedidos.filter {
            (tabs[tabSel].second == null || it.estado == tabs[tabSel].second) &&
                it.numero.contains(query.trim(), ignoreCase = true)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mis pedidos", color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { })
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { i, t ->
                        TabChip(label = t.first, selected = tabSel == i, onClick = { tabSel = i }, modifier = Modifier.weight(1f))
                    }
                }

                SearchBar(query = query, onQuery = { query = it }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

                if (visibles.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No tienes pedidos en esta categoría.", color = FrutAppColors.InkMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                    ) {
                        items(visibles.size) { idx ->
                            OrderCard(visibles[idx], onClick = {
                                val o = visibles[idx]
                                navigator.push(OrderTrackingScreen(numero = o.numero, total = o.total, direccion = o.direccion, entrega = o.entrega))
                            })
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
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.canasta_frutas),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp).padding(4.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.numero, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                EstadoChip(order.estado, modifier = Modifier.padding(start = 8.dp))
            }
            Text(order.fecha, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Text(formatClp(order.total), color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EstadoChip(estado: OrderEstado, modifier: Modifier = Modifier) {
    val (text, bg) = when (estado) {
        OrderEstado.EN_CURSO -> FrutAppColors.AmberCoin to FrutAppColors.AmberSoft
        OrderEstado.COMPLETADO -> FrutAppColors.Brand600 to FrutAppColors.Brand50
        OrderEstado.CANCELADO -> FrutAppColors.Error to FrutAppColors.Cream
    }
    Box(modifier = modifier.background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(estado.label, color = text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
