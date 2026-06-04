package cl.frutapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Chip compacto con icono opcional + label + color de tinte personalizable. Reemplaza ~10
 * implementaciones casi-identicas dispersas (ChipPrioridad, ChipAntiguedad, ChipResolucion,
 * ChipEntrega, ChipEstado, etc.) que diferian solo en color y label.
 *
 * Distinto de [FrutChip] (el FrutChip del cliente es un filter-chip seleccionable 38dp
 * con padding 16x9; este StatusChip es el formato 'pildora informativa' chiquito ~22dp
 * con padding 8x4 que usan las cards de la app).
 *
 * Para el caso comun 'fondo color.copy(alpha=0.12f) + texto color', basta pasar `color`
 * y dejar `bg`/`fg` en null. Para overrides (background custom), pasar bg/fg explicitos.
 */
@Composable
fun StatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    bg: Color? = null,
    fg: Color? = null,
    iconSize: Dp = 11.dp,
    fontSize: TextUnit = 11.sp,
    shape: Shape = RoundedCornerShape(8.dp),
    padH: Dp = 8.dp,
    padV: Dp = 4.dp,
    leadingDot: Boolean = false
) {
    val bgEfectivo = bg ?: color.copy(alpha = 0.12f)
    val fgEfectivo = fg ?: color
    Row(
        modifier = modifier.background(bgEfectivo, shape).padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingDot) {
            Box(modifier = Modifier.size(iconSize - 4.dp).background(fgEfectivo, CircleShape))
            Spacer(Modifier.width(6.dp))
        } else if (icon != null) {
            Icon(icon, null, tint = fgEfectivo, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(4.dp))
        }
        Text(label, color = fgEfectivo, fontSize = fontSize, fontWeight = FontWeight.SemiBold)
    }
}
