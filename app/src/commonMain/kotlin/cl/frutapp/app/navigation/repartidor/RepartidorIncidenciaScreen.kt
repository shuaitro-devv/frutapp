package cl.frutapp.app.navigation.repartidor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.CircularProgressIndicator
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.isUuidLike
import cl.frutapp.app.data.remote.StaffDispatchApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.IconBubble
import cl.frutapp.app.ui.components.IncidenciaScaffold
import cl.frutapp.app.ui.components.MotivoSpec
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Incidencia de la entrega (boton 'Problema' en EnCamino/Entrega). Post-refactor delega
 * en [IncidenciaScaffold]; aca solo viven los motivos del lado calle (cliente ausente,
 * direccion incorrecta, pedido danado, cliente rechazo, otro) y la cabecera con cliente.
 *
 * Antes este archivo tenia 200+ lineas con TopBar, MotivoRow, FotoBtn, textarea
 * duplicado byte-a-byte con PickerIncidenciaScreen. Ahora ~55.
 */
class RepartidorIncidenciaScreen(private val pedidoId: String) : Screen {
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
                    ErrorReporter.report(screen = "RepartidorIncidencia", action = "fetch_detalle", error = e)
                    showToast("No pudimos cargar el pedido. Volvé a intentarlo.")
                    navigator.pop()
                }
        }
        val despacho = despachoState ?: run {
            Box(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FrutAppColors.Brand400)
            }
            return
        }

        IncidenciaScaffold(
            subtitulo = pedidoId,
            motivos = MOTIVOS_REPARTIDOR,
            cabecera = { CabeceraEntrega(despacho = despacho) },
            onBack = { navigator.pop() },
            onEnviar = { motivoKey, _ ->
                val motivoLabel = MOTIVOS_REPARTIDOR.first { it.key == motivoKey }.label
                showToast("Incidencia enviada (mock) - $motivoLabel")
                navigator.popUntilRoot()
            }
        )
    }
}

/** Catalogo de motivos del lado calle. OTRO primero para default no-destructivo. */
private val MOTIVOS_REPARTIDOR = listOf(
    MotivoSpec("OTRO", "Otro motivo", "Especifica el problema en el detalle.", Icons.Filled.MoreHoriz),
    MotivoSpec("AUSENTE", "Cliente ausente", "Nadie responde en la dirección.", Icons.Filled.PersonOff),
    MotivoSpec("DIRECCION", "Dirección incorrecta", "La dirección no existe o es incorrecta.", Icons.Filled.LocationOff),
    MotivoSpec("DANADO", "Pedido incompleto o dañado", "Faltan productos o vienen en mal estado.", Icons.Filled.Inventory),
    MotivoSpec("RECHAZADO", "Cliente rechazó el pedido", "El cliente no quiso recibir el pedido.", Icons.Filled.Cancel, destructiva = true)
)

@Composable
private fun CabeceraEntrega(despacho: DespachoItem) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            initial = despacho.cliente,
            size = 36.dp,
            bg = Color.White,
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Entrega a", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text(despacho.cliente, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${despacho.direccion}, ${despacho.sector}", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("En curso", color = Color(0xFFB91C1C), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Llegada estimada", color = FrutAppColors.InkSoft, fontSize = 10.sp)
            Text("10:25 - 10:40", color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
