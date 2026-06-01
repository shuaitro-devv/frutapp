package cl.frutapp.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                LaunchedEffect(TokenStore.accessToken) {
                    // accessToken == null en arranque fresco → SplashScreen ya enruta a Onboarding/Login.
                    // El caso que cubrimos acá: estaba !=null (sesion activa) y pasó a null sin
                    // navegacion explicita. Detectamos eso pidiendo que el navigator NO este
                    // ya en LoginScreen — si lo está, fue logout voluntario y no hace falta hacer nada.
                    if (TokenStore.accessToken == null) {
                        val ultimaPantalla = navigator.lastItem
                        val yaEstaEnAuth = ultimaPantalla is LoginScreen ||
                            ultimaPantalla::class.qualifiedName?.contains("OnboardingScreen") == true
                        if (!yaEstaEnAuth) {
                            showToast("Tu sesión expiró. Vuelve a iniciar sesión.")
                            navigator.replaceAll(LoginScreen())
                        }
                    }
                }
                SlideTransition(navigator)
            }
        }
    }
}
