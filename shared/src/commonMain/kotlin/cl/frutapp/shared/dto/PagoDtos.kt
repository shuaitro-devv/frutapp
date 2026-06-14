package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Request del cliente para iniciar el flujo de pago Webpay. */
@Serializable
data class IniciarPagoRequest(val orderId: String)

/** Respuesta del backend con lo que la app necesita para abrir la WebView
 *  con form-POST hacia Webpay (`token_ws = token`, action = url). */
@Serializable
data class IniciarPagoResponse(val token: String, val urlFormPost: String)

/** Estado de la transaccion consultado por la app al cerrar la WebView.
 *  Valores de [estado]: INICIADA / PAGADA / RECHAZADA / ERROR. La app
 *  distingue INICIADA ("aun confirmando") de RECHAZADA. */
@Serializable
data class EstadoPagoResponse(
    val token: String,
    val estado: String,
    val orderId: String,
)
