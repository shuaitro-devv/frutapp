package cl.frutapp.app.navigation.repartidor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.material.icons.filled.Cancel
import cl.frutapp.app.data.isUuidLike
import cl.frutapp.app.data.remote.StaffDispatchApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.StaffActionsSheet
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.launch

/**
 * repartidor-02 — Detalle del despacho asignado. Mapa placeholder con origen+destino, stats
 * de ruta, cliente con call/chat, retiro (origen), items resumidos, instrucciones, stepper
 * de estado y boton "Iniciar retiro" que avanza a la pantalla 'en camino'.
 */
class RepartidorDetalleScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val esBackendReal = remember(pedidoId) { pedidoId.isUuidLike() }
        val dispatchApi = remember { StaffDispatchApi() }
        var despachoState by remember(pedidoId) {
            mutableStateOf(if (esBackendReal) null else despachoPorId(pedidoId))
        }
        var tomando by remember(pedidoId) { mutableStateOf(false) }
        LaunchedEffect(pedidoId) {
            if (!esBackendReal) return@LaunchedEffect
            runCatching { dispatchApi.detalle(pedidoId) }
                .onSuccess { dto ->
                    despachoState = DespachoItem(
                        id = dto.numero,
                        cliente = dto.clienteNombre,
                        sector = dto.sector,
                        direccion = dto.direccion,
                        kmDistancia = 3.0,
                        minutosEntrega = 30,
                        prioridad = PrioridadDespacho.MEDIA,
                        items = dto.items.size,
                        unidades = dto.items.size,
                        backendId = dto.id,
                        telefono = dto.telefono
                    )
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "RepartidorDetalle", action = "fetch_detalle", error = e)
                    despachoState = despachoPorId(pedidoId)
                    showToast("No pudimos cargar el detalle, mostrando vista parcial.")
                }
        }
        val despacho = despachoState ?: run {
            Box(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FrutAppColors.Brand400)
            }
            return
        }
        var menuAbierto by remember { mutableStateOf(false) }
        var dialogoCancelar by remember { mutableStateOf(false) }
        var verItemsAbierto by remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            TopBar(estado = "Listo para retiro", onBack = { navigator.pop() }, onMenu = { menuAbierto = true })
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(despacho.id, color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                MapaPlaceholder(direccionDestino = despacho.direccion)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(icon = Icons.Filled.AccessTime, valor = "${despacho.minutosEntrega} min", label = "Estimado", modifier = Modifier.weight(1f))
                    StatCard(icon = Icons.Filled.Route, valor = "${despacho.kmDistancia} km", label = "Distancia", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                ClienteCard(despacho = despacho)
                Spacer(Modifier.height(12.dp))
                RetiroCard()
                Spacer(Modifier.height(12.dp))
                ItemsCard(items = despacho.items, unidades = despacho.unidades)
                Spacer(Modifier.height(12.dp))
                InstruccionesCard()
                Spacer(Modifier.height(12.dp))
                EstadoStepper(activo = 0)
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrutButtonOutline(text = "Ver items", onClick = { verItemsAbierto = true }, modifier = Modifier.weight(1f))
                FrutButtonPrimary(
                    text = if (tomando) "Tomando..." else "Iniciar retiro",
                    onClick = {
                        if (tomando) return@FrutButtonPrimary
                        if (!esBackendReal) {
                            navigator.replace(RepartidorEnCaminoScreen(pedidoId))
                            return@FrutButtonPrimary
                        }
                        tomando = true
                        scope.launch {
                            runCatching { dispatchApi.take(pedidoId) }
                                .onSuccess { res ->
                                    if (res.ok) {
                                        showToast("Retiro iniciado 🚚")
                                        navigator.replace(RepartidorEnCaminoScreen(pedidoId))
                                    } else {
                                        showToast("Otro repartidor ya tomó este pedido.")
                                        navigator.pop()
                                    }
                                }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "RepartidorDetalle", action = "take", error = e)
                                    showToast(mensajeAmigable(e, "tomar el despacho"))
                                    tomando = false
                                }
                        }
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
        if (menuAbierto) {
            StaffActionsSheet(
                titulo = "Opciones de la entrega",
                acciones = accionesRepartidor(
                    onPausar = { showToast("Entrega pausada - Próximamente"); navigator.popUntilRoot() },
                    onReportar = { navigator.push(RepartidorIncidenciaScreen(pedidoId)) },
                    onCambiarDireccion = { showToast("Solicitar cambio de dirección - Próximamente") },
                    onLlamarCliente = { showToast("Llamar al cliente - Próximamente") },
                    onChatCliente = { showToast("Abrir chat - Próximamente") },
                    onHistorial = { showToast("Ver historial - Próximamente") },
                    onCancelar = { dialogoCancelar = true }
                ),
                onCerrar = { menuAbierto = false }
            )
        }
        if (verItemsAbierto) {
            RepartidorItemsSheet(pedidoId = pedidoId, onCerrar = { verItemsAbierto = false })
        }
        if (dialogoCancelar) {
            AlertDialog(
                onDismissRequest = { dialogoCancelar = false },
                icon = { Icon(Icons.Filled.Cancel, null, tint = Color(0xFFB91C1C)) },
                title = { Text("¿Cancelar entrega?", fontWeight = FontWeight.Bold) },
                text = { Text("La entrega se marcará como cancelada y deberá registrarse el motivo a soporte. Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        dialogoCancelar = false
                        showToast("Entrega cancelada (mock)")
                        navigator.popUntilRoot()
                    }) { Text("Sí, cancelar", color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { dialogoCancelar = false }) { Text("Volver") }
                }
            )
        }
    }
}

