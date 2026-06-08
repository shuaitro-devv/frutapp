@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.Categoria
import cl.frutapp.app.data.Producto
import cl.frutapp.app.navigation.shop.ProductDetailScreen
import cl.frutapp.app.ui.components.ProductCard
import cl.frutapp.app.ui.theme.BrandCatalogs
import cl.frutapp.app.ui.theme.BrandProduct
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.brandProductDrawable
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.manzana_roja
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Pantalla de categoria white-label (Sofruco). Muestra los productos del
 * catalogo demo filtrados por categoria — funciona porque [BrandCatalog]
 * tiene los 30 productos curados con su imagen real, sin pegarle al backend.
 *
 * Cuando el usuario agrega al carrito, los productos viajan como [Producto]
 * normales y caen al [CartStore]. Al hacer checkout en modo Sofruco, el
 * [CheckoutScreen] intercepta con el modal "Vista demo Sofruco" antes de
 * pegarle al backend (que rechazaria estos IDs).
 *
 * Para FrutApp esta pantalla no se usa — el flujo de catalogo real va por
 * [BuscadorScreen] que pega al backend.
 */
class BrandCategoryScreen(
    private val categoryId: String,
    private val categoryLabel: String,
    private val categoryEmoji: String
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val productos = BrandCatalogs.sofrucoProducts.filter { it.categoriaId == categoryId }

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background)) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Box(
                    modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(categoryEmoji, fontSize = 18.sp)
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(categoryLabel, color = FrutAppColors.Brand800, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "${productos.size} productos",
                        color = FrutAppColors.InkMuted,
                        fontSize = 12.sp
                    )
                }
            }
            if (productos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin productos en esta categoría aún.", color = FrutAppColors.InkMuted)
                }
                return
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(productos, key = { it.id }) { bp ->
                    val producto = bp.toProducto()
                    val linea = CartStore.items.firstOrNull { it.producto.id == producto.id }
                    ProductCard(
                        name = producto.nombre,
                        price = formatCLP(producto.precioClp),
                        image = producto.imagen,
                        unit = producto.unidad,
                        onAdd = { CartStore.add(producto, 1, null) },
                        onClick = { navigator.push(ProductDetailScreen(producto)) },
                        quantity = linea?.cantidad ?: 0,
                        onIncrement = { CartStore.add(producto, 1, null) },
                        onDecrement = { linea?.let { CartStore.setCantidad(it, it.cantidad - 1) } }
                    )
                }
            }
        }
    }
}

private fun BrandProduct.toProducto(): Producto = Producto(
    id = id,
    nombre = nombre,
    precioClp = precioCLP,
    unidad = unidad,
    categoria = Categoria.FRUTAS,
    imagen = brandProductDrawable(imageKey) ?: Res.drawable.manzana_roja,
    organico = false,
    brandCategoryId = categoriaId
)

private fun formatCLP(monto: Int): String {
    val s = monto.toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(s[i])
    }
    return "$$sb"
}
