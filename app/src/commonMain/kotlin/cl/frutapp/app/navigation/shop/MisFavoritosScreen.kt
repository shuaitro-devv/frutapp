@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.DemoCatalog
import cl.frutapp.app.data.FavoritesStore
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.ui.components.FrutLoader
import cl.frutapp.app.ui.components.ProductCard
import cl.frutapp.app.ui.theme.FrutAppColors
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Mis favoritos (dummy): los productos marcados con el corazón. Lista reactiva al store. */
class MisFavoritosScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        // Resolvemos los ids favoritos contra el catálogo del backend (uuid) y el mock local
        // (slug), para cubrir productos venidos de cualquiera de los dos orígenes.
        var catalogo by remember { mutableStateOf<Map<String, Producto>?>(null) }
        LaunchedEffect(Unit) {
            val backendResult = runCatching { CatalogApi().products() }
            val backend = backendResult.getOrNull()?.map { it.toProducto() } ?: run {
                backendResult.exceptionOrNull()?.let {
                    cl.frutapp.app.ui.ErrorReporter.report(screen = "MisFavoritos", action = "load_catalog", error = it)
                }
                emptyList()
            }
            catalogo = (backend + DemoCatalog.productos).associateBy { it.id }
        }

        val mapa = catalogo
        val favoritos = if (mapa == null) null else FavoritesStore.items.mapNotNull { mapa[it] }.reversed()

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
                    }
                    Text("Mis favoritos", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                when {
                    favoritos == null -> Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) { FrutLoader() }
                    favoritos.isEmpty() -> VacioFavoritos()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        items(favoritos.chunked(2)) { fila ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                fila.forEach { producto ->
                                    val defGramos = if (producto.unidad == "kg") 1000 else null
                                    val linea = CartStore.items.firstOrNull { it.producto.id == producto.id && it.gramos == defGramos }
                                    ProductCard(
                                        name = producto.nombre,
                                        price = formatClp(producto.precioClp),
                                        image = producto.imagen,
                                        unit = producto.unidad,
                                        onAdd = { CartStore.add(producto, 1, defGramos) },
                                        onClick = { navigator.push(ProductDetailScreen(producto)) },
                                        quantity = linea?.cantidad ?: 0,
                                        onIncrement = { CartStore.add(producto, 1, defGramos) },
                                        onDecrement = { linea?.let { CartStore.setCantidad(it, it.cantidad - 1) } },
                                        disponible = producto.disponible,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (fila.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VacioFavoritos() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.FavoriteBorder, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(36.dp))
        }
        Text("Aún no tienes favoritos", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Toca el corazón en un producto para guardarlo acá y encontrarlo rápido.",
            color = FrutAppColors.InkMuted, fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

