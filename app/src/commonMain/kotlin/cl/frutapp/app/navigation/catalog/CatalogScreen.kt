@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.Categoria
import cl.frutapp.app.data.DemoCatalog
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.navigation.profile.ProfileScreen
import cl.frutapp.app.navigation.shop.CartScreen
import cl.frutapp.app.navigation.shop.ProductDetailScreen
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.components.ProductCard
import androidx.compose.runtime.LaunchedEffect
import cl.frutapp.app.ui.theme.FrutAppColors
import org.jetbrains.compose.resources.ExperimentalResourceApi

private data class Filtro(val label: String, val match: (Producto) -> Boolean)

/**
 * Catálogo (mockup 07): búsqueda + chips de categoría con filtrado en cliente sobre el
 * catálogo (backend con fallback a [DemoCatalog]). Tab "Explorar" del bottom nav.
 */
class CatalogScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var productos by remember { mutableStateOf(DemoCatalog.productos) }
        LaunchedEffect(Unit) {
            runCatching { CatalogApi().products() }
                .onSuccess { dtos -> if (dtos.isNotEmpty()) productos = dtos.map { it.toProducto() } }
        }

        val filtros = remember {
            listOf(
                Filtro("Todos") { true },
                Filtro("Frutas") { it.categoria == Categoria.FRUTAS },
                Filtro("Verduras") { it.categoria == Categoria.VERDURAS },
                Filtro("Hierbas") { it.categoria == Categoria.HIERBAS },
                Filtro("Orgánicos") { it.organico }
            )
        }
        var filtroSel by remember { mutableStateOf(0) }
        var query by remember { mutableStateOf("") }

        val visibles = productos.filter {
            filtros[filtroSel].match(it) && it.nombre.contains(query.trim(), ignoreCase = true)
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Catálogo", color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Frescura seleccionada para ti", color = FrutAppColors.InkMuted, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier.size(44.dp).background(FrutAppColors.Brand50, CircleShape).clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "Filtros", tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
                    }
                }

                SearchBar(query = query, onQuery = { query = it }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))

                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtros.size) { i ->
                        CategoriaChip(label = filtros[i].label, selected = filtroSel == i, onClick = { filtroSel = i })
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 16.dp)
                ) {
                    item {
                        Text(
                            "${filtros[filtroSel].label} · ${visibles.size}",
                            color = FrutAppColors.Brand800,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                        )
                    }
                    if (visibles.isEmpty()) {
                        item {
                            Text(
                                "No encontramos productos para tu búsqueda.",
                                color = FrutAppColors.InkMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                    items(visibles.chunked(2)) { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            fila.forEach { p ->
                                ProductCard(
                                    name = p.nombre,
                                    price = formatClp(p.precioClp),
                                    image = p.imagen,
                                    unit = p.unidad,
                                    onAdd = { CartStore.add(p, 1, if (p.unidad == "kg") 1000 else null) },
                                    onClick = { navigator.push(ProductDetailScreen(p)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(16.dp)
                        ) {
                            Text("Productos frescos directo a tu hogar", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                FrutBottomNav(
                    selected = FrutTab.EXPLORAR,
                    onSelect = { tab ->
                        when (tab) {
                            FrutTab.EXPLORAR -> {}
                            FrutTab.CARRITO -> navigator.push(CartScreen())
                            FrutTab.PERFIL -> navigator.push(ProfileScreen())
                            else -> navigator.popUntilRoot()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(50.dp).background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = FrutAppColors.InkMuted, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            if (query.isEmpty()) {
                Text("Buscar frutas o verduras…", color = FrutAppColors.InkSoft, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 14.sp),
                cursorBrush = SolidColor(FrutAppColors.Brand400),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoriaChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) FrutAppColors.Brand400 else Color.White, CircleShape)
            .border(1.dp, if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand100, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Color.White else FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
