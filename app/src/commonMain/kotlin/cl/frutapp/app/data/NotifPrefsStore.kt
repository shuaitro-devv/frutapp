package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateOf

/**
 * Preferencias de notificaciones del usuario (toggles). Persistido en [SessionStorage]
 * por dispositivo; mientras no haya endpoint backend, son solo cliente. Defaults: todo
 * en true (mejor onboarding para no perder al usuario al inicio).
 *
 * Las propiedades exponen getter/setter custom para combinar reactividad Compose
 * (`mutableStateOf`) con persistencia automática en cada cambio.
 */
object NotifPrefsStore {
    private const val K_PEDIDOS = "notif_pedidos"
    private const val K_OFERTAS = "notif_ofertas"
    private const val K_FRUTCOINS = "notif_frutcoins"
    private const val K_RECICLAJE = "notif_reciclaje"

    private val _pedidos = mutableStateOf(load(K_PEDIDOS))
    private val _ofertas = mutableStateOf(load(K_OFERTAS))
    private val _frutcoins = mutableStateOf(load(K_FRUTCOINS))
    private val _reciclaje = mutableStateOf(load(K_RECICLAJE))

    var pedidos: Boolean
        get() = _pedidos.value
        set(value) { _pedidos.value = value; save(K_PEDIDOS, value) }

    var ofertas: Boolean
        get() = _ofertas.value
        set(value) { _ofertas.value = value; save(K_OFERTAS, value) }

    var frutcoins: Boolean
        get() = _frutcoins.value
        set(value) { _frutcoins.value = value; save(K_FRUTCOINS, value) }

    var reciclaje: Boolean
        get() = _reciclaje.value
        set(value) { _reciclaje.value = value; save(K_RECICLAJE, value) }

    /** Lectura inicial: si la clave no existe (instalación nueva) → true por default. */
    private fun load(key: String): Boolean = SessionStorage.getString(key) != "0"

    private fun save(key: String, value: Boolean) {
        SessionStorage.putString(key, if (value) "1" else "0")
    }
}
