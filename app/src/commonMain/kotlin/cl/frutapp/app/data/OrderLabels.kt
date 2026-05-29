package cl.frutapp.app.data

/** Etiqueta legible del medio de pago (los códigos vienen del backend). */
fun paymentMethodLabel(code: String): String = when (code) {
    "TARJETA" -> "Tarjeta de crédito/débito"
    "DEBITO" -> "Tarjeta de débito"
    "WEBPAY" -> "Webpay"
    "MERCADO_PAGO" -> "Mercado Pago"
    "EFECTIVO" -> "Efectivo"
    "FRUTCOINS" -> "FrutCoins"
    "TRANSFERENCIA" -> "Transferencia"
    else -> code
}

/** Etiqueta legible de la modalidad de entrega. */
fun fulfillmentLabel(type: String): String = when (type) {
    "DELIVERY" -> "Despacho a domicilio"
    "RETIRO" -> "Retiro en sucursal"
    else -> type
}
