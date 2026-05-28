package cl.frutapp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val FrutAppLightColors = lightColorScheme(
    primary = FrutAppColors.Brand400,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = FrutAppColors.Brand50,
    onPrimaryContainer = FrutAppColors.Brand800,

    secondary = FrutAppColors.Brand600,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = FrutAppColors.Brand100,
    onSecondaryContainer = FrutAppColors.Brand800,

    tertiary = FrutAppColors.AmberCoin,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = FrutAppColors.AmberSoft,
    onTertiaryContainer = FrutAppColors.AmberCoin,

    background = androidx.compose.ui.graphics.Color.White,
    onBackground = FrutAppColors.Ink,

    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = FrutAppColors.Ink,
    surfaceVariant = FrutAppColors.Cream,
    onSurfaceVariant = FrutAppColors.InkMuted,

    error = FrutAppColors.Error,
    onError = androidx.compose.ui.graphics.Color.White
)

@Composable
fun FrutAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FrutAppLightColors,
        typography = FrutAppTypography,
        shapes = FrutAppShapes,
        content = content
    )
}
