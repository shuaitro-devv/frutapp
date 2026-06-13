@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.data

import cl.frutapp.shared.dto.ProductDto
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.ajo
import frutapp.app.generated.resources.albahaca
import frutapp.app.generated.resources.brocoli
import frutapp.app.generated.resources.canasta_frutas
import frutapp.app.generated.resources.cebolla
import frutapp.app.generated.resources.choclo
import frutapp.app.generated.resources.cilantro
import frutapp.app.generated.resources.coliflor
import frutapp.app.generated.resources.frutillas
import frutapp.app.generated.resources.huevos
import frutapp.app.generated.resources.jengibre
import frutapp.app.generated.resources.kiwi
import frutapp.app.generated.resources.lechuga
import frutapp.app.generated.resources.limon
import frutapp.app.generated.resources.mandarinas
import frutapp.app.generated.resources.mango
import frutapp.app.generated.resources.manzana_roja
import frutapp.app.generated.resources.manzana_verde
import frutapp.app.generated.resources.melon
import frutapp.app.generated.resources.menta
import frutapp.app.generated.resources.miel
import frutapp.app.generated.resources.naranja
import frutapp.app.generated.resources.oregano
import frutapp.app.generated.resources.palta_hass
import frutapp.app.generated.resources.papa
import frutapp.app.generated.resources.pepino
import frutapp.app.generated.resources.pera
import frutapp.app.generated.resources.perejil
import frutapp.app.generated.resources.pimenton_verde
import frutapp.app.generated.resources.pimiento_rojo
import frutapp.app.generated.resources.pina
import frutapp.app.generated.resources.platano
import frutapp.app.generated.resources.romero
import frutapp.app.generated.resources.sandia
import frutapp.app.generated.resources.tomate
import frutapp.app.generated.resources.uvas
import frutapp.app.generated.resources.zanahoria
import frutapp.app.generated.resources.zapallo_italiano
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Mapea el `imageKey` (string del backend) al drawable empaquetado en la app.
 * Al sumar productos nuevos hay que agregar la foto a composeResources y un caso acá.
 */
fun drawableForImageKey(key: String): DrawableResource = when (key) {
    // Frutas
    "frutillas" -> Res.drawable.frutillas
    "kiwi" -> Res.drawable.kiwi
    "limon" -> Res.drawable.limon
    "mandarinas" -> Res.drawable.mandarinas
    "mango" -> Res.drawable.mango
    "manzana_roja" -> Res.drawable.manzana_roja
    "manzana_verde" -> Res.drawable.manzana_verde
    "melon" -> Res.drawable.melon
    "naranja" -> Res.drawable.naranja
    "palta_hass" -> Res.drawable.palta_hass
    "pera" -> Res.drawable.pera
    "pina" -> Res.drawable.pina
    "platano" -> Res.drawable.platano
    "sandia" -> Res.drawable.sandia
    "uvas" -> Res.drawable.uvas
    // Verduras
    "ajo" -> Res.drawable.ajo
    "brocoli" -> Res.drawable.brocoli
    "cebolla" -> Res.drawable.cebolla
    "choclo" -> Res.drawable.choclo
    "coliflor" -> Res.drawable.coliflor
    "lechuga" -> Res.drawable.lechuga
    "papa" -> Res.drawable.papa
    "pepino" -> Res.drawable.pepino
    "pimenton_verde" -> Res.drawable.pimenton_verde
    "pimiento_rojo" -> Res.drawable.pimiento_rojo
    "tomate" -> Res.drawable.tomate
    "zanahoria" -> Res.drawable.zanahoria
    "zapallo_italiano" -> Res.drawable.zapallo_italiano
    // Hierbas
    "albahaca" -> Res.drawable.albahaca
    "cilantro" -> Res.drawable.cilantro
    "menta" -> Res.drawable.menta
    "oregano" -> Res.drawable.oregano
    "perejil" -> Res.drawable.perejil
    "romero" -> Res.drawable.romero
    // Despensa
    "huevos" -> Res.drawable.huevos
    "jengibre" -> Res.drawable.jengibre
    "miel" -> Res.drawable.miel
    else -> Res.drawable.canasta_frutas
}

// IDs de categoría del seed (V1__init.sql + V9__catalog_expansion.sql). Si cambian, ajustar acá.
private const val CAT_FRUTAS = "11111111-1111-1111-1111-111111111111"
private const val CAT_VERDURAS = "22222222-2222-2222-2222-222222222222"
private const val CAT_HIERBAS = "33333333-3333-3333-3333-333333333333"
private const val CAT_DESPENSA = "44444444-4444-4444-4444-444444444444"

private fun categoriaFor(categoryId: String): Categoria = when (categoryId) {
    CAT_FRUTAS -> Categoria.FRUTAS
    CAT_VERDURAS -> Categoria.VERDURAS
    CAT_HIERBAS -> Categoria.HIERBAS
    CAT_DESPENSA -> Categoria.DESPENSA
    else -> Categoria.FRUTAS
}

// Quick fix p/ demo: el backend aún no tiene flag `organico` en `products`. Lo
// derivamos por imageKey. Cuando se agregue la columna real en una migration Flyway,
// este Set desaparece y leemos de ProductDto.
private val ORGANIC_IMAGE_KEYS = setOf(
    "palta_hass", "lechuga", "cilantro", "manzana_verde",
    "albahaca", "menta", "oregano", "perejil",
    "huevos", "miel"
)

/**
 * Convierte el DTO del backend al modelo de UI. Usamos `slug` como id local en vez del
 * UUID porque slug es estable y human-readable — los Packs de Ofertas y el
 * DemoCatalog usan slugs como identificadores. CheckoutScreen detecta si el id no es
 * UUID y hace lookup por nombre antes de enviar a la API.
 */
fun ProductDto.toProducto(): Producto = Producto(
    id = slug,
    nombre = name,
    precioClp = priceClp,
    unidad = unit,
    categoria = categoriaFor(categoryId),
    imagen = drawableForImageKey(imageKey),
    organico = imageKey in ORGANIC_IMAGE_KEYS,
    disponible = disponible
)
