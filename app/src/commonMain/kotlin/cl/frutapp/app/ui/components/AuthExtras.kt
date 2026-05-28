package cl.frutapp.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.FrutAppShapes

/** Título + subtítulo centrados de las pantallas auth. */
@Composable
fun AuthHeaderText(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            color = FrutAppColors.Brand800,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            color = FrutAppColors.InkMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, start = 8.dp, end = 8.dp)
        )
    }
}

/** Divisor "o continúa con" con líneas a los lados. */
@Composable
fun OrDivider(text: String = "o continúa con", modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = FrutAppColors.Brand100)
        Text(
            text = text,
            color = FrutAppColors.InkMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = FrutAppColors.Brand100)
    }
}

/**
 * Botones de proveedores sociales (Google / Apple). Por ahora solo texto; cuando
 * existan los PNG oficiales `logo_google` / `logo_apple` se agregan como leadingIcon.
 */
@Composable
fun SocialButtons(onGoogle: () -> Unit, onApple: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SocialButton(text = "Google", onClick = onGoogle, modifier = Modifier.weight(1f))
        SocialButton(text = "Apple", onClick = onApple, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SocialButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = FrutAppShapes.medium,
        border = BorderStroke(1.dp, FrutAppColors.Brand100)
    ) {
        Text(text = text, color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
