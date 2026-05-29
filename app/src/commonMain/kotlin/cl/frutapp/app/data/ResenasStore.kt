package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class Resena(val nombre: String, val estrellas: Int, val fecha: String, val texto: String)

/**
 * Reseñas por producto (dummy, en memoria). Cada producto arranca con las reseñas semilla
 * y el usuario puede agregar la suya (queda arriba). En producción esto vendría del backend.
 */
object ResenasStore {
    private val SEMILLA = listOf(
        Resena("Camila R.", 5, "hace 2 días", "Llegó fresquísimo y muy rápido. Calidad de feria sin moverme de la casa."),
        Resena("Felipe M.", 5, "hace 1 semana", "Excelente selección, todo en su punto. Ya es mi compra fija de la semana."),
        Resena("Daniela P.", 4, "hace 2 semanas", "Muy buena calidad y buen precio. Repito sin dudarlo.")
    )

    private val porProducto = mutableStateMapOf<String, SnapshotStateList<Resena>>()

    fun resenas(productId: String): SnapshotStateList<Resena> =
        porProducto.getOrPut(productId) { mutableStateListOf<Resena>().apply { addAll(SEMILLA) } }

    /** Reseñas que el usuario agregó a este producto (sobre las semilla). */
    fun extras(productId: String): Int = (resenas(productId).size - SEMILLA.size).coerceAtLeast(0)

    fun agregar(productId: String, nombre: String, estrellas: Int, texto: String) {
        resenas(productId).add(0, Resena(nombre, estrellas, "recién", texto.trim()))
    }
}
