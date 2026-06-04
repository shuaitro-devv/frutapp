package cl.frutapp.app.navigation.picker

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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Incidencia global del pedido (boton 'Reportar' del picklist). Distinto de la resolucion
 * por item (que pasa por el modal de sustitucion): aca el picker reporta algo que afecta
 * al pedido completo y lo escala a soporte/supervisor.
 *
 * Espejo conceptual de RepartidorIncidenciaScreen pero con motivos del lado bodega.
 */
private enum class MotivoIncidenciaPicker(val label: String, val detalle: String, val icon: ImageVector) {
    CANCELACION("Cliente canceló", "El cliente quiere anular este pedido antes de salir.", Icons.Filled.Cancel),
    ERROR_SISTEMA("Error en el sistema", "Falla técnica que impide continuar (precios, stock, etc).", Icons.Filled.BugReport),
    PEDIDO_DUPLICADO("Pedido duplicado", "Este pedido aparece duplicado en la cola.", Icons.Filled.ContentCopy),
    PRODUCTOS_DANADOS("Productos dañados en bodega", "Stock recibido en mal estado, no se puede usar.", Icons.Filled.BrokenImage),
    FALTA_STOCK("Falta de stock generalizada", "No hay reposicion suficiente para varios items.", Icons.Filled.Inventory),
    OTRO("Otro motivo", "Especifica en el detalle.", Icons.Filled.MoreHoriz)
}

class PickerIncidenciaScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val data = remember(pedidoId) { picklistMock(pedidoId) }
        var motivo by remember { mutableStateOf(MotivoIncidenciaPicker.CANCELACION) }
        var detalle by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reportar incidencia", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(pedidoId, color = FrutAppColors.InkMuted, fontSize = 11.sp)
                }
                Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                CabeceraPedido(data = data)
                Spacer(Modifier.height(14.dp))
                Text("¿Qué sucedió?", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Esto afecta al pedido completo, no a un item puntual.", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                MotivoIncidenciaPicker.entries.forEach { m ->
                    MotivoRow(motivo = m, seleccionado = motivo == m, onClick = { motivo = m })
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(4.dp))
                Text("Agrega más detalles (opcional)", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                FrutTextField(
                    value = detalle,
                    onValueChange = { if (it.length <= 200) detalle = it },
                    label = "Cuéntanos qué pasó…"
                )
                Text("${detalle.length}/200", color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                Spacer(Modifier.height(12.dp))
                Text("Agrega fotos (opcional)", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Esto nos ayuda a resolver más rápido.", color = FrutAppColors.InkSoft, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FotoBtn(icon = Icons.Filled.CameraAlt, label = "Tomar foto", modifier = Modifier.weight(1f))
                    FotoBtn(icon = Icons.Filled.PhotoLibrary, label = "Elegir de galería", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(20.dp))
            }
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp)) {
                FrutButtonPrimary(
                    text = "Enviar incidencia",
                    onClick = {
                        showToast("Incidencia enviada (mock) - ${motivo.label}")
                        // El pedido sale de la cola del picker. En real va a supervisor/soporte
                        // y queda en estado 'En revision'; el picker vuelve a su cola.
                        navigator.popUntilRoot()
                    }
                )
            }
        }
    }
}

@Composable
private fun CabeceraPedido(data: PicklistData) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(Color.White, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Inventory, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Pedido", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text(data.pedidoId, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${data.sector} · ${data.destino}", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("En preparación", color = Color(0xFFD97706), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("${data.totalItems} items", color = FrutAppColors.InkSoft, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MotivoRow(motivo: MotivoIncidenciaPicker, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(
                width = if (seleccionado) 2.dp else 1.dp,
                color = if (seleccionado) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(motivo.icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(motivo.label, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(motivo.detalle, color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier.size(20.dp).border(2.dp, if (seleccionado) FrutAppColors.Brand400 else FrutAppColors.Brand100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionado) Box(modifier = Modifier.size(10.dp).background(FrutAppColors.Brand400, CircleShape))
        }
    }
}

@Composable
private fun FotoBtn(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(80.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
            .clickable { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
