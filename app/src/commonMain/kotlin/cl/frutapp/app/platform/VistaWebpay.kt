package cl.frutapp.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * WebView que abre el formulario de Webpay con form-POST de `token_ws` a la
 * `urlFormPost` que devolvio el backend.
 *
 * El flujo en Android:
 *  1. La WebView carga un HTML local que auto-submitea un form POST con
 *     `token_ws = token` hacia [urlFormPost].
 *  2. Webpay muestra su pagina de pago (datos de tarjeta + redirect del
 *     banco simulado / real). El usuario completa.
 *  3. Webpay redirige el browser al `return_url` configurado en el backend
 *     (`{returnUrlBase}/v1/pagos/webpay/retorno?token_ws=...`).
 *  4. Cuando la WebView termina de cargar esa URL del retorno, sabemos que
 *     el commit del backend ya corrio. Llama a [onRetornoListo] para que la
 *     pantalla padre consulte `/v1/pagos/estado/{token}` y decida que mostrar.
 *
 * iOS: actual pendiente (cuando agreguemos iosMain).
 *
 * @param token token Webpay devuelto por `/v1/pagos/iniciar`.
 * @param urlFormPost URL al que la WebView debe POSTear el form.
 * @param returnUrlMarker fragmento de la URL del retorno que delata el final
 *  del flujo (ej. "/v1/pagos/webpay/retorno"). Cuando la WebView lo detecta,
 *  dispara [onRetornoListo].
 */
@Composable
expect fun VistaWebpay(
    token: String,
    urlFormPost: String,
    returnUrlMarker: String,
    onRetornoListo: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
)
