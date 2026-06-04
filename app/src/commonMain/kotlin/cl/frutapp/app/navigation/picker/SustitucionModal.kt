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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapHoriz
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

private enum class TipoSustitucion { SUSTITUIR, REDUCIR, FALTANTE }

private fun TipoSustitucion.aEstado(): EstadoItem = when (this) {
    TipoSustitucion.SUSTITUIR -> EstadoItem.SUSTITUIDO
    TipoSustitucion.REDUCIR -> EstadoItem.REDUCIDO
    TipoSustitucion.FALTANTE -> EstadoItem.FALTANTE
}

private data class Alternativa(val emoji: String, val nombre: String, val cantidad: String, val stock: String, val sufStock: Boolean)

/**
 * Modal de sustitucion / faltante (picker-04). Tres opciones expandibles:
 * - Sustituir por similar: muestra una lista de alternativas (radio).
 * - Reducir cantidad: indicar cuanto entregar.
 * - Reportar faltante: no hay alternativas.
 *
 * [onConfirmar] recibe el [EstadoItem] resultante (SUSTITUIDO/REDUCIDO/FALTANTE) para que
 * el picklist actualice el estado del item — asi cada item del pedido siempre termina
 * 'resuelto' y el handoff sabe el desglose real.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SustitucionModal(item: ItemPicklist, onCerrar: () -> Unit, onConfirmar: (EstadoItem) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tipo by remember { mutableStateOf(TipoSustitucion.SUSTITUIR) }
    var alternativaSel by remember { mutableStateOf(0) }

    val alternativas = remember(item) {
        listOf(
            Alternativa("🥑", "${item.nombre} Fuerte", "${formatoCant(item.cantidad)} ${item.unidad}", "Disponible", true),
            Alternativa("🥑", "${item.nombre} madura", "${formatoCant(item.cantidad - 0.2)} ${item.unidad}", "Suficiente stock", true),
            Alternativa("🥑", "Mix ${item.nombre.split(" ").first()} / similar", "${formatoCant(item.cantidad)} ${item.unidad}", "Disponible", true)
        )
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
                Text(
                    text = "Sustitución o faltante",
                    color = FrutAppColors.Brand800,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = FrutAppColors.InkSoft)
                }
            }
            Spacer(Modifier.height(12.dp))
            // Cabecera del item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FrutAppColors.Brand50, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(item.emoji, fontSize = 26.sp) }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.nombre, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("${formatoCant(item.cantidad)} ${item.unidad}", color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Pasillo ${item.pasillo} · Estante ${item.estante}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            // Opciones
            OpcionExpandible(
                seleccionada = tipo == TipoSustitucion.SUSTITUIR,
                onSelect = { tipo = TipoSustitucion.SUSTITUIR },
                icon = Icons.Filled.SwapHoriz,
                titulo = "1. Sustituir por similar",
                subtitulo = "Elige una alternativa equivalente disponible."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    alternativas.forEachIndexed { idx, alt ->
                        AlternativaRow(
                            alt = alt,
                            seleccionada = alternativaSel == idx,
                            onClick = { alternativaSel = idx }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OpcionExpandible(
                seleccionada = tipo == TipoSustitucion.REDUCIR,
                onSelect = { tipo = TipoSustitucion.REDUCIR },
                icon = Icons.Filled.RemoveCircleOutline,
                titulo = "2. Reducir cantidad",
                subtitulo = "Entregar menos de lo solicitado."
            )
            Spacer(Modifier.height(8.dp))
            OpcionExpandible(
                seleccionada = tipo == TipoSustitucion.FALTANTE,
                onSelect = { tipo = TipoSustitucion.FALTANTE },
                icon = Icons.Filled.ReportProblem,
                titulo = "3. Reportar faltante",
                subtitulo = "No hay reemplazo disponible."
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FrutButtonOutline(text = "Cancelar", onClick = onCerrar, modifier = Modifier.weight(1f))
                FrutButtonPrimary(text = "Confirmar acción", onClick = { onConfirmar(tipo.aEstado()) }, modifier = Modifier.weight(1.4f))
            }
        }
    }
}

@Composable
private fun OpcionExpandible(
    seleccionada: Boolean,
    onSelect: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    contenido: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(
                width = if (seleccionada) 2.dp else 1.dp,
                color = if (seleccionada) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitulo, color = FrutAppColors.InkMuted, fontSize = 11.sp)
            }
            RadioCircle(seleccionada = seleccionada)
        }
        if (seleccionada && contenido != null) contenido()
    }
}

@Composable
private fun RadioCircle(seleccionada: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(
                width = 2.dp,
                color = if (seleccionada) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (seleccionada) Box(modifier = Modifier.size(10.dp).background(FrutAppColors.Brand400, CircleShape))
    }
}

@Composable
private fun AlternativaRow(alt: Alternativa, seleccionada: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(
                width = if (seleccionada) 2.dp else 1.dp,
                color = if (seleccionada) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text(alt.emoji, fontSize = 20.sp) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(alt.nombre, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${alt.cantidad}", color = FrutAppColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(alt.stock, color = if (alt.sufStock) FrutAppColors.Brand600 else FrutAppColors.InkMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            RadioCircle(seleccionada = seleccionada)
        }
    }
}
