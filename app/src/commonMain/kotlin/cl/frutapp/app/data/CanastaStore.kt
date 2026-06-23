package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf
import cl.frutapp.app.data.remote.BasketApi
import cl.frutapp.shared.dto.ActualizarCanastaRequest
import cl.frutapp.shared.dto.CanastaDto
import cl.frutapp.shared.dto.CrearCanastaRequest
import cl.frutapp.shared.dto.NuevoCanastaItem

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
 * - `esTemplate=true` → es una de las 4 sugeridas; no se edita ni elimina.
 * - `recordatorioMensual` → toggle "Pídenos avisarte cuando toque pedirla".
 * - `id` es String: UUID del backend cuando es del usuario, "tpl-<slug>" cuando
 *   es template local.
 */
data class Canasta(
    val id: String,
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
 * Canastas del usuario, cableadas al backend (V35). Templates locales se
 * componen on-demand desde [DemoCatalog]. La fuente de verdad para canastas
 * del usuario es `/v1/baskets`; el cache local refleja lo del backend para
 * que las pantallas Compose lo lean sincronicamente.
 */
object CanastaStore {
    /** Canastas del usuario, hidratadas desde el backend. Las pantallas leen
     *  directamente — al cambiar el state, recomponen automaticamente. */
    val items = mutableStateListOf<Canasta>()

    private val api = BasketApi()

    /** Catalogo (para mapear product.id ↔ slug en los items que vienen del
     *  backend). Lo setea la app temprano (CatalogStore) y se reusa. */
    var catalogoResolver: ((String) -> Producto?)? = null

    val templates: List<Canasta> by lazy { construirTemplates() }

    fun get(id: String): Canasta? =
        items.firstOrNull { it.id == id } ?: templates.firstOrNull { it.id == id }

    /** Hidrata desde el backend. Idempotente: pisa lo que haya en cache. */
    suspend fun cargar() {
        val resp = runCatching { api.listar() }.getOrNull() ?: return
        val mapeadas = resp.map { it.toUi() }
        items.clear()
        items.addAll(mapeadas)
    }

    /** Crea una canasta en backend; al volver agrega al cache local arriba. */
    suspend fun crear(nombre: String, emoji: String, items: List<CanastaItem>): Canasta? {
        val req = CrearCanastaRequest(
            nombre = nombre.ifBlank { "Mi canasta" },
            emoji = emoji.ifBlank { "🧺" },
            recordatorioMensual = false,
            items = items.mapNotNull { it.toRequestItem() },
        )
        val dto = runCatching { api.crear(req) }.getOrNull() ?: return null
        val ui = dto.toUi()
        this.items.add(0, ui)
        return ui
    }

    /** PUT del header / items. Cualquier null se ignora (no se manda al backend). */
    suspend fun actualizar(
        id: String,
        nombre: String? = null,
        emoji: String? = null,
        items: List<CanastaItem>? = null,
        recordatorioMensual: Boolean? = null,
    ): Canasta? {
        val req = ActualizarCanastaRequest(
            nombre = nombre,
            emoji = emoji,
            recordatorioMensual = recordatorioMensual,
            items = items?.mapNotNull { it.toRequestItem() },
        )
        val dto = runCatching { api.actualizar(id, req) }.getOrNull() ?: return null
        val ui = dto.toUi()
        val idx = this.items.indexOfFirst { it.id == id }
        if (idx >= 0) this.items[idx] = ui else this.items.add(0, ui)
        return ui
    }

    suspend fun eliminar(id: String): Boolean {
        val ok = runCatching { api.eliminar(id) }.isSuccess
        if (ok) items.removeAll { it.id == id }
        return ok
    }

    /** Copia un template a las canastas del usuario (POST al backend). */
    suspend fun copiarTemplate(template: Canasta): Canasta? =
        crear(template.nombre, template.emoji, template.items)

    /** Agrega un producto a una canasta del usuario (PUT con la lista nueva). */
    suspend fun agregarProducto(canastaId: String, producto: Producto, cantidad: Int = 1, gramos: Int? = null): Boolean {
        val canasta = items.firstOrNull { it.id == canastaId } ?: return false
        val idx = canasta.items.indexOfFirst { it.producto.id == producto.id && it.gramos == gramos }
        val nuevos = if (idx >= 0) {
            canasta.items.toMutableList().also {
                it[idx] = it[idx].copy(cantidad = it[idx].cantidad + cantidad)
            }
        } else {
            canasta.items + CanastaItem(producto, cantidad, gramos)
        }
        return actualizar(canastaId, items = nuevos) != null
    }

    fun reset() {
        items.clear()
    }

    private fun CanastaItem.toRequestItem(): NuevoCanastaItem? {
        // El backend espera UUID; si el producto no tiene backendId (DemoCatalog),
        // lo skipeamos silenciosamente (no se puede persistir).
        val pid = producto.backendId ?: return null
        return NuevoCanastaItem(productoId = pid, cantidad = cantidad, gramos = gramos)
    }

    private fun CanastaDto.toUi(): Canasta {
        val resolver = catalogoResolver
        val itemsUi = items.mapNotNull { dto ->
            val prod = resolver?.invoke(dto.productoId)
            if (prod == null) null
            else CanastaItem(producto = prod, cantidad = dto.cantidad, gramos = dto.gramos)
        }
        return Canasta(
            id = id,
            nombre = nombre,
            emoji = emoji,
            items = itemsUi,
            esTemplate = false,
            recordatorioMensual = recordatorioMensual,
        )
    }

    private fun construirTemplates(): List<Canasta> {
        val cat = DemoCatalog.productos.associateBy { it.id }
        fun p(id: String) = cat[id] ?: error("Falta producto '$id' en DemoCatalog")

        return listOf(
            Canasta(
                id = "tpl-asado", esTemplate = true, nombre = "Canasta Asado", emoji = "🔥",
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
                id = "tpl-fitness", esTemplate = true, nombre = "Canasta Fitness", emoji = "💪",
                items = listOf(
                    CanastaItem(p("palta-hass"), 1, 500),
                    CanastaItem(p("lechuga"), 2),
                    CanastaItem(p("manzana-roja"), 1, 1000),
                    CanastaItem(p("platano"), 1, 1000),
                    CanastaItem(p("naranja"), 1, 1000)
                )
            ),
            Canasta(
                id = "tpl-ninos", esTemplate = true, nombre = "Canasta Niños", emoji = "👶",
                items = listOf(
                    CanastaItem(p("platano"), 1, 1000),
                    CanastaItem(p("manzana-roja"), 1, 1000),
                    CanastaItem(p("naranja"), 1, 1000),
                    CanastaItem(p("zanahoria"), 1, 500),
                    CanastaItem(p("pepino"), 2)
                )
            ),
            Canasta(
                id = "tpl-mediterranea", esTemplate = true, nombre = "Canasta Mediterránea", emoji = "🍅",
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
