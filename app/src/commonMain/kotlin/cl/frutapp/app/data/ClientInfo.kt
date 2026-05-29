package cl.frutapp.app.data

/**
 * Contexto del cliente (canal/dispositivo) que se adjunta al crear un pedido.
 * Lo provee la plataforma; se usa como metadata operacional/analítica en el backend.
 */
expect object ClientInfo {
    val channel: String       // APP_ANDROID, WEB, ...
    val appVersion: String
    val deviceModel: String
    val osVersion: String
    val locale: String
}
