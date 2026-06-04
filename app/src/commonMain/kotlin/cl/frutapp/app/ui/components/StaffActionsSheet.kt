package cl.frutapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
 * Una accion del bottom sheet de 3 puntos. Data-driven: cada perfil (picker/repartidor/
 * admin) define su lista y se la pasa al sheet. La accion `destructiva=true` se pinta en
 * rojo (icono + borde + texto) para que sea visualmente distinta.
 *
 * `onClick` viene aqui porque facilita el dispatch — antes el callsite tenia que hacer
 * `when (opcion) { ... }`. Ahora cada accion sabe que hacer. Si una accion tiene varias
 * variantes contextuales (PAUSAR vs CANCELAR con confirm), la propia accion decide.
 */
data class StaffAction(
    val titulo: String,
    val detalle: String,
    val icono: ImageVector,
    val destructiva: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Bottom sheet generico para el menu de 3 puntos en pantallas staff. Animacion de cierre
 * correcta: hide() PRIMERO, luego onElegir(). Permite que la pantalla padre navegue sin
 * que el sheet quede stranded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffActionsSheet(
    titulo: String,
    detalle: String = "Acciones contextuales",
    acciones: List<StaffAction>,
    onCerrar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val ejecutar: (StaffAction) -> Unit = { accion ->
        scope.launch {
            sheetState.hide()
            onCerrar()
            accion.onClick()
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
                    Text(titulo, color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(detalle, color = FrutAppColors.InkMuted, fontSize = 12.sp)
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = FrutAppColors.InkSoft)
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                acciones.forEach { accion ->
                    AccionRow(accion = accion, onClick = { ejecutar(accion) })
                }
            }
        }
    }
}

@Composable
private fun AccionRow(accion: StaffAction, onClick: () -> Unit) {
    val acento = if (accion.destructiva) Color(0xFFB91C1C) else FrutAppColors.Brand600
    val bgIcono = if (accion.destructiva) Color(0xFFFEE2E2) else FrutAppColors.Brand50
    val bordeColor = if (accion.destructiva) Color(0xFFFCA5A5) else FrutAppColors.Brand100
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, bordeColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon = accion.icono, size = 40.dp, bg = bgIcono, fg = acento, iconSize = 20.dp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accion.titulo,
                color = if (accion.destructiva) acento else FrutAppColors.Brand800,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(accion.detalle, color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
    }
}
