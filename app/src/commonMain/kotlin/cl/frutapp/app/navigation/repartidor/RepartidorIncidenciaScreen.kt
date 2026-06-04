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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PersonOff
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

private enum class MotivoIncidencia(val label: String, val detalle: String, val icon: ImageVector) {
    AUSENTE("Cliente ausente", "Nadie responde en la dirección.", Icons.Filled.PersonOff),
    DIRECCION("Dirección incorrecta", "La dirección no existe o es incorrecta.", Icons.Filled.LocationOff),
    DANADO("Pedido incompleto o dañado", "Faltan productos o vienen en mal estado.", Icons.Filled.Inventory),
    RECHAZADO("Cliente rechazó el pedido", "El cliente no quiso recibir el pedido.", Icons.Filled.Cancel),
    OTRO("Otro motivo", "Especifica el problema en el detalle.", Icons.Filled.MoreHoriz)
}

/** repartidor-05 — Reportar incidencia con motivo + detalle + fotos. */
class RepartidorIncidenciaScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val despacho = remember(pedidoId) { despachoPorId(pedidoId) }
        var motivo by remember { mutableStateOf(MotivoIncidencia.AUSENTE) }
        var detalle by remember { mutableStateOf("") }

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reportar incidencia", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Pedido ${despacho.id}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                EntregaCabeceraCard(despacho = despacho)
                Spacer(Modifier.height(14.dp))
                Text("¿Qué sucedió?", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Selecciona el motivo de la incidencia.", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                MotivoIncidencia.entries.forEach { m ->
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
            Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                FrutButtonPrimary(
                    text = "Enviar incidencia",
                    onClick = {
                        showToast("Incidencia enviada (mock)")
                        navigator.popUntilRoot()
                    }
                )
            }
        }
    }
}

@Composable
private fun EntregaCabeceraCard(despacho: DespachoItem) {
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

@Composable
private fun MotivoRow(motivo: MotivoIncidencia, seleccionado: Boolean, onClick: () -> Unit) {
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
