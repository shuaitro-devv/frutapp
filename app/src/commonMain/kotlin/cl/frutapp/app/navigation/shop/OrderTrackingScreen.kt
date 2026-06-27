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
import androidx.compose.material.icons.filled.PhotoCamera
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
        var evidencias by remember { mutableStateOf<List<cl.frutapp.shared.dto.OrderItemEvidenceDto>>(emptyList()) }
        var visorEvidencia by remember { mutableStateOf<cl.frutapp.shared.dto.OrderItemEvidenceDto?>(null) }

        // Auto-refresh: re-consulta el pedido mientras la pantalla está abierta para que el
        // timeline avance solo (el backend mueve el estado). Para al llegar a un estado final.
        LaunchedEffect(orderId) {
            while (true) {
                runCatching { OrderApi().get(orderId) }
                    .onSuccess { order = it }
                    .onFailure { e ->
                        // Solo reporta el primer fallo (sin order); los reintentos posteriores
                        // son ruido de polling, no eventos de error que valga la pena loggear.
                        if (order == null) {
                            cl.frutapp.app.ui.ErrorReporter.report(screen = "OrderTracking", action = "get_order", error = e)
                            error = true
                        }
                    }
                if (order?.status in setOf("ENTREGADO", "CANCELADO", "DEVOLUCION")) break
                delay(8000)
            }
        }

        // Refrescar feature flags al abrir el tracking: si la central activo
        // chat / mapa / etc despues de que el cliente abrio la app, el TTL
        // de 30 min del ConfigStore no nos sirve. Esta pantalla es uno de
        // los puntos de entrada principales del cliente, asi que aprovechamos
        // para tirar un refresh y que los botones del chat / mapa aparezcan
        // sin que el cliente cierre la app.
        LaunchedEffect(Unit) {
            runCatching { cl.frutapp.app.data.ConfigStore.refreshNow() }
        }

        // Evidencias del picker: silencioso si falla (no es info critica).
        // Re-fetch cada 30s para que las fotos aparezcan a medida que el picker
        // las sube en otro lado mientras el cliente mira el tracking.
        LaunchedEffect(orderId) {
            while (true) {
                runCatching { cl.frutapp.app.data.remote.OrderEvidenceApi().listar(orderId) }
                    .onSuccess { evidencias = it }
                delay(30_000)
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TrackTopBar(onBack = { navigator.pop() }, onAyuda = { navigator.push(cl.frutapp.app.navigation.profile.AyudaScreen()) })

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
                    o.status == "ESPERANDO_AJUSTE_CLIENTE" -> AjusteBanner(
                        numero = o.numero,
                        modifier = Modifier.weight(1f),
                        onRevisar = { navigator.push(AjusteAprobacionScreen(o.id)) }
                    )
                    else -> Detail(
                        o,
                        modifier = Modifier.weight(1f),
                        evidencias = evidencias,
                        onEvidenciaClick = { visorEvidencia = it },
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
            visorEvidencia?.let { ev ->
                VisorEvidenciaFullscreen(evidencia = ev, onCerrar = { visorEvidencia = null })
            }
        }
    }
}

/** Banner full-screen cuando el pedido esta en ESPERANDO_AJUSTE_CLIENTE: oculta el
 *  timeline (no aporta info nueva) y empuja al cliente a la pantalla de aprobacion.
 *  Hasta que actue, el pedido no avanza. */
@Composable
private fun AjusteBanner(numero: String, modifier: Modifier = Modifier, onRevisar: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Star,
                null,
                tint = FrutAppColors.Brand600,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Hay un ajuste de peso",
            color = FrutAppColors.Brand800,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Pedido $numero · Tu Seleccionador pesó tus frutas y algunos items quedaron " +
                "con un peso distinto al pedido. Revisa el nuevo total antes de continuar.",
            color = FrutAppColors.InkSoft,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        FrutButtonPrimary(
            text = "Revisar ajuste",
            onClick = onRevisar
        )
    }
}

