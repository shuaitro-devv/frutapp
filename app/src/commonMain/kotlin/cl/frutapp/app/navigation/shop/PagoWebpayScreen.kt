package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.PagoApi
import cl.frutapp.app.platform.VistaWebpay
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.components.FrutButtonPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla envoltura del WebView de Webpay.
 *
 *  1. Recibe el (token, urlFormPost) ya generado por POST /v1/pagos/iniciar.
 *  2. Abre [VistaWebpay] que auto-postea el form y maneja el flujo.
 *  3. Cuando el WebView detecta el retorno (URL contiene "/v1/pagos/webpay/retorno"),
 *     consulta GET /v1/pagos/estado/{token} y navega segun el resultado:
 *       - PAGADA: replace -> OrderConfirmedScreen (camino feliz)
 *       - RECHAZADA: muestra mensaje + boton "Volver al carrito"
 *       - ERROR: muestra "estamos verificando, vuelve a tus pedidos"
 *       - INICIADA (aun confirmando): reintenta 3 veces espaciadas 1s antes
 *         de dar por timeout. El backend hace commit en el handler del
 *         retorno, asi que normalmente al primer poll ya esta listo.
 *  4. Si el usuario aprieta back hardware (o el icono back), se cierra el
 *     flujo y vuelve al checkout — la tx queda colgada en INICIADA pero el
 *     guard del backend (hayIniciadaReciente) bloquea otro POST iniciar
 *     hasta que esa tx expire (10 min).
 */
class PagoWebpayScreen(
    private val orderId: String,
    private val token: String,
    private val urlFormPost: String,
    private val returnUrlMarker: String = "/v1/pagos/webpay/retorno",
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val api = remember { PagoApi() }
        var fase by remember { mutableStateOf(FasePago.PAGANDO) }
        var mensajeError by remember { mutableStateOf<String?>(null) }

        // Cuando el WebView detecta el retorno, la pantalla pasa a POLL_ESTADO
        // y arranca el poll.
        LaunchedEffect(fase) {
            if (fase != FasePago.POLL_ESTADO) return@LaunchedEffect
            // 3 intentos espaciados (1s, 1s, 2s = total 4s) por si el commit
            // del backend tarda un toque mas que el HTML de respuesta.
            val intervalos = listOf(0L, 1000L, 1000L, 2000L)
            var ultimoEstado: String? = null
            for (espera in intervalos) {
                if (espera > 0) delay(espera)
                val estado = runCatching { api.estado(token).estado }
                    .onFailure { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        ErrorReporter.report(screen = "PagoWebpay", action = "estado", error = e)
                    }
                    .getOrNull()
                ultimoEstado = estado
                if (estado != null && estado != "INICIADA") break
            }
            when (ultimoEstado) {
                "PAGADA" -> {
                    // Reemplazamos por OrderTrackingScreen que se hidrata sola
                    // con un GET y muestra estado actual + timeline + items.
                    // OrderConfirmedScreen pide muchos campos que aca no tenemos
                    // a mano (numero, total, etc) — el tracking es mejor camino.
                    navigator.replace(OrderTrackingScreen(orderId))
                }
                "RECHAZADA" -> {
                    mensajeError = "Tu tarjeta no autorizó el cobro. Puedes reintentar desde el carrito."
                    fase = FasePago.FALLIDO
                }
                "ERROR" -> {
                    mensajeError = "Estamos verificando el resultado con el banco. Revisa tus pedidos en unos minutos."
                    fase = FasePago.FALLIDO
                }
                else -> {
                    // INICIADA después de varios reintentos: tratar como
                    // "verificando" — el usuario revisa luego.
                    mensajeError = "Estamos confirmando tu pago. Revisa tus pedidos en unos segundos."
                    fase = FasePago.FALLIDO
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
            // Top bar minimal — solo back cuando NO estamos en POLL (no querés
            // que el usuario salga a medio commit).
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navigator.pop() },
                    enabled = fase != FasePago.POLL_ESTADO
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Text("Pago con Webpay", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            when (fase) {
                FasePago.PAGANDO -> VistaWebpay(
                    token = token,
                    urlFormPost = urlFormPost,
                    returnUrlMarker = returnUrlMarker,
                    onRetornoListo = { fase = FasePago.POLL_ESTADO },
                    onError = { msg ->
                        mensajeError = "No pudimos abrir Webpay ($msg). Intenta de nuevo desde el carrito."
                        fase = FasePago.FALLIDO
                    },
                    modifier = Modifier.fillMaxSize().weight(1f)
                )

                FasePago.POLL_ESTADO -> Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FrutAppColors.Brand600, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "Confirmando tu pago…",
                            color = FrutAppColors.Brand800,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "No cierres esta pantalla.",
                            color = FrutAppColors.InkSoft,
                            fontSize = 12.sp
                        )
                    }
                }

                FasePago.FALLIDO -> Column(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            mensajeError ?: "No se pudo completar el pago.",
                            color = FrutAppColors.Brand800,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    FrutButtonPrimary(
                        text = "Volver",
                        onClick = { navigator.pop() }
                    )
                    Spacer(Modifier.height(12.dp).navigationBarsPadding())
                }
            }
        }
    }
}

private enum class FasePago { PAGANDO, POLL_ESTADO, FALLIDO }
