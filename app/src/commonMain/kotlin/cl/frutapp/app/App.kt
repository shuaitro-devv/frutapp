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
import cl.frutapp.app.navigation.picker.PickerHomeScreen
import cl.frutapp.app.navigation.repartidor.RepartidorHomeScreen
import cl.frutapp.app.navigation.picker.PickerPicklistScreen
import cl.frutapp.app.navigation.repartidor.RepartidorDetalleScreen
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
                // CRITICAL: navigator.lastItem es la 3ra key. Sin esto, en cold start
                // el effect se ejecuta una vez en SplashScreen (sale por enAuth=true),
                // y cuando Splash navega a Home las keys orderId/accessToken NO cambian
                // → el effect NUNCA se vuelve a triggear y el deep link se pierde.
                // Con lastItem como key, se re-ejecuta en cada cambio de pantalla y
                // hace el push apenas el usuario sale del auth.
                LaunchedEffect(PendingNotification.orderId, TokenStore.accessToken, navigator.lastItem) {
                    val pendingOrderId = PendingNotification.orderId ?: return@LaunchedEffect
                    if (TokenStore.accessToken == null) return@LaunchedEffect
                    val lastItem = navigator.lastItem
                    val enAuth = lastItem is LoginScreen ||
                        lastItem::class.qualifiedName?.contains("OnboardingScreen") == true ||
                        lastItem::class.qualifiedName?.contains("VerifyCodeScreen") == true ||
                        lastItem::class.qualifiedName?.contains("SplashScreen") == true
                    if (enAuth) return@LaunchedEffect
                    val data = PendingNotification.consume() ?: return@LaunchedEffect
                    val (orderId, type, status) = data
                    val roles = TokenStore.user?.roles ?: emptyList()
                    val esCliente = "cliente" in roles
                    val esPicker = "picker" in roles
                    val esRepartidor = "repartidor" in roles
                    // Fallback: si el push apunta a un rol que el usuario NO tiene
                    // (ej. cliente recibe picker_new_order por error de targeting),
                    // mandar a la home apropiada en vez de OrderTrackingScreen que
                    // es pantalla de cliente (un picker/repartidor sin rol cliente
                    // recibe 403/404 ahi). Asi al menos el usuario aterriza en una
                    // pantalla operativa para SU rol.
                    val fallback: Screen = when {
                        esCliente -> OrderTrackingScreen(orderId)
                        esPicker -> PickerHomeScreen()
                        esRepartidor -> RepartidorHomeScreen()
                        else -> OrderTrackingScreen(orderId)  // sin roles utiles
                    }
                    // Routing por tipo de push + rol del usuario logueado.
                    // - Cliente (order_status): si hay ajuste pendiente va DIRECTO a
                    //   AjusteAprobacionScreen; cualquier otro estado (EN_PICKING,
                    //   STOCK_CONFIRMADO, EN_DESPACHO, ENTREGADO, CANCELADO) va a
                    //   OrderTrackingScreen que ya pinta timeline, banner de ajuste,
                    //   productos del pedido y CTAs (calificar, volver a pedir).
                    // - Picker (picker_new_order): va al picklist del pedido para que
                    //   pueda tomarlo y arrancar. picker_ajuste_resuelto va al home
                    //   del picker (su trabajo en ese pedido ya termino, solo recibe
                    //   el aviso del destino).
                    // - Repartidor (repartidor_new_dispatch): va al detalle del
                    //   despacho para poder tomarlo.
                    val destino: Screen = when (type) {
                        "picker_new_order" -> if (esPicker) PickerPicklistScreen(orderId) else fallback
                        "picker_ajuste_resuelto" -> if (esPicker) PickerHomeScreen() else fallback
                        "repartidor_new_dispatch" -> if (esRepartidor) RepartidorDetalleScreen(orderId) else fallback
                        "order_status" -> when {
                            !esCliente -> fallback
                            status == "ESPERANDO_AJUSTE_CLIENTE" -> AjusteAprobacionScreen(orderId)
                            else -> OrderTrackingScreen(orderId)
                        }
                        "chat_mensaje" -> when {
                            // Staff abre ChatScreen directo al cliente. Para el
                            // cliente vamos al tracking (ahi elige picker o
                            // repartidor segun corresponda al estado del
                            // pedido — no resolvemos el destinatario aca
                            // porque no sabemos el estado actual).
                            esPicker -> cl.frutapp.app.navigation.shop.ChatScreen(
                                orderId = orderId,
                                destinatarioRol = "cliente",
                                tituloContraparte = "Cliente del pedido",
                            )
                            esRepartidor -> cl.frutapp.app.navigation.shop.ChatScreen(
                                orderId = orderId,
                                destinatarioRol = "cliente",
                                tituloContraparte = "Cliente del pedido",
                            )
                            esCliente -> OrderTrackingScreen(orderId)
                            else -> fallback
                        }
                        else -> fallback
                    }
                    navigator.push(destino)
                }
                SlideTransition(navigator)
            }
        }
    }
}
