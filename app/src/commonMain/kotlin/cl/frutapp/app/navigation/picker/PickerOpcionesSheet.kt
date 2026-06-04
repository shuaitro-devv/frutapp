package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapVerticalCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.launch

/**
 * Acciones contextuales del menu de 3 puntos del picklist y handoff. Se elige una y se
 * cierra el sheet. La pantalla padre decide que hacer con cada accion (toast por ahora,
 * navegacion cuando esten las pantallas reales).
 */
enum class PickerOpcion(val titulo: String, val detalle: String, val icono: ImageVector, val destructiva: Boolean = false) {
    PAUSAR("Pausar pedido", "Liberar y devolver a la cola para retomar despues", Icons.Filled.PauseCircle),
    REPORTAR("Reportar problema", "Escalar a soporte o supervisor", Icons.Filled.ReportProblem),
    REASIGNAR("Reasignar picker", "Pasar el pedido a otro miembro del equipo", Icons.Filled.SwapVerticalCircle),
    LLAMAR_SUPERVISOR("Llamar al supervisor", "Contacto directo", Icons.AutoMirrored.Filled.Chat),
    HISTORIAL("Ver historial del pedido", "Cambios, intentos, comentarios", Icons.Filled.History),
    CANCELAR("Cancelar pedido", "Marcarlo como cancelado (requiere motivo)", Icons.Filled.Cancel, destructiva = true)
}

/** Modal bottom sheet con las opciones del menu de 3 puntos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerOpcionesSheet(
    onCerrar: () -> Unit,
    onElegir: (PickerOpcion) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Fix #8: cerrar el sheet con animacion ANTES de invocar la accion. Antes el orden
    // era onElegir(); onCerrar() → cuando la accion navegaba, la sheet quedaba stranded
    // sin animacion de cierre. Ahora primero hide() del sheet (con coroutine), y al
    // completar la animacion ejecutamos onElegir.
    val seleccionarYCerrar: (PickerOpcion) -> Unit = { opcion ->
        scope.launch {
            sheetState.hide()
            onCerrar()
            onElegir(opcion)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Opciones del pedido", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Acciones contextuales", color = FrutAppColors.InkMuted, fontSize = 12.sp)
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = FrutAppColors.InkSoft)
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerOpcion.entries.forEach { opcion ->
                    OpcionRow(opcion = opcion, onClick = { seleccionarYCerrar(opcion) })
                }
            }
        }
    }
}

@Composable
private fun OpcionRow(opcion: PickerOpcion, onClick: () -> Unit) {
    val acento = if (opcion.destructiva) Color(0xFFB91C1C) else FrutAppColors.Brand600
    val bgIcono = if (opcion.destructiva) Color(0xFFFEE2E2) else FrutAppColors.Brand50
    val bordeColor = if (opcion.destructiva) Color(0xFFFCA5A5) else FrutAppColors.Brand100
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, bordeColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(bgIcono, CircleShape), contentAlignment = Alignment.Center) {
            Icon(opcion.icono, null, tint = acento, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(opcion.titulo, color = if (opcion.destructiva) acento else FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(opcion.detalle, color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
    }
}
