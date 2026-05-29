package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf

/**
 * Favoritos del cliente (en memoria de la sesión). Observable por Compose: el corazón
 * del detalle refleja el estado y se mantiene al navegar. No se persiste entre reinicios
 * (suficiente para el demo; sin pantalla de favoritos aún).
 */
object FavoritesStore {
    private val ids = mutableStateListOf<String>()

    fun isFavorite(productId: String): Boolean = ids.contains(productId)

    fun toggle(productId: String) {
        if (!ids.remove(productId)) ids.add(productId)
    }
}