@Composable
private fun Detail(
    o: OrderDto,
    modifier: Modifier,
    evidencias: List<cl.frutapp.shared.dto.OrderItemEvidenceDto>,
    onEvidenciaClick: (cl.frutapp.shared.dto.OrderItemEvidenceDto) -> Unit,
    onReorder: () -> Unit,
    onCalificar: () -> Unit,
    onGuardarCanasta: () -> Unit
) {
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

        // Codigo de entrega: visible solo cuando el pedido esta EN_DESPACHO.
        // El cliente DEBE darselo al repartidor cara a cara para confirmar la
        // entrega — sin esto, el repartidor no puede marcar entregado.
        val codigoEntrega = o.deliveryCode
        if (o.status == "EN_DESPACHO" && !codigoEntrega.isNullOrBlank()) {
            CodigoEntregaCard(codigo = codigoEntrega, modifier = Modifier.padding(top = 14.dp))
        }

        // Hero: mapa con tracking del repartidor cuando EN_DESPACHO; imagen
        // ilustrativa para el resto de los estados. Gated por feature.mapa_repartidor.
        // Si la flag esta apagada, mostramos siempre la imagen.
        val mapaHabilitado = cl.frutapp.app.data.ConfigStore.boolOrDefault("feature.mapa_repartidor", default = true)
        if (o.status == "EN_DESPACHO" && mapaHabilitado) {
            MapaRepartidor(orderId = o.id, modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(220.dp))
        } else {
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

        // Lista de items del pedido. Muestra items rechazados (SIN_STOCK) tachados
        // con badge "No disponible" para que el cliente recuerde por que el total
        // cambio respecto a lo que pedio. Y items con peso ajustado del picker
        // muestran el peso real para que sea claro lo que llega.
        ProductosResumen(o.items, evidencias = evidencias, onEvidenciaClick = onEvidenciaClick, modifier = Modifier.padding(top = 16.dp))

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

        // Botones de chat segun estado del pedido. Gated por feature.chat.
        // Cliente puede chatear con el picker cuando el pedido esta en
        // armado (EN_PICKING / ESPERANDO_AJUSTE / STOCK_CONFIRMADO) y con el
        // repartidor cuando esta EN_DESPACHO.
        // Default = true (mismo patron que el resto de features actuales).
        // featureEnabled() defaultea a false y eso oculta el boton si la
        // cache local del cliente todavia no recibio la flag — bug
        // reportado por el usuario al no ver el chat en tracking.
        val chatHabilitado = cl.frutapp.app.data.ConfigStore.boolOrDefault("feature.chat", default = true)
        if (chatHabilitado) {
            val canPicker = o.status in setOf("EN_PICKING", "ESPERANDO_AJUSTE_CLIENTE", "STOCK_CONFIRMADO")
            val canRepartidor = o.status == "EN_DESPACHO"
            if (canPicker || canRepartidor) {
                val navigator2 = LocalNavigator.currentOrThrow
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (canPicker) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(12.dp))
                                .clickable {
                                    navigator2.push(ChatScreen(
                                        orderId = o.id,
                                        destinatarioRol = "picker",
                                        tituloContraparte = "Tu Seleccionador de Frescura"
                                    ))
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Chatear con el seleccionador",
                                    color = FrutAppColors.Brand800,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                // Badge de mensajes no leidos. La query del backend
                                // cuenta todos los mensajes destinados al cliente sin
                                // leer en este pedido — al tocar cualquier chat se
                                // marcan todos como leidos y el badge desaparece.
                                if (o.chatUnread > 0 && !canRepartidor) {
                                    ChatUnreadDot(o.chatUnread)
                                }
                            }
                        }
                    }
                    if (canRepartidor) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(12.dp))
                                .clickable {
                                    navigator2.push(ChatScreen(
                                        orderId = o.id,
                                        destinatarioRol = "repartidor",
                                        tituloContraparte = "Tu Repartidor"
                                    ))
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Chatear con el repartidor",
                                    color = FrutAppColors.Brand800,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                // Cuando hay 2 botones (picker + repartidor),
                                // mostramos el badge solo en este (el ultimo
                                // contacto vivo del pedido).
                                if (o.chatUnread > 0) {
                                    ChatUnreadDot(o.chatUnread)
                                }
                            }
                        }
                    }
                }
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

