@file:OptIn(ExperimentalResourceApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import cl.frutapp.app.ui.theme.BrandCatalogs
import cl.frutapp.app.ui.theme.BrandProduct
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.LocalBrand
import cl.frutapp.app.ui.theme.SofrucoBrand
import cl.frutapp.app.ui.theme.brandProductDrawable
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.manzana_roja
import org.jetbrains.compose.resources.ExperimentalResourceApi

private data class Filtro(val label: String, val match: (Producto) -> Boolean)

private enum class OrdenCatalogo(val label: String) {
    RECOMENDADOS("Recomendados"),
    PRECIO_ASC("Precio: menor a mayor"),
    PRECIO_DESC("Precio: mayor a menor"),
    NOMBRE_AZ("Nombre A-Z")
}

/**
 * Catálogo (mockup 07): búsqueda + chips de categoría con filtrado en cliente sobre el
 * catálogo (backend con fallback a [DemoCatalog]). Tab "Explorar" del bottom nav.
 */
class CatalogScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val brand = LocalBrand.current
        val esSofruco = brand.id == SofrucoBrand.id
        // Productos: en FrutApp arrancan con el mock y se reemplazan por el backend
        // cuando contesta; en Sofruco salen del catalogo brand mapeado a Producto
        // (con brandCategoryId seteado para que los filtros por categoria funcionen).
        var productos by remember(brand.id) {
            mutableStateOf(
                if (esSofruco) BrandCatalogs.sofrucoProducts.map { it.toProductoSofrucoLocal() }
                else DemoCatalog.productos
            )
        }
        LaunchedEffect(brand.id) {
            if (esSofruco) return@LaunchedEffect
            runCatching { CatalogApi().products() }
                .onSuccess { dtos -> if (dtos.isNotEmpty()) productos = dtos.map { it.toProducto() } }
                .onFailure { e -> cl.frutapp.app.ui.ErrorReporter.report(screen = "Catalog", action = "load_catalog", error = e) }
        }

        val filtros = remember(brand.id) {
            if (esSofruco) {
                // Filtros Sofruco: "Todos" + las 6 categorias del brand.
                val cats = BrandCatalogs.categoriesFor(brand)
                buildList {
                    add(Filtro("Todos") { true })
                    cats.forEach { c ->
                        add(Filtro(c.label) { it.brandCategoryId == c.id })
                    }
                }
            } else {
                listOf(
                    Filtro("Todos") { true },
                    Filtro("Frutas") { it.categoria == Categoria.FRUTAS },
                    Filtro("Verduras") { it.categoria == Categoria.VERDURAS },
                    Filtro("Hierbas") { it.categoria == Categoria.HIERBAS },
                    Filtro("Orgánicos") { it.organico }
                )
            }
        }
        var filtroSel by remember(brand.id) { mutableStateOf(0) }
        var query by remember { mutableStateOf("") }
        var orden by remember { mutableStateOf(OrdenCatalogo.RECOMENDADOS) }
        var soloOrganicos by remember { mutableStateOf(false) }
        var sheetAbierto by remember { mutableStateOf(false) }
        val filtrosActivos = orden != OrdenCatalogo.RECOMENDADOS || soloOrganicos

        val visibles = productos
            .filter { filtros[filtroSel].match(it) && it.nombre.contains(query.trim(), ignoreCase = true) }
            .filter { !soloOrganicos || it.organico }
            .let { lista ->
                when (orden) {
                    OrdenCatalogo.RECOMENDADOS -> lista
                    OrdenCatalogo.PRECIO_ASC -> lista.sortedBy { it.precioClp }
                    OrdenCatalogo.PRECIO_DESC -> lista.sortedByDescending { it.precioClp }
                    OrdenCatalogo.NOMBRE_AZ -> lista.sortedBy { it.nombre.lowercase() }
                }
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
                        Text(
                            text = if (esSofruco) "Productos de origen ${brand.displayName}" else "Frescura seleccionada para ti",
                            color = FrutAppColors.InkMuted,
                            fontSize = 13.sp
                        )
                    }
                    Box(modifier = Modifier.size(44.dp)) {
                        Box(
                            modifier = Modifier.size(44.dp).background(FrutAppColors.Brand50, CircleShape).clickable { sheetAbierto = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = "Filtros", tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
                        }
                        // Indicador visual cuando hay un orden o filtro distinto del default activo.
                        if (filtrosActivos) {
                            Box(
                                modifier = Modifier.size(12.dp)
                                    .background(FrutAppColors.Brand600, CircleShape)
                                    .border(2.dp, Color.White, CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
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
                                val defGramos = if (p.unidad == "kg") 1000 else null
                                val linea = CartStore.items.firstOrNull { it.producto.id == p.id && it.gramos == defGramos }
                                ProductCard(
                                    name = p.nombre,
                                    price = formatClp(p.precioClp),
                                    image = p.imagen,
                                    unit = p.unidad,
                                    onAdd = { CartStore.add(p, 1, defGramos) },
                                    onClick = { navigator.push(ProductDetailScreen(p)) },
                                    quantity = linea?.cantidad ?: 0,
                                    onIncrement = { CartStore.add(p, 1, defGramos) },
                                    onDecrement = { linea?.let { CartStore.setCantidad(it, it.cantidad - 1) } },
                                    disponible = p.disponible,
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

            if (sheetAbierto) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { sheetAbierto = false },
                    sheetState = sheetState,
                    containerColor = Color.White
                ) {
                    FiltrosSheet(
                        orden = orden,
                        onOrden = { orden = it },
                        soloOrganicos = soloOrganicos,
                        onSoloOrganicos = { soloOrganicos = it },
                        mostrarOrganicos = !esSofruco,
                        onLimpiar = {
                            orden = OrdenCatalogo.RECOMENDADOS
                            soloOrganicos = false
                        },
                        onAplicar = { sheetAbierto = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun FiltrosSheet(
    orden: OrdenCatalogo,
    onOrden: (OrdenCatalogo) -> Unit,
    soloOrganicos: Boolean,
    onSoloOrganicos: (Boolean) -> Unit,
    mostrarOrganicos: Boolean = true,
    onLimpiar: () -> Unit,
    onAplicar: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Filtros y orden", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Limpiar",
                color = FrutAppColors.Brand600,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onLimpiar)
            )
        }

        Text(
            "Ordenar por",
            color = FrutAppColors.Brand800,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
        )
        OrdenCatalogo.values().forEach { opcion ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOrden(opcion) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (opcion == orden) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (opcion == orden) FrutAppColors.Brand600 else FrutAppColors.InkMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    opcion.label,
                    color = FrutAppColors.Ink,
                    fontSize = 14.sp,
                    fontWeight = if (opcion == orden) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        if (mostrarOrganicos) {
            Text(
                "Filtrar por",
                color = FrutAppColors.Brand800,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onSoloOrganicos(!soloOrganicos) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Solo orgánicos", color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Productos con certificación o de huerto familiar.",
                        color = FrutAppColors.InkMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = soloOrganicos,
                    onCheckedChange = onSoloOrganicos,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = FrutAppColors.Brand600,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = FrutAppColors.InkSoft
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp)
                .background(FrutAppColors.Brand400, RoundedCornerShape(14.dp))
                .clickable(onClick = onAplicar),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Aplicar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
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

/** Mapper local al modelo [Producto]; igual al de Home y BrandCategoryScreen.
 *  Se queda local para no exportar helpers de mapping desde el modulo theme,
 *  que es para tokens visuales/branding, no para data. */
private fun BrandProduct.toProductoSofrucoLocal(): Producto = Producto(
    id = id,
    nombre = nombre,
    precioClp = precioCLP,
    unidad = unidad,
    categoria = Categoria.FRUTAS,
    imagen = brandProductDrawable(imageKey) ?: Res.drawable.manzana_roja,
    organico = false,
    brandCategoryId = categoriaId
)
