@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.fulfillmentLabel
import cl.frutapp.app.data.huboItems
import cl.frutapp.app.data.paymentMethodLabel
import cl.frutapp.app.data.pedidoToCanastaItems
import cl.frutapp.app.data.reorderIntoCart
import cl.frutapp.app.data.toastMessage
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.navigation.canastas.NuevaCanastaScreen
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.components.SkeletonBox
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.OrderDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.camion_reparto
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private enum class PasoEstado { COMPLETADO, ACTIVO, PENDIENTE }
private data class Paso(val titulo: String, val detalle: String, val estado: PasoEstado)

/**
 * Seguimiento de pedido (mockup 12): carga el pedido real del backend y deriva el
 * timeline del estado de la orden. La operación avanza ese estado desde el back office.
 */
class OrderTrackingScreen(private val orderId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var order by remember { mutableStateOf<OrderDto?>(null) }
        var error by remember { mutableStateOf(false) }

        // Auto-refresh: re-consulta el pedido mientras la pantalla está abierta para que el
        // timeline avance solo (el backend mueve el estado). Para al llegar a un estado final.
        LaunchedEffect(orderId) {
            while (true) {
                runCatching { OrderApi().get(orderId) }
                    .onSuccess { order = it }
                    .onFailure { if (order == null) error = true }
                if (order?.status in setOf("ENTREGADO", "CANCELADO", "DEVOLUCION")) break
                delay(8000)
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TrackTopBar(onBack = { navigator.pop() })

                val o = order
                when {
                    error -> Centered("No pudimos cargar el pedido.")
                    o == null -> Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth(0.5f).height(18.dp))
                        Spacer(Modifier.height(16.dp))
                        SkeletonBox(Modifier.fillMaxWidth().height(150.dp), RoundedCornerShape(20.dp))
                        Spacer(Modifier.height(20.dp))
                        repeat(4) {
                            SkeletonBox(Modifier.fillMaxWidth(0.7f).height(16.dp))
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    else -> Detail(
                        o,
                        modifier = Modifier.weight(1f),
                        onReorder = {
                            scope.launch {
                                val r = reorderIntoCart(o.items)
                                showToast(r.toastMessage())
                                if (r.huboItems()) navigator.push(CartScreen())
                            }
                        },
                        onCalificar = { navigator.push(CalificarPedidoScreen(o.items)) },
                        onGuardarCanasta = {
                            scope.launch {
                                val items = pedidoToCanastaItems(o.items)
                                if (items.isEmpty()) {
                                    showToast("No pudimos cargar los productos del pedido")
                                } else {
                                    navigator.push(NuevaCanastaScreen(itemsIniciales = items))
                                }
                            }
                        }
                    )
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
private fun Detail(o: OrderDto, modifier: Modifier, onReorder: () -> Unit, onCalificar: () -> Unit, onGuardarCanasta: () -> Unit) {
    val pasos = pasosFor(o.status)
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(o.numero, color = FrutAppColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(modifier = Modifier.background(FrutAppColors.Brand400, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 5.dp)) {
                Text(statusLabel(o.status), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(170.dp).background(FrutAppColors.Brand50, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.camion_reparto),
                contentDescription = "Reparto",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(140.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Entrega estimada", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                Text(o.entrega, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text("Estado del pedido", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
        pasos.forEachIndexed { i, paso -> TimelineStep(paso, isLast = i == pasos.lastIndex) }

        val esRetiro = o.fulfillmentType == "RETIRO"
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (esRetiro) Icons.Filled.Storefront else Icons.Filled.Place, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(fulfillmentLabel(o.fulfillmentType), color = FrutAppColors.InkSoft, fontSize = 12.sp)
                Text(if (esRetiro) (o.sucursal ?: o.direccion) else o.direccion, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (o.payments.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(14.dp)
            ) {
                Text("Medios de pago", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                o.payments.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(paymentMethodLabel(p.method), color = FrutAppColors.Ink, fontSize = 13.sp)
                        Text(formatClp(p.monto), color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(if (o.totalFinal != null) "Total final" else "Total", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(formatClp(o.totalFinal ?: o.totalEstimado), color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        if (o.status == "ENTREGADO") {
            Box(modifier = Modifier.padding(top = 16.dp)) {
                FrutButtonPrimary(text = "Califica tu compra", onClick = onCalificar, leadingIcon = Icons.Filled.Star)
            }
        }
        Box(modifier = Modifier.padding(top = 16.dp)) {
            FrutButtonPrimary(text = "Volver a pedir", onClick = onReorder)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onGuardarCanasta),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ShoppingBasket, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
            Text(
                "Guardar este pedido como canasta",
                color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Centered(text: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(text, color = FrutAppColors.InkMuted, fontSize = 14.sp)
    }
}

private fun statusLabel(status: String): String = when (status) {
    "CREADO" -> "Creado"
    "PAGADO" -> "Pagado"
    "EN_PICKING" -> "En preparación"
    "STOCK_CONFIRMADO" -> "Stock confirmado"
    "FACTURADO" -> "Facturado"
    "EN_DESPACHO" -> "En camino"
    "ENTREGADO" -> "Entregado"
    "CANCELADO" -> "Cancelado"
    "DEVOLUCION" -> "Devolución"
    else -> status
}

private fun pasosFor(status: String): List<Paso> {
    val step = when (status) {
        "CREADO", "PAGADO" -> 0
        "EN_PICKING", "STOCK_CONFIRMADO", "FACTURADO" -> 1
        "EN_DESPACHO" -> 2
        "ENTREGADO" -> 3
        else -> 0
    }
    val labels = listOf(
        "Pedido confirmado" to "Recibimos tu pedido",
        "Preparando tu pedido" to "Seleccionando productos frescos",
        "En camino" to "Tu pedido va hacia tu dirección",
        "Entregado" to "Disfruta tus productos"
    )
    return labels.mapIndexed { i, (titulo, detalle) ->
        val estado = when {
            i < step -> PasoEstado.COMPLETADO
            i == step -> PasoEstado.ACTIVO
            else -> PasoEstado.PENDIENTE
        }
        Paso(titulo, detalle, estado)
    }
}

@Composable
private fun TrackTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
        }
        Text("Seguimiento de pedido", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { })
    }
}

@Composable
private fun TimelineStep(paso: Paso, isLast: Boolean) {
    val verde = paso.estado == PasoEstado.COMPLETADO || paso.estado == PasoEstado.ACTIVO
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(28.dp).background(if (verde) FrutAppColors.Brand400 else FrutAppColors.Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (paso.estado == PasoEstado.COMPLETADO) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else if (paso.estado == PasoEstado.ACTIVO) {
                    Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                }
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(34.dp).background(if (paso.estado == PasoEstado.COMPLETADO) FrutAppColors.Brand400 else FrutAppColors.Brand100))
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp, bottom = if (isLast) 0.dp else 8.dp)) {
            Text(
                paso.titulo,
                color = if (paso.estado == PasoEstado.PENDIENTE) FrutAppColors.InkSoft else FrutAppColors.Ink,
                fontSize = 15.sp,
                fontWeight = if (paso.estado == PasoEstado.ACTIVO) FontWeight.Bold else FontWeight.SemiBold
            )
            Text(paso.detalle, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
        }
    }
}
