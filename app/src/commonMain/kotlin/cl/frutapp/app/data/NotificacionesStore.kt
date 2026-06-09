package cl.frutapp.app.data

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
 * Notificaciones del usuario (dummy en memoria). En producción vendrían por push (FCM)
 * + endpoint de inbox. Para el demo arrancan con 3 sin leer para que el badge muestre algo.
 */
object NotificacionesStore {
    val items = mutableStateListOf<Notificacion>(
        Notificacion(1, TipoNotificacion.PEDIDO, "Tu pedido está en camino", "Sale el repartidor desde Lo Valledor. Entrega entre 10:00-12:00.", "hace 5 min", leida = false),
        Notificacion(2, TipoNotificacion.COINS, "¡Ganaste 50 FrutCoins!", "Por tu compra del jueves. Saldo total: 147 coins.", "hace 2 h", leida = false),
        Notificacion(3, TipoNotificacion.RACHA, "🔥 12 días en racha verde", "Estás a 19 días del próximo nivel: Árbol.", "hace 1 día", leida = false),
        Notificacion(4, TipoNotificacion.RECICLA, "Tu reciclaje fue validado", "390g de cartón al ciclo · +30 coins acreditados.", "hace 2 días", leida = true),
        Notificacion(5, TipoNotificacion.PROMO, "🎁 -40% en frutas seleccionadas", "Solo hasta el domingo. Aprovecha la promo de fin de semana.", "hace 3 días", leida = true)
    )

    val noLeidas: Int get() = items.count { !it.leida }

    /**
     * Inserta una notificacion nueva al inicio de la lista (mas reciente arriba).
     * Llamada desde [FrutAppMessagingService.onMessageReceived] para que los push
     * que llegan por FCM tambien queden visibles en la pantalla de Notificaciones,
     * no solo en la barra de Android. El id se autoasigna sumando 1 al max actual.
     */
    fun add(titulo: String, detalle: String, tipo: TipoNotificacion = TipoNotificacion.PEDIDO) {
        val nuevoId = (items.maxOfOrNull { it.id } ?: 0) + 1
        items.add(0, Notificacion(nuevoId, tipo, titulo, detalle, "ahora", leida = false))
    }

    fun marcarTodasLeidas() {
        val nuevos = items.map { it.copy(leida = true) }
        items.clear()
        items.addAll(nuevos)
    }

    fun reset() {
        items.clear()
        items.addAll(
            listOf(
                Notificacion(1, TipoNotificacion.PEDIDO, "Tu pedido está en camino", "Sale el repartidor desde Lo Valledor. Entrega entre 10:00-12:00.", "hace 5 min", leida = false),
                Notificacion(2, TipoNotificacion.COINS, "¡Ganaste 50 FrutCoins!", "Por tu compra del jueves. Saldo total: 147 coins.", "hace 2 h", leida = false),
                Notificacion(3, TipoNotificacion.RACHA, "🔥 12 días en racha verde", "Estás a 19 días del próximo nivel: Árbol.", "hace 1 día", leida = false)
            )
        )
    }
}
