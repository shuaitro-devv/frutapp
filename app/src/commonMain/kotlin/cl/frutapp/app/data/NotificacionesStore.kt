package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf

enum class TipoNotificacion(val emoji: String) {
    PEDIDO("📦"),
    COINS("🪙"),
    RECICLA("♻️"),
    RACHA("🔥"),
    PROMO("🎁");
}

data class Notificacion(
    val id: Int,
    val tipo: TipoNotificacion,
    val titulo: String,
    val detalle: String,
    val cuando: String,
    val leida: Boolean = false
)

/**
 * Cache LIVE de notificaciones en memoria — fuente de verdad es el backend
 * (`GET /v1/notifications`). Sirve solo para que el BADGE de la campanita
 * se actualice instantáneo cuando llega un push por FCM, sin esperar al
 * próximo refresh contra el server.
 *
 * Dos contadores:
 *  - [backendUnread]: lo que devolvió el último GET (refresh manual).
 *  - delta in-memory: las notifs nuevas que llegaron via FCM después del refresh.
 *
 * [noLeidas] suma ambos para que el badge sea correcto sin importar el orden.
 * Al entrar a [NotificacionesScreen] todo se marca leído server-side y se
 * llama [resetAll] para vaciar el cache local.
 */
object NotificacionesStore {
    /** Notifs agregadas EN VIVO via FCM (no las del backend). Sirven solo para
     *  el badge — la pantalla siempre consume el GET. */
    val items = mutableStateListOf<Notificacion>()

    /** Snapshot del unreadCount que devolvió el ultimo `GET /v1/notifications`. */
    var backendUnread by mutableStateOf(0)
        private set

    /** Badge = lo que dijo el backend + lo que llegó en runtime después. */
    val noLeidas: Int get() = backendUnread + items.count { !it.leida }

    /** Sumá una notif que llegó por FCM. Llamado desde FrutAppMessagingService. */
    fun add(titulo: String, detalle: String, tipo: TipoNotificacion = TipoNotificacion.PEDIDO) {
        val nuevoId = (items.maxOfOrNull { it.id } ?: 0) + 1
        items.add(0, Notificacion(nuevoId, tipo, titulo, detalle, "ahora", leida = false))
    }

    /** Refresca el contador del badge desde el backend. Llamado al login y
     *  cuando se vuelve a la home (sin entrar al inbox). Nombre con prefijo
     *  `update` para no chocar con el setter implicito del `var` Compose. */
    fun updateBackendUnread(count: Int) {
        backendUnread = count
    }

    /** Limpia el cache local (las que llegaron por FCM ya no cuentan porque
     *  el inbox se acaba de abrir). Llamado por NotificacionesScreen tras
     *  recibir items del GET y disparar markAllRead. */
    fun resetAll() {
        items.clear()
        backendUnread = 0
    }

    fun marcarTodasLeidas() {
        val nuevos = items.map { it.copy(leida = true) }
        items.clear()
        items.addAll(nuevos)
        backendUnread = 0
    }

    fun reset() {
        items.clear()
        backendUnread = 0
    }
}
