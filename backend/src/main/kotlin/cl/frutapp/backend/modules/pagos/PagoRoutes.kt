package cl.frutapp.backend.modules.pagos

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.eventContext
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.EstadoPagoResponse
import cl.frutapp.shared.dto.IniciarPagoRequest
import cl.frutapp.shared.dto.IniciarPagoResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

/**
 * Rutas de pago Webpay Plus.
 *
 * IMPORTANTE: el endpoint /v1/pagos/webpay/retorno es PUBLICO (sin JWT).
 * Webpay redirige el browser del usuario aca al terminar; ese browser NO
 * tiene Bearer. El lookup es por token (PRIMARY KEY de webpay_transaccion).
 * Por eso el token DEBE ser tratado como pseudo-secret: solo lo conoce
 * Transbank, el usuario que pago y este backend. No leak en logs publicos.
 */
fun Route.pagoRoutes(service: WebpayPagoService) {
    authenticate(JWT_AUTH) {
        // POST /v1/pagos/iniciar { orderId }
        post("/v1/pagos/iniciar") {
            val uid = call.userId()
            val body = call.receive<IniciarPagoRequest>()
            val orderId = runCatching { UUID.fromString(body.orderId) }.getOrNull()
                ?: throw ValidationException("orderId invalido.")
            val r = service.iniciar(uid, orderId, call.eventContext())
            call.respond(HttpStatusCode.OK, IniciarPagoResponse(token = r.token, urlFormPost = r.urlFormPost))
        }

        // GET /v1/pagos/estado/{token}
        get("/v1/pagos/estado/{token}") {
            val uid = call.userId()
            val token = call.parameters["token"]?.takeIf { it.isNotBlank() }
                ?: throw ValidationException("token requerido.")
            val estado = service.estado(token, uid)
                ?: throw NotFoundException("Transaccion no encontrada.")
            call.respond(EstadoPagoResponse(token = estado.token, estado = estado.estado, orderId = estado.orderId))
        }
    }

    // PUBLICO (sin JWT). Webpay redirige aca con token_ws. Puede llegar como
    // POST form (mas comun) o GET con query — soportar ambos por compat.
    post("/v1/pagos/webpay/retorno") {
        val params = call.receiveParameters()
        val token = params["token_ws"] ?: params["TBK_TOKEN"]
        responderRetorno(call, service, token)
    }
    get("/v1/pagos/webpay/retorno") {
        val token = call.parameters["token_ws"] ?: call.parameters["TBK_TOKEN"]
        responderRetorno(call, service, token)
    }
}

private suspend fun responderRetorno(
    call: io.ktor.server.application.ApplicationCall,
    service: WebpayPagoService,
    token: String?,
) {
    // HTML simple para el browser del usuario. El WebView de la app detecta
    // la URL "/pagos/webpay/retorno" en onPageFinished y cierra sola; el
    // contenido es de respaldo (si el usuario abrio Webpay desde Chrome
    // externo en lugar de la app, ve este mensaje y vuelve manualmente).
    if (token.isNullOrBlank()) {
        // Webpay puede redirigir SIN token_ws cuando el usuario cancela o
        // hay timeout (manda TBK_TOKEN que ya capturamos arriba, o nada).
        call.respondText(htmlMensaje("Pago cancelado", "Volviste sin completar el pago."),
            contentType = ContentType.Text.Html, status = HttpStatusCode.OK)
        return
    }
    val resultado = service.confirmarRetorno(token)
    val (titulo, mensaje) = when (resultado) {
        is RetornoResult.Aprobada -> "Pago aprobado" to "Listo, ya cobramos tu pedido. Volvé a la app de FrutApp."
        is RetornoResult.Rechazada -> "Pago rechazado" to "Tu tarjeta no autorizó el cobro. Volvé a la app para reintentar."
        is RetornoResult.Error -> "Estamos confirmando" to "Estamos verificando tu pago con el banco. Volvé a la app, ahí ves el resultado."
        is RetornoResult.Desconocida -> "Transacción desconocida" to "No encontramos esta transacción. Volvé a la app y revisa tu pedido."
    }
    call.respondText(htmlMensaje(titulo, mensaje), contentType = ContentType.Text.Html, status = HttpStatusCode.OK)
}

private fun htmlMensaje(titulo: String, mensaje: String): String = """
    <!doctype html>
    <html lang="es">
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1" />
      <title>$titulo · FrutApp</title>
      <style>
        body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
               background: #EAF3DE; color: #27500A;
               display: flex; align-items: center; justify-content: center;
               min-height: 100vh; margin: 0; padding: 20px; }
        .card { background: #FFFFFF; padding: 30px 24px; border-radius: 16px;
                max-width: 360px; box-shadow: 0 4px 16px rgba(39,80,10,0.1); text-align: center; }
        h1 { font-size: 20px; margin: 0 0 12px; color: #27500A; }
        p { font-size: 14px; color: #4A6E2A; margin: 0; line-height: 1.4; }
      </style>
    </head>
    <body>
      <div class="card">
        <h1>$titulo</h1>
        <p>$mensaje</p>
      </div>
    </body>
    </html>
""".trimIndent()
