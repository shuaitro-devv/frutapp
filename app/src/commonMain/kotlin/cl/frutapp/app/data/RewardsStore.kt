package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Caché en memoria del saldo de FrutCoins. La FUENTE DE VERDAD es el backend (ledger,
 * GET /v1/frutcoins); las pantallas lo cargan y actualizan acá para mostrarlo.
 */
object RewardsStore {
    var balance by mutableStateOf(0)
        private set

    fun set(value: Int) {
        balance = value
    }

    /**
     * Resta `amount` del balance (canje de recompensa). Cliente-only por ahora — cuando
     * el backend tenga POST /v1/frutcoins/redeem, llamamos primero y solo si responde
     * 200 actualizamos acá.
     */
    fun spend(amount: Int) {
        balance = (balance - amount).coerceAtLeast(0)
    }

    /** Al cerrar sesión. */
    fun reset() {
        balance = 0
    }
}
