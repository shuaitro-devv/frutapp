package cl.frutapp.backend.config

import io.ktor.server.config.ApplicationConfig

/**
 * Config de Webpay Plus (Transbank). Single-tenant: una cuenta de comercio
 * cubre toda la operacion de FrutApp. Si en el futuro hay co-marca con
 * Sofruco / otros operadores con cuenta Transbank propia, hay que mover
 * estas creds a "por tenant" (mismo patron que polizapp prevee en su
 * playbook §7).
 *
 * Defaults (`application.yaml`): credenciales PUBLICAS de sandbox que
 * publica Transbank — no son secret y funcionan para desarrollo local.
 * En prod (Contabo) sobreescribir por env (WEBPAY_*).
 *
 * Importante: no usamos el SDK de Transbank (peso, dependencies). Son 2
 * llamadas REST simples — mismo patron que para FCM.
 *
 *  - [apiBaseUrl]: integracion = `https://webpay3gint.transbank.cl`;
 *    produccion = `https://webpay3g.transbank.cl`.
 *  - [commerceCode] / [apiKey]: identificacion del comercio ante Transbank.
 *    En sandbox son publicos (no son secret).
 *  - [returnUrlBase]: host PUBLICO al que Webpay redirige el browser al
 *    terminar. Tiene que ser alcanzable desde el WebView del celu. En
 *    emulador es `10.0.2.2`; en prod, `https://frutapp-api.grandline.cl`.
 *    Es bug tipico al pasar de emu a celu.
 */
data class WebpayConfig(
    val apiBaseUrl: String,
    val commerceCode: String,
    val apiKey: String,
    val returnUrlBase: String,
) {
    /** True si estamos apuntando al ambiente de integracion (sandbox). */
    val esSandbox: Boolean get() = apiBaseUrl.contains("webpay3gint")

    companion object {
        fun from(config: ApplicationConfig) = WebpayConfig(
            apiBaseUrl = config.property("webpay.apiBaseUrl").getString(),
            commerceCode = config.property("webpay.commerceCode").getString(),
            apiKey = config.property("webpay.apiKey").getString(),
            returnUrlBase = config.property("webpay.returnUrlBase").getString(),
        )
    }
}
