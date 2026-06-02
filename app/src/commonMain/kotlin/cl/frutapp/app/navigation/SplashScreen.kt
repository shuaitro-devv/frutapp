@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.BiometricAuth
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.navigation.auth.LoginScreen
import cl.frutapp.app.navigation.auth.VerifyCodeScreen
import cl.frutapp.app.navigation.home.HomeScreen
import cl.frutapp.app.ui.components.FrutLoader
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.logo_white
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Splash inmersivo: logo FrutApp blanco sobre fondo verde, con animación de
 * entrada. Tras ~1.8s navega a Login.
 */
class SplashScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scale = remember { Animatable(0.8f) }
        val alpha = remember { Animatable(0f) }
        // Guard: si BiometricAuth invoca onSuccess/onError más de una vez (algunas
        // implementaciones lo hacen) o si el LaunchedEffect se reinicia, navegar dos
        // veces a la misma Screen rompe Voyager con
        // "Key cl.frutapp.app.navigation.auth.LoginScreen:transition was used multiple times".
        // El State sobrevive recomposiciones pero se resetea si la pantalla se recrea
        // por completo (intencional: en ese caso reiniciamos el flujo de splash).
        var navigated by remember { mutableStateOf(false) }
        fun goOnce(action: () -> Unit) {
            if (!navigated) { navigated = true; action() }
        }

        LaunchedEffect(Unit) {
            alpha.animateTo(1f, tween(600))
            scale.animateTo(1f, tween(600))
            delay(1200)
            val pending = TokenStore.pendingEmail
            when {
                // Sin sesión PERO con registro a medias (cuenta creada, codigo no ingresado):
                // ruteamos directo a VerifyCode para no obligar al usuario a empezar de cero.
                // Si quiere salir del limbo, en VerifyCode el boton 'Volver al inicio de sesion'
                // limpia pendingEmail y lo lleva a Login.
                !TokenStore.isLoggedIn && pending != null ->
                    goOnce { navigator.replace(VerifyCodeScreen(email = pending)) }
                // Sin sesión y sin limbo → onboarding (intro), que al terminar lleva a Login.
                !TokenStore.isLoggedIn -> goOnce { navigator.replace(OnboardingScreen()) }
                // Con sesión pero sin huella disponible → Home directo (no bloquear).
                !BiometricAuth.isAvailable() -> goOnce { navigator.replace(HomeScreen()) }
                // Con sesión + huella → pedir huella; al cancelar/fallar, fallback a Login.
                else -> BiometricAuth.authenticate(
                    onSuccess = { goOnce { navigator.replace(HomeScreen()) } },
                    onError = { goOnce { navigator.replace(LoginScreen()) } }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(Res.drawable.logo_white),
                    contentDescription = "FrutApp",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .width(240.dp)
                        .scale(scale.value)
                        .alpha(alpha.value)
                )
                Spacer(Modifier.height(36.dp))
                FrutLoader(
                    modifier = Modifier.alpha(alpha.value),
                    colorA = Color.White,
                    colorB = FrutAppColors.Brand100
                )
            }
        }
    }
}
