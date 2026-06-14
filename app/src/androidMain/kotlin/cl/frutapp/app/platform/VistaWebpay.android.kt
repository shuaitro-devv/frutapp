package cl.frutapp.app.platform

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * WebView Android para Webpay. Implementacion del expect en commonMain.
 *
 * Detalles del flujo (tomado del playbook polizapp, probado end-to-end sandbox):
 *
 *  1. Auto-form-POST: Webpay exige POST con `token_ws=<token>` hacia la
 *     `urlFormPost`. Cargamos un HTML minimo con `<form>` + `onload` para
 *     submitearlo automaticamente sin que el usuario vea el paso intermedio.
 *
 *  2. Detectar el retorno: en `onPageFinished`, cuando la URL contiene
 *     `returnUrlMarker` el backend YA hizo el commit (el GET/POST de retorno
 *     dispara `confirmarRetorno` antes de responder el HTML). Disparamos
 *     `onRetornoListo` para que la pantalla padre consulte `/pagos/estado`.
 *     NO interceptar la URL (NO usar `shouldOverrideUrlLoading` para
 *     bloquearla) — si la cancelas antes de que el server responda, el
 *     backend NUNCA hace commit y queda colgada.
 *
 *  3. JavaScript: habilitado (el form auto-submit lo necesita; Webpay tambien
 *     ejecuta JS en su pagina de tarjeta).
 *
 *  4. mixedContent: Webpay redirige a HTTPS pero la return_url en dev puede
 *     ser HTTP (10.0.2.2:8080 en emulador). Permitimos contenido mixto
 *     compatible con MODE_COMPATIBILITY para que el redirect no falle.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun VistaWebpay(
    token: String,
    urlFormPost: String,
    returnUrlMarker: String,
    onRetornoListo: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                webViewClient = object : WebViewClient() {
                    private var retornoDisparado = false

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null && url.contains(returnUrlMarker) && !retornoDisparado) {
                            retornoDisparado = true
                            onRetornoListo()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?,
                    ) {
                        // Ignorar errores de subrecursos (anuncios, etc) — solo
                        // reportar si fallo la URL principal del WebView.
                        if (failingUrl != null && failingUrl == view?.url) {
                            onError(description ?: "Error cargando Webpay")
                        }
                    }
                }
                // HTML con form auto-submit para POSTear token_ws al url
                // que Webpay nos dio. Sin esto la WebView no tiene como hacer
                // POST (no hay nav.formData en Android WebView publica).
                val html = """
                    <html>
                      <body onload="document.forms[0].submit()">
                        <form method="post" action="$urlFormPost">
                          <input type="hidden" name="token_ws" value="$token" />
                        </form>
                      </body>
                    </html>
                """.trimIndent()
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    )
}
