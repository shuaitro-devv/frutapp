package cl.frutapp.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de colores de la app. Delega en [ActiveBrand] para soportar white-label:
 * el mismo codigo de pantalla pinta verde FrutApp por default, naranjo-verde Sofruco
 * cuando el flavor sofruco arranca, etc.
 *
 * Esto preserva todos los call sites existentes (`FrutAppColors.Brand400`) sin tocar
 * nada en las 30+ pantallas. Cuando cambia [ActiveBrand.current] antes de setContent,
 * la siguiente lectura de cada token devuelve el color del brand activo.
 *
 * Para tematizar dentro de un Composable, preferir [LocalBrand].current.palette
 * (se mantiene reactivo a CompositionLocalProvider para previews/tests).
 */
object FrutAppColors {
    val Brand50: Color get() = ActiveBrand.current.palette.brand50
    val Brand100: Color get() = ActiveBrand.current.palette.brand100
    val Brand200: Color get() = ActiveBrand.current.palette.brand200
    val Brand400: Color get() = ActiveBrand.current.palette.brand400
    val Brand600: Color get() = ActiveBrand.current.palette.brand600
    val Brand800: Color get() = ActiveBrand.current.palette.brand800

    val AmberCoin: Color get() = ActiveBrand.current.palette.amberCoin
    val AmberSoft: Color get() = ActiveBrand.current.palette.amberSoft

    val Cream: Color get() = ActiveBrand.current.palette.cream
    val Background: Color get() = ActiveBrand.current.palette.background
    val Ink: Color get() = ActiveBrand.current.palette.ink
    val InkMuted: Color get() = ActiveBrand.current.palette.inkMuted
    val InkSoft: Color get() = ActiveBrand.current.palette.inkSoft

    val Error: Color get() = ActiveBrand.current.palette.error
    val Warning: Color get() = ActiveBrand.current.palette.warning
}
