package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Balance de FrutCoins en memoria (cliente). Arranca con un saldo demo y suma lo ganado
 * al pagar un pedido. Cuando exista el backend de recompensas, esto se reemplaza por la API.
 */
object RewardsStore {
    var balance by mutableStateOf(245)
        private set

    fun add(coins: Int) {
        balance += coins
    }

    /** Reinicia al saldo inicial (al cerrar sesión). */
    fun reset() {
        balance = 245
    }
}
