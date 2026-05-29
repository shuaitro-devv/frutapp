package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class Resena(
    val id: Int,
    val nombre: String,
    val estrellas: Int,
    val fecha: String,
    val texto: String,
    /** True si la escribió el usuario actual (se puede editar). */
    val propia: Boolean = false
)

/**
 * Reseñas por producto (dummy, en memoria). Cada producto arranca con las reseñas semilla
 * y el usuario puede agregar/editar la suya (queda arriba). En producción esto vendría del backend.
 */
object ResenasStore {
    private fun semilla() = listOf(
        Resena(1, "Camila R.", 5, "hace 2 días", "Llegó fresquísimo y muy rápido. Calidad de feria sin moverme de la casa."),
        Resena(2, "Felipe M.", 5, "hace 1 semana", "Excelente selección, todo en su punto. Ya es mi compra fija de la semana."),
        Resena(3, "Daniela P.", 4, "hace 2 semanas", "Muy buena calidad y buen precio. Repito sin dudarlo.")
    )

    private const val SEMILLA_COUNT = 3
    private var nextId = 100
    private val porProducto = mutableStateMapOf<String, SnapshotStateList<Resena>>()

    fun resenas(productId: String): SnapshotStateList<Resena> =
        porProducto.getOrPut(productId) { mutableStateListOf<Resena>().apply { addAll(semilla()) } }

    /** Reseñas que el usuario agregó a este producto (sobre las semilla). */
    fun extras(productId: String): Int = (resenas(productId).size - SEMILLA_COUNT).coerceAtLeast(0)

    fun agregar(productId: String, nombre: String, estrellas: Int, texto: String) {
        resenas(productId).add(0, Resena(nextId++, nombre, estrellas, "recién", texto.trim(), propia = true))
    }

    fun editar(productId: String, id: Int, estrellas: Int, texto: String) {
        val lista = resenas(productId)
        val i = lista.indexOfFirst { it.id == id }
        if (i >= 0) lista[i] = lista[i].copy(estrellas = estrellas, texto = texto.trim(), fecha = "editada")
    }
}
