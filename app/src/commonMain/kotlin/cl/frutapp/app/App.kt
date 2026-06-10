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
                // Deteccion global de "sesion expirada en runtime": cuando ApiClient
                // ve un 401 protegido + refresh tambien devuelve 401 (token invalidado
                // server-side), llama TokenStore.markSessionExpired(). Observamos ese
                // flag aca y pateamos a Login con toast.
                //
                // Por que un flag explicito y no observar accessToken=null:
                // - Logout voluntario desde ProfileScreen ya hace TokenStore.clear() +
                //   navigator.replaceAll(LoginScreen()) por su cuenta. NO queremos
                //   mostrar "tu sesion expiro" (mentira) ni hacer doble replaceAll.
                // - El arranque fresco ve accessToken=null sin que sea expiracion.
                // El flag distingue determinante: solo se setea cuando es expiracion real.
                LaunchedEffect(TokenStore.sessionExpired) {
                    if (!TokenStore.sessionExpired) return@LaunchedEffect
                    // Si el usuario ya esta en una pantalla de auth (logout cruzado o
                    // splash redirigiendo), no dupliques. Vacia el flag y sal.
                    val lastItem = navigator.lastItem
                    val yaEstaEnAuth = lastItem is LoginScreen ||
                        lastItem::class.qualifiedName?.contains("OnboardingScreen") == true ||
                        lastItem::class.qualifiedName?.contains("VerifyCodeScreen") == true ||
                        lastItem::class.qualifiedName?.contains("SplashScreen") == true
                    TokenStore.consumeSessionExpired()
                    if (!yaEstaEnAuth) {
                        showToast("Tu sesión expiró. Vuelve a iniciar sesión.")
                        navigator.replaceAll(LoginScreen())
                    }
                }
                SlideTransition(navigator)
            }
        }
    }
}
