package cl.frutapp.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import cl.frutapp.app.data.PendingNotification
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.navigation.SplashScreen
import cl.frutapp.app.navigation.auth.LoginScreen
import cl.frutapp.app.navigation.shop.AjusteAprobacionScreen
import cl.frutapp.app.navigation.shop.OrderTrackingScreen
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
                // Deep link del push: cuando MainActivity recibe un Intent con
                // orderId (tap del push en la barra, frio o caliente), lo deja
                // en PendingNotification. Observamos el flag y navegamos a la
                // pantalla correcta SI el usuario tiene sesion activa y NO esta
                // en una pantalla de auth (en ese caso queda en cola hasta que
                // termine el login). Sin esto, el push abria la app en home y el
                // usuario tenia que buscar manualmente el pedido.
                LaunchedEffect(PendingNotification.orderId, TokenStore.accessToken) {
                    val pendingOrderId = PendingNotification.orderId ?: return@LaunchedEffect
                    if (TokenStore.accessToken == null) return@LaunchedEffect
                    val lastItem = navigator.lastItem
                    val enAuth = lastItem is LoginScreen ||
                        lastItem::class.qualifiedName?.contains("OnboardingScreen") == true ||
                        lastItem::class.qualifiedName?.contains("VerifyCodeScreen") == true ||
                        lastItem::class.qualifiedName?.contains("SplashScreen") == true
                    if (enAuth) return@LaunchedEffect
                    val data = PendingNotification.consume() ?: return@LaunchedEffect
                    val (orderId, _, status) = data
                    // Routing minimo por status: si el cliente tiene un ajuste de peso
                    // pendiente, va DIRECTO al diálogo de aprobacion (1 tap menos).
                    // Para cualquier otro estado, vamos al tracking que ya sabe pintar
                    // los banners y timelines correctos. Para pushes de staff
                    // (picker_new_order / repartidor_new_dispatch) el routing actual
                    // tambien lleva al tracking — esa cuenta es del cliente, y los
                    // staff abren la app y van a su home; no aplica.
                    val destino: Screen = if (status == "ESPERANDO_AJUSTE_CLIENTE") {
                        AjusteAprobacionScreen(orderId)
                    } else {
                        OrderTrackingScreen(orderId)
                    }
                    navigator.push(destino)
                }
                SlideTransition(navigator)
            }
        }
    }
}
