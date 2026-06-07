package cl.frutapp.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** CompositionLocal con el Brand activo. Util cuando un Composable necesita el
 *  copy/coinsName/displayName, o para previews que quieran forzar otro brand. */
val LocalBrand = staticCompositionLocalOf<Brand> { FrutAppBrand }

/**
 * Tema raiz de la app. Lee el Brand activo desde [ActiveBrand] (que es un
 * mutableStateOf) y construye el `lightColorScheme` desde su paleta. Cuando el
 * usuario cambia el Brand desde el toggle de Perfil, esta lectura se invalida
 * y toda la app se recompone con la paleta nueva.
 *
 * Para overrides puntuales (preview/tests) usar
 * `CompositionLocalProvider(LocalBrand provides X) { FrutAppTheme(brand = X) { ... } }`.
 */
@Composable
fun FrutAppTheme(
    brand: Brand = ActiveBrand.current,
    content: @Composable () -> Unit
) {
    val scheme = remember(brand.id) {
        val p = brand.palette
        lightColorScheme(
            primary = p.brand400,
            onPrimary = Color.White,
            primaryContainer = p.brand50,
            onPrimaryContainer = p.brand800,

            secondary = p.brand600,
            onSecondary = Color.White,
            secondaryContainer = p.brand100,
            onSecondaryContainer = p.brand800,

            tertiary = p.amberCoin,
            onTertiary = Color.White,
            tertiaryContainer = p.amberSoft,
            onTertiaryContainer = p.amberCoin,

            background = Color.White,
            onBackground = p.ink,

            surface = Color.White,
            onSurface = p.ink,
            surfaceVariant = p.cream,
            onSurfaceVariant = p.inkMuted,

            error = p.error,
            onError = Color.White
        )
    }
    CompositionLocalProvider(LocalBrand provides brand) {
        MaterialTheme(
            colorScheme = scheme,
            typography = FrutAppTypography,
            shapes = FrutAppShapes,
            content = content
        )
    }
}
