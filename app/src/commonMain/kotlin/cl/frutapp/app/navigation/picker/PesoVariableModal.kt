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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Modal de peso variable (picker-03). Se abre al tocar el check de un item con
 * `pesoVariable=true`. Muestra la cantidad solicitada y un stepper grande para que el
 * picker confirme el peso real entregado. Al confirmar marca el item como completado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesoVariableModal(item: ItemPicklist, onCerrar: () -> Unit, onConfirmar: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pesoReal by remember { mutableStateOf(item.cantidad) }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Peso variable",
                    color = FrutAppColors.Brand800,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = FrutAppColors.InkSoft)
                }
            }
            Spacer(Modifier.height(16.dp))
            // Foto + nombre + chip
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(110.dp).background(FrutAppColors.Brand50, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(item.emoji, fontSize = 64.sp) }
                    Spacer(Modifier.height(12.dp))
                    Text(item.nombre, color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .background(FrutAppColors.Brand50, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Scale, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Peso variable", color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Cantidad solicitada: ${formatoCant(item.cantidad)} ${item.unidad}",
                        color = FrutAppColors.InkMuted,
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Peso real entregado",
                color = FrutAppColors.Brand800,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            // Stepper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperBtn(icon = Icons.Filled.Remove, onClick = { pesoReal = (pesoReal - 0.1).coerceAtLeast(0.1).redondear() })
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${formatoCant(pesoReal)} ${item.unidad}", color = FrutAppColors.Brand800, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                StepperBtn(icon = Icons.Filled.Add, onClick = { pesoReal = (pesoReal + 0.1).redondear() })
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ajusta el peso final para calcular el cobro correcto.",
                color = FrutAppColors.InkSoft,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FrutAppColors.Brand50, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Se actualizará el total del pedido",
                    color = FrutAppColors.Brand800,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FrutButtonOutline(text = "Cancelar", onClick = onCerrar, modifier = Modifier.weight(1f))
                FrutButtonPrimary(text = "Confirmar", onClick = onConfirmar, modifier = Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
private fun StepperBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .background(Color.White, CircleShape)
            .border(1.5.dp, FrutAppColors.Brand400, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(26.dp))
    }
}

private fun Double.redondear(): Double = (this * 10).toInt() / 10.0
