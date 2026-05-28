@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.logo_main
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Logo oficial FrutApp (logo-main.png): isotipo + wordmark + slogan.
 * Se usa en las pantallas de auth y donde se requiera la marca completa.
 */
@Composable
fun FrutLogo(
    modifier: Modifier = Modifier,
    width: Dp = 210.dp
) {
    Image(
        painter = painterResource(Res.drawable.logo_main),
        contentDescription = "FrutApp",
        modifier = modifier.width(width),
        contentScale = ContentScale.FillWidth
    )
}
