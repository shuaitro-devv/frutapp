package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cl.frutapp.app.data.remote.RewardApi
import cl.frutapp.shared.dto.CuponDto

/**
 * Caché en memoria del saldo de FrutCoins + cupones. La FUENTE DE VERDAD es el
 * backend (ledger via GET /v1/frutcoins, cupones via /v1/frutcoins/cupones).
 *
 * El canje real va por [canjearRemoto] → POST /v1/frutcoins/redeem. Idempotente
 * con [idempotencyKey] (UUID v4 generado por la pantalla al abrir el dialogo).
 * Si responde OK, descontamos del balance local Y agregamos el cupon al cache;
 * si falla, no tocamos nada.
 */
object RewardsStore {
    var balance by mutableStateOf(0)
        private set

    /** Cupones del usuario en memoria. Estado Compose para que las pantallas
     *  observen sin polling. */
    val cupones = mutableStateListOf<CuponDto>()

    private val api = RewardApi()

    fun set(value: Int) {
        balance = value
    }

    /** Canje real contra el backend. Retorna el cupon o null si fallo (la UI
     *  decide si mostrar error). Idempotencia: si el POST se reintenta con
     *  la misma [idempotencyKey], el backend devuelve el cupon ya creado y
     *  acá no duplicamos en el cache. */
    suspend fun canjearRemoto(
        monto: Int,
        recompensa: String,
        idempotencyKey: String,
    ): CuponDto? {
        val cupon = runCatching { api.canjear(monto, recompensa, idempotencyKey) }.getOrNull()
            ?: return null
        // Update local: ajustamos balance optimisticamente con el monto canjeado.
        // El proximo refresh de /v1/frutcoins reconciliara con el ledger real.
        balance = (balance - cupon.monto).coerceAtLeast(0)
        if (cupones.none { it.id == cupon.id }) {
            cupones.add(0, cupon)
        }
        return cupon
    }

    /** Carga la lista de cupones del usuario desde el backend. */
    suspend fun cargarCupones() {
        val lista = runCatching { api.listarCupones() }.getOrNull() ?: return
        cupones.clear()
        cupones.addAll(lista)
    }

    /** Al cerrar sesión. */
    fun reset() {
        balance = 0
        cupones.clear()
    }
}
