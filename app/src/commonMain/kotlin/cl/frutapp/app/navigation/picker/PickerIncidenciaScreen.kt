package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.IconBubble
import cl.frutapp.app.ui.components.IncidenciaScaffold
import cl.frutapp.app.ui.components.MotivoSpec
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Incidencia global del pedido (boton 'Reportar' del picklist). Despues del refactor
 * delega TODO el chrome (top bar, lista, textarea, fotos, boton enviar) a
 * [IncidenciaScaffold]; aca solo viven los motivos especificos del lado bodega y la
 * cabecera del pedido (no del cliente — esto es lado picker).
 */
class PickerIncidenciaScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val data = remember(pedidoId) { picklistMock(pedidoId) }

        IncidenciaScaffold(
            subtitulo = pedidoId,
            motivos = MOTIVOS_PICKER,
            cabecera = { CabeceraPedido(data = data) },
            onBack = { navigator.pop() },
            onEnviar = { motivoKey, _ ->
                val motivoLabel = MOTIVOS_PICKER.first { it.key == motivoKey }.label
                showToast("Incidencia enviada (mock) - $motivoLabel")
                navigator.popUntilRoot()
            }
        )
    }
}

/** Catalogo de motivos del picker. `OTRO` viene primero para que sea el default no
 *  destructivo (IncidenciaScaffold elige el primer no-destructivo). Antes el default
 *  era CANCELACION y un Enviar accidental cancelaba el pedido. */
private val MOTIVOS_PICKER = listOf(
    MotivoSpec("OTRO", "Otro motivo", "Especifica en el detalle.", Icons.Filled.MoreHoriz),
    MotivoSpec("ERROR_SISTEMA", "Error en el sistema", "Falla técnica que impide continuar (precios, stock, etc).", Icons.Filled.BugReport),
    MotivoSpec("PEDIDO_DUPLICADO", "Pedido duplicado", "Este pedido aparece duplicado en la cola.", Icons.Filled.ContentCopy),
    MotivoSpec("PRODUCTOS_DANADOS", "Productos dañados en bodega", "Stock recibido en mal estado, no se puede usar.", Icons.Filled.BrokenImage),
    MotivoSpec("FALTA_STOCK", "Falta de stock generalizada", "No hay reposicion suficiente para varios items.", Icons.Filled.Inventory),
    MotivoSpec("CANCELACION", "Cliente canceló", "El cliente quiere anular este pedido antes de salir.", Icons.Filled.Cancel, destructiva = true)
)

@Composable
private fun CabeceraPedido(data: PicklistData) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            icon = Icons.Filled.Inventory,
            size = 36.dp,
            bg = Color.White,
            shape = RoundedCornerShape(10.dp),
            iconSize = 18.dp
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Pedido", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text(data.pedidoId, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${data.sector} · ${data.destino}", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("En preparación", color = EstadoPaleta.reducido, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("${data.totalItems} items", color = FrutAppColors.InkSoft, fontSize = 10.sp)
        }
    }
}
