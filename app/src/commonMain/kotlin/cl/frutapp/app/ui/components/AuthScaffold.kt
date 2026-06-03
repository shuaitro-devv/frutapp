@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cl.frutapp.app.ui.PlatformBackHandler
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.hoja_decorativa
import frutapp.app.generated.resources.limon
import frutapp.app.generated.resources.manzana_roja
import frutapp.app.generated.resources.naranja
import frutapp.app.generated.resources.palta_hass
import frutapp.app.generated.resources.platano
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Plantilla compartida de las pantallas de autenticación (mockups 02-05):
 * fondo blanco, hojas decorativas en las esquinas superiores, composición de
 * frutas asomando por el borde inferior, logo vertical arriba y un slot de
 * contenido scrolleable al centro.
 *
 * La composición de frutas inferior es temporal (frutas sueltas). Si llega un
 * PNG transparente tipo `frutas_inferior` se reemplaza [BottomFruits] por una Image.
 */
@Composable
fun AuthScaffold(
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBack: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    // El back fisico/gesto del sistema dispara el MISMO onBack que la flecha in-screen.
    // Sin esto, Voyager hacia pop() por default y bypaseaba las semanticas custom (ej. en
    // VerifyCodeScreen 'salir del limbo' no se disparaba con el back del sistema, dejando
    // al usuario atrapado: cierra la app, abre, Splash lo vuelve a meter a VerifyCode).
    PlatformBackHandler(enabled = showBackButton, onBack = onBack)
    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        // Hojas decorativas — pegadas a las esquinas superiores y empujadas hacia arriba
        // para no cruzarse con el logo (el logo es transparente y el verde se confundía).
        Image(
            painter = painterResource(Res.drawable.hoja_decorativa),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopEnd).size(132.dp).offset(x = 14.dp, y = (-30).dp),
            contentScale = ContentScale.Fit
        )
        Image(
            painter = painterResource(Res.drawable.hoja_decorativa),
            contentDescription = null,
            modifier = Modifier.align(Alignment.TopStart).size(90.dp).offset(x = (-8).dp, y = (-20).dp).scale(scaleX = -1f, scaleY = 1f),
            contentScale = ContentScale.Fit
        )

        BottomFruits(modifier = Modifier.align(Alignment.BottomCenter))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(112.dp))
            FrutLogo()
            Spacer(Modifier.height(28.dp))
            content()
            Spacer(Modifier.height(130.dp))
        }

        if (showBackButton) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 40.dp)
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = FrutAppColors.Brand800
                )
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun BoxScope.BottomFruits(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = 30.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Image(painterResource(Res.drawable.platano), null, Modifier.size(98.dp), contentScale = ContentScale.Fit)
        Image(painterResource(Res.drawable.manzana_roja), null, Modifier.size(76.dp), contentScale = ContentScale.Fit)
        Image(painterResource(Res.drawable.naranja), null, Modifier.size(68.dp), contentScale = ContentScale.Fit)
        Image(painterResource(Res.drawable.palta_hass), null, Modifier.size(72.dp), contentScale = ContentScale.Fit)
        Image(painterResource(Res.drawable.limon), null, Modifier.size(58.dp), contentScale = ContentScale.Fit)
    }
}
