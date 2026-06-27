package cl.frutapp.app.navigation.repartidor

import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.launch

/**
 * repartidor-04 — Confirmar entrega. Cliente, instrucciones, codigo de verificacion 4
 * digitos en cajas grandes, acciones de tomar foto / firma, resumen, stepper de estado
 * casi completo y botones de problema o confirmar.
 */
class RepartidorEntregaScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
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
                    ErrorReporter.report(screen = "RepartidorEntrega", action = "fetch_detalle", error = e)
                    showToast("No pudimos cargar los datos de entrega. Vuelve a intentarlo.")
                    navigator.pop()
                }
        }
        val despacho = despachoState ?: run {
            Box(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FrutAppColors.Brand400)
            }
            return
        }
        // El repartidor tipea aca los 4 digitos que el cliente le dice cara
        // a cara. El backend valida match contra el delivery_code generado al
        // EN_DESPACHO; si no coincide, devuelve 400 "Codigo incorrecto" y
        // mostramos el error sin transicionar el pedido.
        var codigoInput by remember { mutableStateOf("") }
        var entregando by remember { mutableStateOf(false) }
        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Confirmar entrega", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Pedido ${despacho.id}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.background(FrutAppColors.Brand50, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("En destino", color = FrutAppColors.Brand800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                ClienteCard(despacho = despacho)
                Spacer(Modifier.height(10.dp))
                InstruccionesCard()
                Spacer(Modifier.height(14.dp))
                Text("Código de verificación", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Pídele al cliente el código de 4 dígitos que ve en su app", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                CodigoInputBoxes(codigo = codigoInput, onCodigo = { codigoInput = it })
                Spacer(Modifier.height(14.dp))
                AccionCard(icon = Icons.Filled.CameraAlt, titulo = "Tomar foto", sub = "Toma foto al paquete entregado", onClick = { showToast("Cámara - Próximamente") })
                Spacer(Modifier.height(8.dp))
                AccionCard(icon = Icons.Filled.Draw, titulo = "Firma del receptor", sub = "Solicitar firma en la pantalla", onClick = { showToast("Firma - Próximamente") })
                Spacer(Modifier.height(12.dp))
                ResumenPedidoCard(items = despacho.items, unidades = despacho.unidades)
                Spacer(Modifier.height(12.dp))
                EstadoStepper(activo = 3)
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrutButtonOutline(text = "Problema", onClick = { navigator.push(RepartidorIncidenciaScreen(pedidoId)) }, modifier = Modifier.weight(1f))
                FrutButtonPrimary(
                    text = if (entregando) "Confirmando..." else "Confirmar entrega",
                    enabled = !entregando && codigoInput.length == 4,
                    onClick = {
                        if (entregando) return@FrutButtonPrimary
                        if (codigoInput.length != 4) {
                            showToast("Pídele al cliente el código de 4 dígitos.")
                            return@FrutButtonPrimary
                        }
                        if (!esBackendReal) {
                            navigator.popUntilRoot()
                            return@FrutButtonPrimary
                        }
                        entregando = true
                        scope.launch {
                            runCatching { dispatchApi.delivered(pedidoId, codigoInput) }
                                .onSuccess {
                                    showToast("¡Entrega confirmada! 🌿")
                                    navigator.popUntilRoot()
                                }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "RepartidorEntrega", action = "delivered", error = e)
                                    showToast(mensajeAmigable(e, "confirmar la entrega"))
                                    entregando = false
                                }
                        }
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

@Composable
private fun ClienteCard(despacho: DespachoItem) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Text(despacho.cliente, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("${despacho.direccion}, ${despacho.sector}", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InstruccionesCard() {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Text("Instrucciones de entrega", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Entregar en recepción · Llamar al llegar", color = FrutAppColors.InkSoft, fontSize = 12.sp)
    }
}

@Composable
private fun CodigoBoxes(codigo: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        codigo.forEach { digito ->
            Box(
                modifier = Modifier.weight(1f).height(64.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(2.dp, FrutAppColors.Brand400, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("$digito", color = FrutAppColors.Brand800, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 4 cajas editables que reflejan el codigo que el repartidor escribe. El
 *  TextField real es invisible (alpha 0 sobre las cajas), asi el teclado
 *  funciona pero el render sigue siendo el de las cajas grandes. */
@Composable
private fun CodigoInputBoxes(codigo: String, onCodigo: (String) -> Unit) {
    androidx.compose.foundation.layout.Box {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) { i ->
                val digito = codigo.getOrNull(i)?.toString() ?: ""
                val activa = i == codigo.length
                Box(
                    modifier = Modifier.weight(1f).height(64.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(
                            width = if (activa) 2.dp else 1.dp,
                            color = if (activa) FrutAppColors.Brand600 else FrutAppColors.Brand200,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(digito, color = FrutAppColors.Brand800, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // TextField invisible que captura el teclado. Solo digitos, max 4.
        androidx.compose.foundation.text.BasicTextField(
            value = codigo,
            onValueChange = { nuevo ->
                val limpio = nuevo.filter { it.isDigit() }.take(4)
                onCodigo(limpio)
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .alpha(0f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
        )
    }
}

@Composable
private fun AccionCard(icon: ImageVector, titulo: String, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResumenPedidoCard(items: Int, unidades: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Inventory2, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Resumen del pedido", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("$items productos · $unidades unidades", color = FrutAppColors.InkSoft, fontSize = 12.sp)
            Text("Sin incidencias", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
