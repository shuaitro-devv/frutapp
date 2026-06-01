@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.data

import cl.frutapp.shared.dto.ProductDto
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.ajo
import frutapp.app.generated.resources.canasta_frutas
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
 * Mapea el `imageKey` (string del backend) al drawable empaquetado en la app.
 * Al sumar productos nuevos hay que agregar la foto a composeResources y un caso acá.
 */
fun drawableForImageKey(key: String): DrawableResource = when (key) {
    "ajo" -> Res.drawable.ajo
    "cebolla" -> Res.drawable.cebolla
    "cilantro" -> Res.drawable.cilantro
    "lechuga" -> Res.drawable.lechuga
    "limon" -> Res.drawable.limon
    "manzana_roja" -> Res.drawable.manzana_roja
    "naranja" -> Res.drawable.naranja
    "palta_hass" -> Res.drawable.palta_hass
    "papa" -> Res.drawable.papa
    "pepino" -> Res.drawable.pepino
    "pimenton_verde" -> Res.drawable.pimenton_verde
    "pimiento_rojo" -> Res.drawable.pimiento_rojo
    "platano" -> Res.drawable.platano
    "tomate" -> Res.drawable.tomate
    "zanahoria" -> Res.drawable.zanahoria
    else -> Res.drawable.canasta_frutas
}

// IDs de categoría del seed (V1__init.sql). Si cambian, ajustar acá.
private const val CAT_FRUTAS = "11111111-1111-1111-1111-111111111111"
private const val CAT_VERDURAS = "22222222-2222-2222-2222-222222222222"

private fun categoriaFor(categoryId: String): Categoria = when (categoryId) {
    CAT_FRUTAS -> Categoria.FRUTAS
    CAT_VERDURAS -> Categoria.VERDURAS
    else -> Categoria.FRUTAS
}

// Quick fix p/ demo: el backend aún no tiene flag `organico` en `products`. Lo
// derivamos por imageKey (mismo set que en DemoCatalog). Cuando se agregue la
// columna real en una migration Flyway, este Set desaparece y leemos de ProductDto.
private val ORGANIC_IMAGE_KEYS = setOf("palta_hass", "lechuga", "cilantro")

/** Convierte el DTO del backend al modelo de UI que ya usa el Home. */
fun ProductDto.toProducto(): Producto = Producto(
    id = id,
    nombre = name,
    precioClp = priceClp,
    unidad = unit,
    categoria = categoriaFor(categoryId),
    imagen = drawableForImageKey(imageKey),
    organico = imageKey in ORGANIC_IMAGE_KEYS
)
