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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import cl.frutapp.app.data.isUuidLike
import cl.frutapp.app.data.remote.StaffDispatchApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.AvatarImage
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.StaffActionsSheet
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * repartidor-03 — En camino. Hero verde con ETA, mapa con ruta, cards de cliente y
 * direccion, instruccion de entrega sin contacto y botones para reportar problema o
 * confirmar llegada al destino (avanza a la pantalla de confirmar entrega).
 */
class RepartidorEnCaminoScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val esBackendReal = remember(pedidoId) { pedidoId.isUuidLike() }
        val dispatchApi = remember { StaffDispatchApi() }
        var despachoState by remember(pedidoId) {
            mutableStateOf(if (esBackendReal) null else despachoPorId(pedidoId))
        }
        LaunchedEffect(pedidoId) {
            if (!esBackendReal) return@LaunchedEffect
            runCatching { dispatchApi.detalle(pedidoId) }
                .onSuccess { despachoState = it.toDespachoItem() }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "RepartidorEnCamino", action = "fetch_detalle", error = e)
                    // No caer al mock: el detalle es crítico (cliente, dirección).
                    // Mejor avisar y pop a En ruta para reintentar.
                    showToast("No pudimos cargar el despacho. Volvé a intentarlo.")
                    navigator.pop()
                }
        }

        // Tracking: el repartidor postea su ubicacion cada 10s mientras esta
        // en esta pantalla. El cliente la consume desde OrderTrackingScreen
        // para pintar el marker en su mapa. Si no hay permisos o el backend
        // rechaza (pedido cambio de estado), abortamos silenciosamente — no
        // bloqueamos al repartidor por algo del background.
        val location = cl.frutapp.app.platform.rememberLocationProvider()
        val ubicacionApi = remember { cl.frutapp.app.data.remote.UbicacionApi() }
        LaunchedEffect(pedidoId) {
            if (!esBackendReal) return@LaunchedEffect
            if (!location.solicitarPermiso()) return@LaunchedEffect
            while (true) {
                val coord = location.obtenerActual()
                if (coord != null) {
                    runCatching { ubicacionApi.reportar(pedidoId, coord.lat, coord.lng) }
                        .onFailure { e ->
                            if (e is kotlinx.coroutines.CancellationException) throw e
                            // No reportar al ErrorReporter cada falla: el polling
                            // genera mucho ruido. Solo logueamos via println para
                            // debug; el flujo del repartidor no se interrumpe.
                        }
                }
                kotlinx.coroutines.delay(10_000)
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
        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("En camino", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Pedido ${despacho.id}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
                }
                // Boton chat con el cliente, gated por feature.chat.
                val chatHabilitado = cl.frutapp.app.data.ConfigStore.featureEnabled("feature.chat")
                if (chatHabilitado && esBackendReal) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                navigator.push(cl.frutapp.app.navigation.shop.ChatScreen(
                                    orderId = pedidoId,
                                    destinatarioRol = "cliente",
                                    tituloContraparte = "Cliente del pedido",
                                ))
                            }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
                    }
                }
                Row(
                    modifier = Modifier.clickable { menuAbierto = true }.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.MoreVert, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                HeroEnCamino()
                Spacer(Modifier.height(12.dp))
                MapaConRuta(kmRestantes = 1.8)
                Spacer(Modifier.height(12.dp))
                ClienteEntregaCard(despacho = despacho)
                Spacer(Modifier.height(10.dp))
                DireccionCard(direccion = "${despacho.direccion}, Depto 54", subdireccion = "Ñuñoa, Santiago")
                Spacer(Modifier.height(10.dp))
                EntregaSinContacto()
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrutButtonOutline(text = "Problema", onClick = { navigator.push(RepartidorIncidenciaScreen(pedidoId)) }, modifier = Modifier.weight(1f))
                FrutButtonPrimary(text = "Llegué al destino", onClick = { navigator.replace(RepartidorEntregaScreen(pedidoId)) }, modifier = Modifier.weight(1.4f))
            }
        }
        if (menuAbierto) {
            StaffActionsSheet(
                titulo = "Opciones de la entrega",
                acciones = accionesRepartidor(
                    onPausar = { showToast("Entrega pausada - Próximamente") },
                    onReportar = { navigator.push(RepartidorIncidenciaScreen(pedidoId)) },
                    onCambiarDireccion = { showToast("Cambiar dirección - Próximamente") },
                    onLlamarCliente = { showToast("Llamar - Próximamente") },
                    onChatCliente = { showToast("Chat - Próximamente") },
                    onHistorial = { showToast("Historial - Próximamente") },
                    onCancelar = { dialogoCancelar = true }
                ),
                onCerrar = { menuAbierto = false }
            )
        }
        if (dialogoCancelar) {
            AlertDialog(
                onDismissRequest = { dialogoCancelar = false },
                icon = { Icon(Icons.Filled.Cancel, null, tint = Color(0xFFB91C1C)) },
                title = { Text("¿Cancelar entrega?", fontWeight = FontWeight.Bold) },
                text = { Text("La entrega se marcará como cancelada y deberá registrarse el motivo a soporte.") },
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
private fun HeroEnCamino() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DeliveryDining, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Vas en camino", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Llegada estimada", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Row(
                modifier = Modifier.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Traffic, null, tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text("Tránsito normal", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("10:25 - 10:40", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MapaConRuta(kmRestantes: Double) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = Brush.verticalGradient(listOf(FrutAppColors.Brand50, FrutAppColors.Brand100)),
                shape = RoundedCornerShape(14.dp)
            )
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Pin(icon = Icons.Filled.DeliveryDining, "Tú")
            Box(modifier = Modifier.weight(1f).height(3.dp).background(FrutAppColors.Brand600))
            Pin(icon = Icons.Filled.Place, "Destino")
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("$kmRestantes km restantes", color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Pin(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand600, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = FrutAppColors.Brand800, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ClienteEntregaCard(despacho: DespachoItem) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            url = despacho.avatarUrl,
            initial = despacho.cliente.take(1).uppercase(),
            size = 40.dp,
            background = FrutAppColors.Brand50,
            initialColor = FrutAppColors.Brand600
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Cliente", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text(despacho.cliente, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ContactBtn(icon = Icons.Filled.Phone)
            ContactBtn(icon = Icons.AutoMirrored.Filled.Chat)
        }
    }
}

@Composable
private fun ContactBtn(icon: ImageVector) {
    Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape).clickable { }, contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun DireccionCard(direccion: String, subdireccion: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(direccion, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subdireccion, color = FrutAppColors.InkMuted, fontSize = 12.sp)
        }
        Row(
            modifier = Modifier.background(FrutAppColors.Brand50, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp).clickable { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Storefront, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Ver en mapa", color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EntregaSinContacto() {
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand100, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.NoEncryption, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Entrega sin contacto", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Dejar en la puerta del edificio. Tocar timbre 2 veces.", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
    }
}
