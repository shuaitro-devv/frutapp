package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Huella verde acumulada del usuario (dummy, en memoria). En producción vendría del
 * backend (sumando reciclajes + compras). Los valores semilla replican lo de la web.
 */
object HuellaVerdeStore {
    var reciclajes by mutableStateOf(13)
        private set
    var gramosAlCiclo by mutableStateOf(390)
        private set
    var coinsGanados by mutableStateOf(450)
        private set
    var ahorradoClp by mutableStateOf(8500)
        private set

    /** Suma una compra al impacto (típica al confirmar pedido). */
    fun sumarCompra(coins: Int, ahorroClp: Int = 0) {
        coinsGanados += coins
        ahorradoClp += ahorroClp
    }

    /** Suma un reciclaje confirmado. */
    fun sumarReciclaje(gramos: Int, coins: Int) {
        reciclajes += 1
        gramosAlCiclo += gramos
        coinsGanados += coins
    }

    fun reset() {
        reciclajes = 13
        gramosAlCiclo = 390
        coinsGanados = 450
        ahorradoClp = 8500
    }
}
