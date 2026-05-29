@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.Categoria
import cl.frutapp.app.data.DemoCatalog
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.navigation.catalog.CatalogScreen
import cl.frutapp.app.navigation.offers.OfertasScreen
import cl.frutapp.app.navigation.orders.MisPedidosScreen
import cl.frutapp.app.navigation.profile.ProfileScreen
import cl.frutapp.app.navigation.recycle.ReciclaScreen
import cl.frutapp.app.navigation.rewards.FrutCoinsScreen
import cl.frutapp.app.navigation.shop.CartScreen
import cl.frutapp.app.navigation.shop.ProductDetailScreen
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.components.ProductCard
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.canasta_frutas
import frutapp.app.generated.resources.cilantro
import frutapp.app.generated.resources.hoja_decorativa
import frutapp.app.generated.resources.lechuga
import frutapp.app.generated.resources.manzana_roja
import frutapp.app.generated.resources.zanahoria
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Home (mockup 06): saludo, búsqueda, banner hero, categorías y productos destacados.
 * Datos desde [DemoCatalog] (mock) hasta conectar el backend de catálogo.
 */
class HomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedTab by remember { mutableStateOf(FrutTab.INICIO) }
        // Catálogo real desde el backend; si falla la red, queda el mock como fallback.
        var destacados by remember { mutableStateOf(DemoCatalog.destacados) }
        LaunchedEffect(Unit) {
            runCatching { CatalogApi().products() }
                .onSuccess { dtos -> if (dtos.isNotEmpty()) destacados = dtos.map { it.toProducto() }.take(6) }
        }

        Scaffold(
            bottomBar = {
                FrutBottomNav(
                    selected = selectedTab,
                    onSelect = { tab ->
                        selectedTab = tab
                        when (tab) {
                            FrutTab.EXPLORAR -> navigator.push(CatalogScreen())
                            FrutTab.CARRITO -> navigator.push(CartScreen())
                            FrutTab.PEDIDOS -> navigator.push(MisPedidosScreen())
                            FrutTab.PERFIL -> navigator.push(ProfileScreen())
                            else -> {}
                        }
                    }
                )
            },
            containerColor = Color.White
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { HomeHeader() }
                item { SearchBarMock(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                item { HeroBanner(modifier = Modifier.padding(horizontal = 20.dp), onClick = { navigator.push(OfertasScreen()) }) }
                item {
                    QuickAccess(
                        onOfertas = { navigator.push(OfertasScreen()) },
                        onFrutCoins = { navigator.push(FrutCoinsScreen()) },
                        onRecicla = { navigator.push(ReciclaScreen()) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
                item { SectionHeader("Categorías", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp)) }
                item { CategoriesRow(modifier = Modifier.padding(vertical = 8.dp)) }
                item { SectionHeader("Productos destacados", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) }
                items(destacados.chunked(2)) { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        fila.forEach { producto ->
                            ProductCard(
                                name = producto.nombre,
                                price = formatClp(producto.precioClp),
                                image = producto.imagen,
                                unit = producto.unidad,
                                onAdd = { CartStore.add(producto, 1, if (producto.unidad == "kg") 1000 else null) },
                                onClick = { navigator.push(ProductDetailScreen(producto)) },
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

@Composable
private fun HomeHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = TokenStore.user?.name?.substringBefore(' ')?.let { "¡Hola, $it! 👋" } ?: "¡Hola! 👋",
                style = MaterialTheme.typography.headlineSmall,
                color = FrutAppColors.Brand800
            )
            Text(
                text = "¿Qué productos frescos buscas hoy?",
                style = MaterialTheme.typography.bodyMedium,
                color = FrutAppColors.InkMuted
            )
        }
        HeaderIcon(Icons.Default.Notifications)
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(FrutAppColors.Brand50, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QuickAccess(
    onOfertas: () -> Unit,
    onFrutCoins: () -> Unit,
    onRecicla: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickItem("Ofertas", Icons.Default.LocalOffer, FrutAppColors.Brand400, onOfertas, Modifier.weight(1f))
        QuickItem("FrutCoins", Icons.Default.MonetizationOn, FrutAppColors.AmberCoin, onFrutCoins, Modifier.weight(1f))
        QuickItem("Recicla", Icons.Default.Recycling, FrutAppColors.Brand600, onRecicla, Modifier.weight(1f))
    }
}

@Composable
private fun QuickItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FrutAppColors.Brand50)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = FrutAppColors.Brand800, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SearchBarMock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FrutAppColors.Cream)
            .clickable { }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = FrutAppColors.InkMuted, modifier = Modifier.size(20.dp))
        Text(
            text = "Buscar frutas o verduras…",
            style = MaterialTheme.typography.bodyMedium,
            color = FrutAppColors.InkSoft,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

@Composable
private fun HeroBanner(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(165.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(listOf(FrutAppColors.Brand800, FrutAppColors.Brand600))
            )
            .clickable(onClick = onClick)
    ) {
        // Canasta sangrando por el borde derecho (imagen transparente)
        Image(
            painter = painterResource(Res.drawable.canasta_frutas),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 14.dp)
                .size(180.dp)
        )
        // Hojas decorativas flotando (como el mockup)
        Image(
            painter = painterResource(Res.drawable.hoja_decorativa),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-6).dp, y = (-8).dp)
                .size(58.dp)
                .rotate(25f)
                .alpha(0.9f)
        )
        Image(
            painter = painterResource(Res.drawable.hoja_decorativa),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-12).dp, y = 10.dp)
                .size(48.dp)
                .rotate(205f)
                .alpha(0.45f)
        )
        // Texto + CTA
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 22.dp, end = 8.dp)
                .fillMaxWidth(0.6f)
        ) {
            Text(
                text = "Frescura que se nota,",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "calidad que te acompaña",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "Ver ofertas",
                    style = MaterialTheme.typography.labelLarge,
                    color = FrutAppColors.Brand600,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = FrutAppColors.Brand800, fontWeight = FontWeight.Bold)
        Text(
            "Ver todo",
            style = MaterialTheme.typography.labelLarge,
            color = FrutAppColors.Brand600,
            modifier = Modifier.clickable { }
        )
    }
}

private data class CategoriaUi(val label: String, val imagen: DrawableResource)

@Composable
private fun CategoriesRow(modifier: Modifier = Modifier) {
    val categorias = listOf(
        CategoriaUi(Categoria.FRUTAS.label, Res.drawable.manzana_roja),
        CategoriaUi(Categoria.VERDURAS.label, Res.drawable.zanahoria),
        CategoriaUi(Categoria.HIERBAS.label, Res.drawable.cilantro),
        CategoriaUi("Orgánicos", Res.drawable.lechuga)
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        categorias.forEach { cat ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(FrutAppColors.Brand50),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(cat.imagen),
                        contentDescription = cat.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(46.dp).padding(4.dp)
                    )
                }
                Text(
                    text = cat.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = FrutAppColors.Ink,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
