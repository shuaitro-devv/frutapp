@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.Categoria
import cl.frutapp.app.data.DemoCatalog
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.navigation.shop.ProductDetailScreen
import cl.frutapp.app.ui.theme.FrutAppColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Buscador: lista productos del backend (con fallback a DemoCatalog si no hay red) y
 * filtra por nombre conforme el usuario escribe. Resultados con foto + precio + tap.
 *
 * @param categoriaPrefiltro si viene, la pantalla arranca filtrada por esa categoría y
 *   solo busca dentro de ella; el header muestra el nombre de la categoría en vez de
 *   "Todos los productos". Lo usan los chips de categoría del Home.
 */
class BuscadorScreen(private val categoriaPrefiltro: Categoria? = null) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var query by remember { mutableStateOf("") }
        var catalogo by remember { mutableStateOf<List<Producto>>(DemoCatalog.productos) }
        val focusRequester = remember { FocusRequester() }

        // Carga real desde backend; si falla queda el mock como fallback.
        LaunchedEffect(Unit) {
            runCatching { CatalogApi().products() }
                .onSuccess { dtos -> if (dtos.isNotEmpty()) catalogo = dtos.map { it.toProducto() } }
                .onFailure { e -> cl.frutapp.app.ui.ErrorReporter.report(screen = "Buscador", action = "load_catalog", error = e) }
            // Si la pantalla viene prefiltrada por una categoría no enfocamos el campo: el
            // usuario está navegando por categoría, no quería teclado a la cara.
            if (categoriaPrefiltro == null) focusRequester.requestFocus()
        }

        val universo = remember(catalogo, categoriaPrefiltro) {
            if (categoriaPrefiltro == null) catalogo else catalogo.filter { it.categoria == categoriaPrefiltro }
        }
        val resultados = remember(universo, query) {
            val q = query.trim().lowercase()
            if (q.isBlank()) universo
            else universo.filter { it.nombre.lowercase().contains(q) || it.categoria.label.lowercase().contains(q) }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar con back + input + clear
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
                    Row(
                        modifier = Modifier.weight(1f).padding(start = 12.dp).height(48.dp)
                            .background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = FrutAppColors.InkMuted, modifier = Modifier.size(20.dp))
                        Box(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            if (query.isEmpty()) Text(
                                text = categoriaPrefiltro?.let { "Buscar en ${it.label.lowercase()}…" } ?: "Buscar frutas o verduras…",
                                color = FrutAppColors.InkSoft, fontSize = 14.sp
                            )
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 14.sp),
                                cursorBrush = SolidColor(FrutAppColors.Brand400),
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        }
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Limpiar",
                                tint = FrutAppColors.InkMuted,
                                modifier = Modifier.size(18.dp).clickable { query = "" }
                            )
                        }
                    }
                }

                if (resultados.isEmpty()) {
                    EmptyResultados(query, categoriaPrefiltro, universoVacio = universo.isEmpty())
                } else {
                    val encabezado = when {
                        query.isNotBlank() -> "${resultados.size} resultado${if (resultados.size != 1) "s" else ""}"
                        categoriaPrefiltro != null -> "${categoriaPrefiltro.label} · ${resultados.size}"
                        else -> "Todos los productos · ${resultados.size}"
                    }
                    Text(
                        encabezado,
                        color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                    )
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                        items(resultados, key = { it.id }) { producto ->
                            ResultadoRow(producto, onClick = { navigator.push(ProductDetailScreen(producto)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultadoRow(producto: Producto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(producto.imagen), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(50.dp).padding(6.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(producto.nombre, color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(producto.categoria.label, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
        }
        Text("${formatClp(producto.precioClp)}/${producto.unidad}", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
    }
}

@Composable
private fun EmptyResultados(query: String, categoria: Categoria?, universoVacio: Boolean) {
    val (titulo, detalle) = when {
        query.isNotBlank() -> "Sin resultados" to "No encontramos productos para \"$query\"."
        categoria != null && universoVacio -> "Aún no hay ${categoria.label.lowercase()}" to "Estamos sumando proveedores y muy pronto los verás acá."
        else -> "Sin resultados" to "Aún no podemos cargar el catálogo."
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔍", fontSize = 44.sp)
        Text(titulo, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Text(detalle, color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
    }
}
