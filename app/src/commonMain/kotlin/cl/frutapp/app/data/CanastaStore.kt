package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf

/** Un ítem dentro de una canasta — guarda el producto + cantidad + gramaje (igual que CartItem). */
data class CanastaItem(
    val producto: Producto,
    val cantidad: Int,
    val gramos: Int? = null
) {
    /** Precio total de la línea (calculado igual que CartItem). */
    val precioTotal: Int
        get() = if (gramos != null) (producto.precioClp * gramos / 1000.0).toInt() * cantidad
        else producto.precioClp * cantidad
}

/**
 * Una canasta del usuario o un template FrutApp.
 * - `esTemplate=true` → es una de las 4 sugeridas; no se edita ni se elimina (solo se "copia").
 * - `recordatorioMensual` → toggle "Pídenos avisarte cuando toque pedirla" (mockup, modo A).
 */
data class Canasta(
    val id: Int,
    val nombre: String,
    val emoji: String,
    val items: List<CanastaItem>,
    val esTemplate: Boolean = false,
    val recordatorioMensual: Boolean = false
) {
    val totalEstimado: Int get() = items.sumOf { it.precioTotal }
    val cantidadProductos: Int get() = items.size
}

/**
 * Canastas del usuario (dummy, en memoria por sesión) + 4 templates FrutApp sugeridos.
 * En producción: vendrían del backend cruzando con historial de compras.
 */
object CanastaStore {
    /** Canastas creadas por el usuario (editables). */
    val items = mutableStateListOf<Canasta>()

    private var nextId = 1

    /** 4 templates fijos de FrutApp. Mauricio dijo "golazo" sobre la canasta familiar. */
    val templates: List<Canasta> by lazy { construirTemplates() }

    fun get(id: Int): Canasta? = items.firstOrNull { it.id == id } ?: templates.firstOrNull { it.id == id }

    fun crear(nombre: String, emoji: String, items: List<CanastaItem>): Canasta {
        val nueva = Canasta(id = nextId++, nombre = nombre.ifBlank { "Mi canasta" }, emoji = emoji.ifBlank { "🧺" }, items = items)
        this.items.add(0, nueva)
        return nueva
    }

    fun actualizar(id: Int, nombre: String? = null, emoji: String? = null, items: List<CanastaItem>? = null, recordatorioMensual: Boolean? = null) {
        val i = this.items.indexOfFirst { it.id == id }
        if (i < 0) return
        val cur = this.items[i]
        this.items[i] = cur.copy(
            nombre = nombre ?: cur.nombre,
            emoji = emoji ?: cur.emoji,
            items = items ?: cur.items,
            recordatorioMensual = recordatorioMensual ?: cur.recordatorioMensual
        )
    }

    fun eliminar(id: Int) {
        items.removeAll { it.id == id }
    }

    /** Copiar un template a las canastas del usuario (devuelve el id nuevo). */
    fun copiarTemplate(template: Canasta): Canasta {
        return crear(nombre = template.nombre, emoji = template.emoji, items = template.items)
    }

    /** Agregar un producto a una canasta existente (si ya está, suma cantidad). */
    fun agregarProducto(canastaId: Int, producto: Producto, cantidad: Int = 1, gramos: Int? = null) {
        val i = items.indexOfFirst { it.id == canastaId }
        if (i < 0) return
        val cur = items[i]
        val itemExistente = cur.items.indexOfFirst { it.producto.id == producto.id && it.gramos == gramos }
        val nuevosItems = if (itemExistente >= 0) {
            cur.items.toMutableList().also {
                it[itemExistente] = it[itemExistente].copy(cantidad = it[itemExistente].cantidad + cantidad)
            }
        } else {
            cur.items + CanastaItem(producto, cantidad, gramos)
        }
        items[i] = cur.copy(items = nuevosItems)
    }

    fun reset() {
        items.clear()
        nextId = 1
    }

    private fun construirTemplates(): List<Canasta> {
        val cat = DemoCatalog.productos.associateBy { it.id }
        fun p(id: String) = cat[id] ?: error("Falta producto '$id' en DemoCatalog")

        return listOf(
            Canasta(
                id = -1, esTemplate = true, nombre = "Canasta Asado", emoji = "🔥",
                items = listOf(
                    CanastaItem(p("papa"), 1, 1000),
                    CanastaItem(p("tomate"), 1, 1000),
                    CanastaItem(p("palta-hass"), 1, 500),
                    CanastaItem(p("cebolla"), 1, 500),
                    CanastaItem(p("limon"), 1, 500),
                    CanastaItem(p("ajo"), 1, 250)
                )
            ),
            Canasta(
                id = -2, esTemplate = true, nombre = "Canasta Fitness", emoji = "💪",
                items = listOf(
                    CanastaItem(p("palta-hass"), 1, 500),
                    CanastaItem(p("lechuga"), 2),
                    CanastaItem(p("manzana-roja"), 1, 1000),
                    CanastaItem(p("platano"), 1, 1000),
                    CanastaItem(p("naranja"), 1, 1000)
                )
            ),
            Canasta(
                id = -3, esTemplate = true, nombre = "Canasta Niños", emoji = "👶",
                items = listOf(
                    CanastaItem(p("platano"), 1, 1000),
                    CanastaItem(p("manzana-roja"), 1, 1000),
                    CanastaItem(p("naranja"), 1, 1000),
                    CanastaItem(p("zanahoria"), 1, 500),
                    CanastaItem(p("pepino"), 2)
                )
            ),
            Canasta(
                id = -4, esTemplate = true, nombre = "Canasta Mediterránea", emoji = "🍅",
                items = listOf(
                    CanastaItem(p("tomate"), 1, 1000),
                    CanastaItem(p("cebolla"), 1, 500),
                    CanastaItem(p("ajo"), 1, 250),
                    CanastaItem(p("pimiento-rojo"), 1, 500),
                    CanastaItem(p("pimenton-verde"), 1, 500),
                    CanastaItem(p("cilantro"), 1)
                )
            )
        )
    }
}
