package cl.frutapp.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.navigation.SplashScreen
import cl.frutapp.app.navigation.auth.LoginScreen
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppTheme

@Composable
fun App() {
    FrutAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Voyager Navigator con transición deslizante entre pantallas (sensación app nativa).
            Navigator(SplashScreen()) { navigator ->
                // Detección global de "sesión expirada": cuando el ApiClient detecta un
                // 401 que no puede refrescar (refresh token invalidado server-side), llama
                // a TokenStore.clear() — eso pone accessToken a null reactivamente.
                // Si eso pasa mientras hay pantallas abiertas que no manejan 401 (MisPedidos,
                // Home, FrutCoins, etc.), antes el usuario se quedaba "logueado en pantalla
                // pero sin sesión real" hasta que probaba algo que sí chequeaba (Checkout).
                // Ahora cualquier pantalla queda automaticamente redirigida a Login.
                //
                // Trackeamos si EN ALGUN momento del proceso hubo sesion real. Sin esto,
                // el LaunchedEffect dispara con accessToken=null en el arranque fresco y
                // hace replaceAll(Login) clobereando al Splash antes que pueda enrutar
                // (rompia el caso 'usuario con registro a medias → caer en VerifyCode').
                var hadSession by remember { mutableStateOf(TokenStore.isLoggedIn) }
                LaunchedEffect(TokenStore.accessToken) {
                    if (TokenStore.accessToken != null) {
                        hadSession = true
                    } else if (hadSession) {
                        // accessToken paso de !=null a null durante esta vida del proceso:
                        // sesion realmente expiro. Mostrar toast y volver a Login.
                        showToast("Tu sesión expiró. Vuelve a iniciar sesión.")
                        navigator.replaceAll(LoginScreen())
                        hadSession = false
                    }
                    // accessToken==null en arranque fresco (sin sesion previa en este proceso):
                    // no hacemos nada — Splash decide a donde ir (Onboarding / Login / VerifyCode).
                }
                SlideTransition(navigator)
            }
        }
    }
}
