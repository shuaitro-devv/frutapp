package cl.frutapp.app.navigation.picker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Paleta y visual unificada de [EstadoItem]. Antes los colores `Color(0xFF3B82F6)` (azul
 * sustituido), `Color(0xFFD97706)` (ambar reducido) y `Color(0xFFB91C1C)` (rojo faltante)
 * aparecian hardcodeados como literales hex en 5+ archivos (bordeColorPorEstado,
 * EstadoBoxGrande, ChipResolucion, ResumenChip, ResolucionBox, ItemResumenRow). Cada
 * vez que se ajustara la paleta habia que tocar 5 lugares y rezar.
 *
 * Ahora todo cuelga de [EstadoItem.visual]: un solo `when` que devuelve color principal,
 * icono y label. Cualquier composable que pinte segun estado consume este helper.
 */
data class EstadoVisual(
    val color: Color,
    val icon: ImageVector?,
    val label: String
) {
    val bg: Color get() = color.copy(alpha = 0.12f)
    val borderTint: Color get() = color.copy(alpha = 0.4f)
}

/** Colores de los 3 tipos de resolucion no-completos. Tampoco viven en FrutAppColors
 *  por ahora porque son especificos del flujo de picking — si se generalizan a otros
 *  perfiles (repartidor: entregado/parcial/rechazado) migran al theme. */
object EstadoPaleta {
    val sustituido = Color(0xFF3B82F6) // azul
    val reducido = Color(0xFFD97706)   // ambar
    val faltante = Color(0xFFB91C1C)   // rojo
}

fun EstadoItem.visual(): EstadoVisual = when (this) {
    EstadoItem.COMPLETADO -> EstadoVisual(FrutAppColors.Brand400, Icons.Filled.Check, "Completado")
    EstadoItem.SUSTITUIDO -> EstadoVisual(EstadoPaleta.sustituido, Icons.Filled.SwapHoriz, "Sustituido")
    EstadoItem.REDUCIDO -> EstadoVisual(EstadoPaleta.reducido, Icons.Filled.Remove, "Cantidad reducida")
    EstadoItem.FALTANTE -> EstadoVisual(EstadoPaleta.faltante, Icons.Filled.Close, "Faltante reportado")
    EstadoItem.PENDIENTE -> EstadoVisual(FrutAppColors.InkSoft, null, "Pendiente")
}
