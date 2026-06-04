package cl.frutapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Burbuja con icono centrado. Reemplaza ~70 occurrences del patron
 * `Box(size(N.dp).background(Brand50, CircleShape), Center) { Icon(...) }` desperdigado
 * por todo el navigation tree (avatars de cliente/picker, iconos de stats, iconos de
 * accion en sheets, badges, etc.).
 */
@Composable
fun IconBubble(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    bg: Color = FrutAppColors.Brand50,
    fg: Color = FrutAppColors.Brand600,
    iconSize: Dp = 20.dp,
    shape: Shape = CircleShape,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier.size(size).background(bg, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = fg, modifier = Modifier.size(iconSize))
    }
}

/** Overload para los avatares de una letra ('R' del cliente, 'F' del logo voucher, etc.). */
@Composable
fun IconBubble(
    initial: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    bg: Color = FrutAppColors.Brand50,
    fg: Color = FrutAppColors.Brand600,
    shape: Shape = CircleShape,
    textSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    Box(
        modifier = modifier.size(size).background(bg, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(initial.take(1).uppercase(), color = fg, fontSize = textSize, fontWeight = FontWeight.Bold)
    }
}
