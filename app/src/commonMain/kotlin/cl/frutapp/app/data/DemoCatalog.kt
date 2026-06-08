@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.data

import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.ajo
import frutapp.app.generated.resources.cebolla
import frutapp.app.generated.resources.cilantro
import frutapp.app.generated.resources.lechuga
import frutapp.app.generated.resources.limon
import frutapp.app.generated.resources.manzana_roja
import frutapp.app.generated.resources.naranja
import frutapp.app.generated.resources.palta_hass
import frutapp.app.generated.resources.papa
import frutapp.app.generated.resources.pepino
import frutapp.app.generated.resources.pimenton_verde
import frutapp.app.generated.resources.pimiento_rojo
import frutapp.app.generated.resources.platano
import frutapp.app.generated.resources.tomate
import frutapp.app.generated.resources.zanahoria
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Catálogo de demo (mock) para construir y validar las pantallas visuales antes de
 * conectar el backend real. Usa las fotos de producto disponibles en composeResources.
 * Cuando el backend de catálogo esté listo, esta fuente se reemplaza por la API.
 */

enum class Categoria(val label: String) {
    FRUTAS("Frutas"),
    VERDURAS("Verduras"),
    HIERBAS("Hierbas"),
    DESPENSA("Despensa"),
    // ORGANICOS no es una categoría de DB — es un filtro transversal que aplica el flag
    // producto.organico. Lo dejamos en el enum por compatibilidad con el chip del Home.
    ORGANICOS("Orgánicos")
}

data class Producto(
    val id: String,
    val nombre: String,
    val precioClp: Int,
    val unidad: String,
    val categoria: Categoria,
    val imagen: DrawableResource,
    val organico: Boolean = false,
    /** Solo aplica al white-label Sofruco. Cuando el catalogo viene de un brand
     *  alternativo, [categoria] queda forzada a FRUTAS por compatibilidad con el
     *  enum, y la categoria real (jugos/aguas/fruta/cajas/secos/vinos) vive aca
     *  para que los filtros de catalogo brand puedan match. */
    val brandCategoryId: String? = null
)

object DemoCatalog {
    val productos: List<Producto> = listOf(
        Producto("palta-hass", "Palta Hass", 1890, "kg", Categoria.FRUTAS, Res.drawable.palta_hass, organico = true),
        Producto("platano", "Plátano", 1190, "kg", Categoria.FRUTAS, Res.drawable.platano),
        Producto("manzana-roja", "Manzana Roja", 1690, "kg", Categoria.FRUTAS, Res.drawable.manzana_roja),
        Producto("naranja", "Naranja", 990, "kg", Categoria.FRUTAS, Res.drawable.naranja),
        Producto("limon", "Limón", 1490, "kg", Categoria.FRUTAS, Res.drawable.limon),
        Producto("tomate", "Tomate", 1290, "kg", Categoria.VERDURAS, Res.drawable.tomate),
        Producto("lechuga", "Lechuga", 800, "unidad", Categoria.VERDURAS, Res.drawable.lechuga, organico = true),
        Producto("zanahoria", "Zanahoria", 890, "kg", Categoria.VERDURAS, Res.drawable.zanahoria),
        Producto("cebolla", "Cebolla", 990, "kg", Categoria.VERDURAS, Res.drawable.cebolla),
        Producto("papa", "Papa", 1290, "kg", Categoria.VERDURAS, Res.drawable.papa),
        Producto("pepino", "Pepino", 690, "unidad", Categoria.VERDURAS, Res.drawable.pepino),
        Producto("pimiento-rojo", "Pimiento Rojo", 2490, "kg", Categoria.VERDURAS, Res.drawable.pimiento_rojo),
        Producto("pimenton-verde", "Pimentón Verde", 1990, "kg", Categoria.VERDURAS, Res.drawable.pimenton_verde),
        Producto("cilantro", "Cilantro", 500, "atado", Categoria.HIERBAS, Res.drawable.cilantro, organico = true),
        Producto("ajo", "Ajo", 3990, "kg", Categoria.VERDURAS, Res.drawable.ajo)
    )

    val destacados: List<Producto> = productos.take(6)

    fun porCategoria(categoria: Categoria): List<Producto> =
        productos.filter { it.categoria == categoria }
}

/** Formatea un monto CLP entero con separador de miles chileno: 1890 → "$1.890". */
fun formatClp(value: Int): String {
    val digits = value.toString()
    val sb = StringBuilder()
    var count = 0
    for (i in digits.lastIndex downTo 0) {
        sb.append(digits[i])
        count++
        if (count % 3 == 0 && i != 0) sb.append('.')
    }
    return "$" + sb.reverse().toString()
}