@Composable
private fun TopBar(estado: String, onBack: () -> Unit, onMenu: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
                .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(FrutAppColors.Brand600, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(estado, color = FrutAppColors.Brand800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.MoreVert, "Más", tint = FrutAppColors.Brand800)
        }
    }
}

@Composable
private fun MapaPlaceholder(direccionDestino: String) {
    // Placeholder visual del mapa. Cuando se integre Maps real, reemplazar por GoogleMap.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                brush = Brush.verticalGradient(listOf(FrutAppColors.Brand50, FrutAppColors.Brand100)),
                shape = RoundedCornerShape(14.dp)
            )
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
    ) {
        // Linea de ruta decorativa
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PinMapa(icon = Icons.Filled.Storefront, label = "Bodega")
                Box(modifier = Modifier.weight(1f).height(2.dp).background(FrutAppColors.Brand400))
                PinMapa(icon = Icons.Filled.Place, label = direccionDestino.take(14) + "…")
            }
        }
    }
}

@Composable
private fun PinMapa(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(36.dp).background(FrutAppColors.Brand600, CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(label, color = FrutAppColors.Brand800, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatCard(icon: ImageVector, valor: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(valor, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ClienteCard(despacho: DespachoItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
                Text(despacho.cliente.take(1).uppercase(), color = FrutAppColors.Brand600, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Cliente", color = FrutAppColors.InkMuted, fontSize = 11.sp)
                Text(despacho.cliente, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${despacho.direccion}, ${despacho.sector}", color = FrutAppColors.InkSoft, fontSize = 12.sp)
            }
            ContactoBtn(icon = Icons.Filled.Phone)
            Spacer(Modifier.width(6.dp))
            ContactoBtn(icon = Icons.AutoMirrored.Filled.Chat)
        }
    }
}

@Composable
private fun ContactoBtn(icon: ImageVector) {
    Box(
        modifier = Modifier.size(38.dp).background(FrutAppColors.Brand50, CircleShape).clickable { },
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun RetiroCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Storefront, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Retiro", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text("Bodega FrutApp - Sector Norte", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Camino El Bosque 987, Santiago", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ItemsCard(items: Int, unidades: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Inventory2, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Items", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text("$items productos · $unidades unidades", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Sin incidencias", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Row {
            Text("🥑", fontSize = 22.sp)
            Text("🥬", fontSize = 22.sp)
            Spacer(Modifier.width(4.dp))
            Box(modifier = Modifier.background(FrutAppColors.Brand50, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("+10", color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun InstruccionesCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text("Instrucciones de entrega", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        BulletText("Entregar en recepción")
        BulletText("Llamar al llegar")
    }
}

@Composable
private fun BulletText(t: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(4.dp).background(FrutAppColors.Brand400, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(t, color = FrutAppColors.Ink, fontSize = 12.sp)
    }
}

@Composable
internal fun EstadoStepper(activo: Int) {
    // 5 etapas alineadas al flujo del repartidor:
    //  0 Pedido preparado (cliente pago, stock confirmado por el picker)
    //  1 Retirado          (el repartidor lo retiro de la sucursal)
    //  2 En ruta           (camino al cliente)
    //  3 En destino        (llego, esta entregando — antes era "En espera"
    //                       que sonaba pasivo y no calzaba con la pantalla de
    //                       Confirmar entrega donde se usa con activo=3)
    //  4 Entregado         (codigo verificado / firma / foto)
    val etapas = listOf("Pedido preparado", "Retirado", "En ruta", "En destino", "Entregado")
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Text("Estado del pedido", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        // verticalAlignment = Top porque los labels tienen alturas distintas
        // ("Pedido preparado" wrappea a 2 lineas, "Retirado" no) y con Center
        // los circulos quedaban desalineados. La linea separadora va con
        // padding superior = 11.dp para quedar en el medio del circulo de 24dp.
        Row(verticalAlignment = Alignment.Top) {
            etapas.forEachIndexed { i, label ->
                val completada = i < activo
                val esActivo = i == activo
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(24.dp).background(
                            color = if (completada || esActivo) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                            shape = CircleShape
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (completada) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        else if (esActivo) Box(modifier = Modifier.size(8.dp).background(Color.White, CircleShape))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        color = if (esActivo) FrutAppColors.Brand800 else FrutAppColors.InkMuted,
                        fontSize = 9.sp,
                        fontWeight = if (esActivo) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                if (i < etapas.lastIndex) {
                    Box(modifier = Modifier
                        .weight(0.3f)
                        .padding(top = 11.dp)
                        .height(2.dp)
                        .background(if (i < activo) FrutAppColors.Brand400 else FrutAppColors.Brand100))
                }
            }
        }
    }
}
