package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Stores en memoria (dummy) para los mini-flujos del perfil: direcciones y métodos de
 * pago. Persisten mientras la app esté abierta y se limpian al cerrar sesión, igual que
 * el carrito. En producción esto vendría del backend.
 */

data class Direccion(val id: Int, val etiqueta: String, val calle: String, val comuna: String)

object DireccionesStore {
    val items = mutableStateListOf(
        Direccion(1, "Casa", "Av. Las Palmeras 1234, depto 52", "Ñuñoa"),
        Direccion(2, "Trabajo", "Av. Providencia 2010, of. 803", "Providencia")
    )
    var predeterminadaId by mutableStateOf(1)
        private set
    private var nextId = 3

    fun setPredeterminada(id: Int) { predeterminadaId = id }

    fun add(etiqueta: String, calle: String, comuna: String) {
        val nueva = Direccion(nextId++, etiqueta.ifBlank { "Dirección" }, calle, comuna)
        items.add(nueva)
        if (items.size == 1) predeterminadaId = nueva.id
    }

    fun remove(id: Int) {
        items.removeAll { it.id == id }
        if (predeterminadaId == id) predeterminadaId = items.firstOrNull()?.id ?: 0
    }

    fun reset() {
        items.clear()
        items.addAll(
            listOf(
                Direccion(1, "Casa", "Av. Las Palmeras 1234, depto 52", "Ñuñoa"),
                Direccion(2, "Trabajo", "Av. Providencia 2010, of. 803", "Providencia")
            )
        )
        predeterminadaId = 1
        nextId = 3
    }
}

data class MetodoPago(val id: Int, val tipo: String, val ultimos4: String, val vence: String)

object MetodosPagoStore {
    val items = mutableStateListOf(
        MetodoPago(1, "Visa", "4821", "08/27"),
        MetodoPago(2, "Mastercard", "1290", "11/26")
    )
    var predeterminadoId by mutableStateOf(1)
        private set
    private var nextId = 3

    fun setPredeterminado(id: Int) { predeterminadoId = id }

    fun add(tipo: String, ultimos4: String, vence: String) {
        val nuevo = MetodoPago(nextId++, tipo, ultimos4, vence)
        items.add(nuevo)
        if (items.size == 1) predeterminadoId = nuevo.id
    }

    fun remove(id: Int) {
        items.removeAll { it.id == id }
        if (predeterminadoId == id) predeterminadoId = items.firstOrNull()?.id ?: 0
    }

    fun reset() {
        items.clear()
        items.addAll(
            listOf(
                MetodoPago(1, "Visa", "4821", "08/27"),
                MetodoPago(2, "Mastercard", "1290", "11/26")
            )
        )
        predeterminadoId = 1
        nextId = 3
    }
}
