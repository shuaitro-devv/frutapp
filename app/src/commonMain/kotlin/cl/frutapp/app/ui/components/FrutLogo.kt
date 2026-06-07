@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.frutapp.app.ui.theme.LocalBrand
import cl.frutapp.app.ui.theme.brandLogoMain
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Logo principal del brand activo (FrutApp por default, Sofruco si el flavor o el
 * toggle de Modo de tienda lo cambiaron). Se usa en pantallas de auth y donde se
 * requiera la marca completa.
 */
@Composable
fun FrutLogo(
    modifier: Modifier = Modifier,
    width: Dp = 210.dp
) {
    Image(
        painter = painterResource(brandLogoMain()),
        contentDescription = LocalBrand.current.displayName,
        modifier = modifier.width(width),
        contentScale = ContentScale.FillWidth
    )
}
