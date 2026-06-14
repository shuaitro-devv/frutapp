package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Buffer entre Android (que recibe el tap del push) y Compose/Voyager (que navega).
 *
 * Flujo:
 *  1. El usuario toca el push en la barra de notificaciones (o icono custom de
 *     la app si Android estaba force-stopped).
 *  2. Android arranca/trae a primer plano MainActivity con extras (`orderId`,
 *     `type`, `status`).
 *  3. MainActivity los lee en onCreate (cold start) o onNewIntent (warm)
 *     y los publica acá via [set].
 *  4. App.kt tiene un LaunchedEffect observando [orderId]: cuando cambia y hay
 *     sesion activa, [consume]-lo y navega segun [type]/[status].
 *
 * El Compose state delegation hace que el LaunchedEffect se dispare incluso si
 * el usuario tocaba el push mientras la app ya estaba abierta (warm tap).
 */
object PendingNotification {
    var orderId: String? by mutableStateOf(null)
        private set
    var type: String? by mutableStateOf(null)
        private set
    var status: String? by mutableStateOf(null)
        private set

    fun set(orderId: String?, type: String?, status: String?) {
        // Solo seteamos si vino algun orderId — sin eso no hay nada para navegar.
        if (orderId.isNullOrBlank()) return
        this.orderId = orderId
        this.type = type
        this.status = status
    }

    /** Devuelve los datos pendientes y los limpia. Llamarlo una vez, desde el
     *  LaunchedEffect de App.kt, cuando se confirme que se puede navegar (hay
     *  sesion activa y el navigator no esta en una pantalla de auth). */
    fun consume(): Triple<String, String?, String?>? {
        val oid = orderId ?: return null
        val t = type
        val s = status
        orderId = null
        type = null
        status = null
        return Triple(oid, t, s)
    }

    /** Limpia el buffer sin navegar. Llamar en LOGOUT: si el usuario tocaba
     *  un push justo antes de cerrar sesion, esa noti era para ese usuario;
     *  no queremos disparar la navegacion para el SIGUIENTE usuario que se
     *  loguee (potencialmente otra persona en el mismo celu compartido). */
    fun clear() {
        orderId = null
        type = null
        status = null
    }
}
