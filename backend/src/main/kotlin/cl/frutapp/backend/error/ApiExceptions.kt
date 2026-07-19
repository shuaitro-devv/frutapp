package cl.frutapp.backend.error

import io.ktor.http.HttpStatusCode

/**
 * Excepciones de dominio que StatusPages mapea a una respuesta HTTP uniforme.
 * Cada una lleva su status y un código de error estable para el cliente.
 */
sealed class ApiException(
    val statusCode: HttpStatusCode,
    val errorCode: String,
    override val message: String
) : RuntimeException(message)

class ValidationException(message: String) :
    ApiException(HttpStatusCode.UnprocessableEntity, "validation_error", message)

class ConflictException(message: String) :
    ApiException(HttpStatusCode.Conflict, "conflict", message)

class UnauthorizedException(message: String = "Credenciales inválidas") :
    ApiException(HttpStatusCode.Unauthorized, "unauthorized", message)

class NotFoundException(message: String = "Recurso no encontrado") :
    ApiException(HttpStatusCode.NotFound, "not_found", message)

/** 413 cuando el body del request supera el limite global (RequestSizeLimit
 *  plugin). Se tira desde el hook onCall para que StatusPages responda con
 *  el status/code adecuado sin nosotros tocar el pipeline de Ktor (que
 *  requeria un intercept + finish que rompia CallStartTime downstream). */
class PayloadTooLargeException(message: String = "Request body demasiado grande.") :
    ApiException(HttpStatusCode.PayloadTooLarge, "payload_too_large", message)

/** 409 especifico para cuando el snapshot del config del cliente quedo desactualizado
 *  respecto al cache del backend (alguien edito el pricing entre que el cliente abrio
 *  el carrito y aprieta pagar). Lleva los nuevos valores para que la app re-pinte
 *  el total y pida confirmacion sin tener que hacer otro round trip. */
class PricingChangedException(
    val nuevoCostoEnvio: Int,
    val nuevoEnvioGratisDesde: Int,
    message: String = "Los precios cambiaron mientras armabas tu pedido. Revisa el nuevo total antes de confirmar."
) : ApiException(HttpStatusCode.Conflict, "pricing_changed", message)

/** 409 cuando el cliente arma carrito + entre medio el operador marca algun producto
 *  como agotado en el back office. Lleva los nombres para que la app muestre cuales
 *  son y los descarte sin un round trip extra. */
class ProductosAgotadosException(
    val agotados: List<String>,
    message: String = "Algunos productos se agotaron mientras armabas tu pedido."
) : ApiException(HttpStatusCode.Conflict, "products_unavailable", message)
