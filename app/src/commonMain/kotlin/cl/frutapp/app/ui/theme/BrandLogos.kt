@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.ui.theme

import androidx.compose.runtime.Composable
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.logo_main
import frutapp.app.generated.resources.logo_main_sofruco
import frutapp.app.generated.resources.logo_white
import frutapp.app.generated.resources.logo_white_sofruco
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Selector de logos por brand. Vive aca (no en [Brand]) porque las referencias
 * `Res.drawable.X` son generadas por compose-mp y no se pueden capturar como campos
 * de un object en el momento que se define el Brand (orden de inicializacion).
 *
 * Los call sites de `painterResource(Res.drawable.logo_main)` se migran a
 * `painterResource(brandLogoMain())` para que el toggle de Modo de tienda repinte
 * el logo junto con la paleta.
 */
@Composable
fun brandLogoMain(): DrawableResource {
    val brand = LocalBrand.current
    return when (brand.id) {
        SofrucoBrand.id -> Res.drawable.logo_main_sofruco
        else -> Res.drawable.logo_main
    }
}

@Composable
fun brandLogoWhite(): DrawableResource {
    val brand = LocalBrand.current
    return when (brand.id) {
        SofrucoBrand.id -> Res.drawable.logo_white_sofruco
        else -> Res.drawable.logo_white
    }
}