/**
 * Card "Productos del pedido" con la lista de items. Items en SIN_STOCK
 * (rechazados por el cliente en el ajuste de peso) salen tachados con badge
 * "No disponible" y monto $0 — el cliente entiende por que el total final
 * difiere del estimado. Items con peso ajustado por el picker muestran el
 * peso real al lado del pedido.
 */
@Composable
private fun ProductosResumen(
    items: List<cl.frutapp.shared.dto.OrderItemDto>,
    evidencias: List<cl.frutapp.shared.dto.OrderItemEvidenceDto> = emptyList(),
    onEvidenciaClick: (cl.frutapp.shared.dto.OrderItemEvidenceDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.Cream, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            "Productos del pedido",
            color = FrutAppColors.Brand800,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        items.forEach { item ->
            val sinStock = item.itemStatus == "SIN_STOCK"
            val sustituido = item.itemStatus == "SUSTITUIDO" && item.sustitutoNombre != null
            val gramosLocal = item.gramos
            val pesoRealLocal = item.pesoReal
            val pesoAjustado = !sustituido && pesoRealLocal != null && gramosLocal != null &&
                pesoRealLocal != gramosLocal * item.cantidad
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.nombre,
                        color = if (sinStock) FrutAppColors.InkMuted else FrutAppColors.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (sinStock) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    Text(
                        text = detalleItem(item),
                        color = FrutAppColors.InkMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (sinStock) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "No disponible",
                                color = FrutAppColors.Brand600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (sustituido) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Sustituido por ${item.sustitutoNombre}",
                                color = FrutAppColors.Brand600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (pesoAjustado) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Peso ajustado",
                                color = FrutAppColors.Brand600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    val fotosItem = evidencias.filter { it.orderItemId == item.id }
                    if (fotosItem.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            fotosItem.forEach { ev ->
                                EvidenciaThumb(ev = ev, onClick = { onEvidenciaClick(ev) })
                            }
                        }
                    }
                }
                Text(
                    text = formatClp(if (sinStock) 0 else (item.montoFinal ?: item.montoEstimado)),
                    color = if (sinStock) FrutAppColors.InkMuted else FrutAppColors.Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (sinStock) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
            }
        }
    }
}

