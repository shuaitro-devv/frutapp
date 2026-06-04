package cl.frutapp.app.ui.components

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
import androidx.compose.material.icons.filled.CameraAlt
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
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Un motivo de incidencia. Cada perfil define su lista de motivos (picker tiene
 * CANCELACION / ERROR_SISTEMA / etc; repartidor tiene CLIENTE_AUSENTE / DIRECCION /
 * etc), pero la pantalla es la misma.
 */
data class MotivoSpec(
    val key: String,
    val label: String,
    val detalle: String,
    val icon: ImageVector,
    val destructiva: Boolean = false
)

/**
 * Pantalla generica de reportar incidencia. Antes PickerIncidenciaScreen y
 * RepartidorIncidenciaScreen tenian 200+ lineas casi-identicas (top bar, cabecera del
 * pedido, lista de motivos radio, textarea, fotos, boton enviar). Ahora ambos perfiles
 * proveen solo: el subtitulo, la cabecera composable (cliente o pedido) y la lista de
 * motivos. La pantalla cuida insets, scroll, take(200), y default no-destructivo.
 */
@Composable
fun IncidenciaScaffold(
    subtitulo: String,
    motivos: List<MotivoSpec>,
    cabecera: @Composable () -> Unit,
    onBack: () -> Unit,
    onEnviar: (motivoKey: String, detalle: String) -> Unit
) {
    // Default no-destructivo: si hay al menos un motivo NO destructivo, elegimos el
    // primero de esos (evita el fix #3 del code-review — antes default era 'CANCELACION'
    // y un tap accidental cancelaba). Si todos son destructivos (raro), cae al primero.
    val defaultMotivo = remember(motivos) {
        motivos.firstOrNull { !it.destructiva } ?: motivos.first()
    }
    var motivo by remember(motivos) { mutableStateOf(defaultMotivo) }
    var detalle by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Reportar incidencia", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitulo, color = FrutAppColors.InkMuted, fontSize = 11.sp)
            }
            Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            cabecera()
            Spacer(Modifier.height(14.dp))
            Text("¿Qué sucedió?", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Selecciona el motivo de la incidencia.", color = FrutAppColors.InkSoft, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            motivos.forEach { m ->
                MotivoRow(motivo = m, seleccionado = motivo.key == m.key, onClick = { motivo = m })
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text("Agrega más detalles (opcional)", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            FrutTextField(
                value = detalle,
                onValueChange = { detalle = it.take(200) },
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
                onClick = { onEnviar(motivo.key, detalle) }
            )
        }
    }
}

@Composable
private fun MotivoRow(motivo: MotivoSpec, seleccionado: Boolean, onClick: () -> Unit) {
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
        IconBubble(icon = motivo.icon, size = 36.dp, iconSize = 18.dp)
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
