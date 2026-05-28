package cl.frutapp.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Botones FrutApp según mockups:
 * - Primario: verde sólido, redondeado, texto blanco, ancho completo.
 * - Secundario: outline verde sobre blanco.
 * - Ghost: texto verde sin peso visual.
 */

@Composable
fun FrutButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = cl.frutapp.app.ui.theme.FrutAppShapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = FrutAppColors.Brand400,
            contentColor = androidx.compose.ui.graphics.Color.White,
            disabledContainerColor = FrutAppColors.Brand200
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp).padding(end = 0.dp))
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = if (leadingIcon != null) 8.dp else 0.dp)
            )
        }
    }
}

@Composable
fun FrutButtonOutline(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = cl.frutapp.app.ui.theme.FrutAppShapes.medium,
        border = BorderStroke(1.5.dp, FrutAppColors.Brand400),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = FrutAppColors.Brand800
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = FrutAppColors.Brand600)
            }
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = if (leadingIcon != null) 8.dp else 0.dp)
            )
        }
    }
}

@Composable
fun FrutButtonGhost(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text = text, color = FrutAppColors.Brand600, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