@Composable
private fun EvidenciaThumb(ev: cl.frutapp.shared.dto.OrderItemEvidenceDto, onClick: () -> Unit) {
    var bytes by remember(ev.url) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(ev.url) {
        runCatching { cl.frutapp.app.platform.Imagenes.descargar(ev.url) }
            .onSuccess { bytes = it }
    }
    val img = bytes?.let { cl.frutapp.app.platform.decodeImagen(it) }
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (img != null) {
            Image(
                bitmap = img,
                contentDescription = "Foto del item",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(54.dp)
            )
        } else {
            Icon(
                Icons.Filled.PhotoCamera,
                null,
                tint = FrutAppColors.Brand600,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VisorEvidenciaFullscreen(evidencia: cl.frutapp.shared.dto.OrderItemEvidenceDto, onCerrar: () -> Unit) {
    var bytes by remember(evidencia.url) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(evidencia.url) {
        runCatching { cl.frutapp.app.platform.Imagenes.descargar(evidencia.url) }
            .onSuccess { bytes = it }
    }
    val img = bytes?.let { cl.frutapp.app.platform.decodeImagen(it) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(onClick = onCerrar),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (img != null) {
                Image(
                    bitmap = img,
                    contentDescription = "Foto del item",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
                )
            } else {
                Text("Cargando…", color = Color.White, fontSize = 14.sp)
            }
            evidencia.comentario?.let { c ->
                Spacer(Modifier.height(16.dp))
                Text(c, color = Color.White, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp))
            Text("Toca para cerrar", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}

private fun detalleItem(item: cl.frutapp.shared.dto.OrderItemDto): String {
    val unidad = item.unidad
    val gramosLocal = item.gramos
    // Pedido: "1 kg × 2" para kg; "3 unidad" para unidad/atado.
    val pedido = if (gramosLocal != null) {
        val peso = if (gramosLocal >= 1000) "${gramosLocal / 1000} kg" else "$gramosLocal g"
        "Pediste $peso × ${item.cantidad}"
    } else {
        "Pediste ${item.cantidad} $unidad"
    }
    val real = item.pesoReal?.let { gramosReales ->
        // Formato compacto para mostrar el peso real: 1234g -> "1.23 kg", 800g -> "800 g".
        val txt = if (gramosReales >= 1000) {
            val kg = gramosReales / 1000.0
            val redondeado = (kg * 100).toInt() / 100.0
            "$redondeado kg"
        } else {
            "$gramosReales g"
        }
        "Llegó $txt"
    }
    return if (real != null) "$pedido · $real" else pedido
}

/**
 * Mini-mapa con la ubicacion del repartidor para un pedido EN_DESPACHO.
 * Hace polling cada 8s para refrescar el marker. Si el repartidor todavia
 * no reporto (204 del backend), muestra mensaje "Esperando ubicacion" en
 * vez del marker — pero el mapa igual aparece (centrado en Santiago).
 *
 * Sin clave Google Maps (manifest sin MAPS_API_KEY), el GoogleMap se ve
 * gris. La app no crashea; el resto del tracking sigue siendo usable.
 */
@Composable
private fun MapaRepartidor(orderId: String, modifier: Modifier = Modifier) {
    val api = remember { cl.frutapp.app.data.remote.UbicacionApi() }
    var ubic by remember { mutableStateOf<cl.frutapp.shared.dto.UbicacionDto?>(null) }
    LaunchedEffect(orderId) {
        while (true) {
            runCatching { api.paraPedido(orderId) }
                .onSuccess { ubic = it }
            delay(8_000)
        }
    }
    Box(
        modifier = modifier.background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
    ) {
        // Default = centro de Santiago. Cuando llega la primera ubicacion
        // real, recentramos.
        val centerLat = ubic?.lat ?: -33.4489
        val centerLng = ubic?.lng ?: -70.6693
        cl.frutapp.app.platform.MapaCompose(
            centerLat = centerLat,
            centerLng = centerLng,
            markerLat = ubic?.lat,
            markerLng = ubic?.lng,
            zoom = 15f,
            modifier = Modifier.fillMaxSize()
        )
        if (ubic == null) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, end = 8.dp)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .align(Alignment.TopEnd)
            ) {
                Text("Esperando al repartidor…", color = FrutAppColors.Brand800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Badge rojo con el conteo de mensajes de chat no leidos. "99+" cuando se
 *  pasa de 99 para no romper layout. */
@Composable
private fun ChatUnreadDot(count: Int) {
    Box(
        modifier = Modifier
            .background(Color(0xFFDC2626), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Card destacada con el codigo de 4 digitos que el cliente debe darle al
 *  repartidor cara a cara para confirmar la entrega. Sin esto el repartidor
 *  no puede marcar entregado. */
@Composable
private fun CodigoEntregaCard(codigo: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.AmberSoft, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔐", fontSize = 18.sp)
            Text(
                "Tu código de entrega",
                color = FrutAppColors.AmberCoin,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            codigo.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        c.toString(),
                        color = FrutAppColors.AmberCoin,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Text(
            "Dáselo al repartidor cuando llegue para confirmar la entrega.",
            color = FrutAppColors.InkSoft,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
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
        "Tu Seleccionador de Frescura" to "Está eligiendo lo mejor para ti",
        "Tu Repartidor en camino" to "Lleva tu pedido a tu mesa",
        "¡Entregado!" to "Tus productos llegaron a tu mesa 🌿"
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
private fun TrackTopBar(onBack: () -> Unit, onAyuda: () -> Unit) {
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
        Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAyuda))
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
