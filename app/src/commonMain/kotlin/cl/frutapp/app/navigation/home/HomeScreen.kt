@file:OptIn(ExperimentalResourceApi::class, ExperimentalFoundationApi::class)

package cl.frutapp.app.navigation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingBasket
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import cl.frutapp.app.navigation.canastas.MisCanastasScreen
import cl.frutapp.app.navigation.catalog.CatalogScreen
import cl.frutapp.app.navigation.offers.OfertasScreen
import cl.frutapp.app.navigation.orders.MisPedidosScreen
import cl.frutapp.app.navigation.profile.ProfileScreen
import cl.frutapp.app.navigation.recycle.ReciclaScreen
import cl.frutapp.app.navigation.rewards.FrutCoinsScreen
import cl.frutapp.app.navigation.shop.CartScreen
import cl.frutapp.app.navigation.shop.MisFavoritosScreen
import cl.frutapp.app.navigation.shop.ProductDetailScreen
import cl.frutapp.app.ui.components.BrandWaveHeader
import cl.frutapp.app.data.CoachmarkStore
import cl.frutapp.app.ui.CoachmarkOverlay
import cl.frutapp.app.ui.CoachmarkStep
import cl.frutapp.app.ui.coachmarkTarget
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.components.ProductCard
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.banner_frescos
import frutapp.app.generated.resources.banner_fruit
import frutapp.app.generated.resources.banner_frutcoins
import frutapp.app.generated.resources.canasta_frutas
import frutapp.app.generated.resources.cilantro
import frutapp.app.generated.resources.hoja_decorativa
import frutapp.app.generated.resources.lechuga
import frutapp.app.generated.resources.manzana_roja
import frutapp.app.generated.resources.naranja
import frutapp.app.generated.resources.palta_hass
import frutapp.app.generated.resources.platano
import frutapp.app.generated.resources.zanahoria
import org.jetbrains.compose.resources.DrawableResource
import kotlinx.coroutines.delay
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

        Box(modifier = Modifier.fillMaxSize()) {
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
            containerColor = FrutAppColors.Background
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HomeLeaves()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { HomeHeader(modifier = Modifier.coachmarkTarget("header"), onFavoritos = { navigator.push(MisFavoritosScreen()) }, onNotificaciones = { navigator.push(NotificacionesScreen()) }, onCanastas = { navigator.push(MisCanastasScreen()) }, onBuscar = { navigator.push(cl.frutapp.app.navigation.catalog.BuscadorScreen()) }) }
                item {
                    HeroCarousel(
                        onOfertas = { navigator.push(OfertasScreen()) },
                        onFrutCoins = { navigator.push(FrutCoinsScreen()) },
                        onCanastas = { navigator.push(MisCanastasScreen()) },
                        onReyVegetal = { navigator.push(cl.frutapp.app.navigation.rewards.ReyVegetalScreen()) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).coachmarkTarget("hero")
                    )
                }
                item {
                    QuickAccess(
                        onOfertas = { navigator.push(OfertasScreen()) },
                        onFrutCoins = { navigator.push(FrutCoinsScreen()) },
                        onRecicla = { navigator.push(ReciclaScreen()) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp).coachmarkTarget("quick_access")
                    )
                }
                item { SectionHeader("Categorías", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 4.dp), onVerTodo = { navigator.push(CatalogScreen()) }) }
                item { CategoriesRow(modifier = Modifier.padding(vertical = 8.dp).coachmarkTarget("categorias"), onCategoria = { cat -> navigator.push(cl.frutapp.app.navigation.catalog.BuscadorScreen(categoriaPrefiltro = cat)) }) }
                item { SectionHeader("Productos destacados", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp), onVerTodo = { navigator.push(CatalogScreen()) }) }
                items(destacados.chunked(2)) { fila ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
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
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (fila.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            HomeBottomFruits()
            }
        }

        // Tour de bienvenida (coachmark): renderizado FUERA del Scaffold para que el
        // overlay oscuro cubra TODO (incluye la barra de navegación inferior y el botón
        // carrito flotante). Solo el target del paso actual queda iluminado.
        LaunchedEffect(Unit) { CoachmarkStore.maybeAutoStart() }
        CoachmarkOverlay(HOME_TOUR_STEPS)
        }
    }
}

private val HOME_TOUR_STEPS = listOf(
    CoachmarkStep(
        key = "header",
        titulo = "¡Hola! Bienvenido a FrutApp",
        texto = "Acá arriba tienes el buscador y tus accesos rápidos: favoritos, canastas y notificaciones."
    ),
    CoachmarkStep(
        key = "hero",
        titulo = "Promos y novedades",
        texto = "Banner con ofertas, canastas listas para pedir y el Rey Vegetal del mes."
    ),
    CoachmarkStep(
        key = "quick_access",
        titulo = "Tu corazón verde",
        texto = "Ofertas, FrutCoins y Recicla. Esto último es lo que nos hace distintos."
    ),
    CoachmarkStep(
        key = "categorias",
        titulo = "Explora por tipo",
        texto = "Frutas, verduras, hierbas y orgánicos. Toca una para ver el catálogo filtrado."
    ),
    CoachmarkStep(
        key = "cart",
        titulo = "Tu carrito siempre a mano",
        texto = "El botón flotante te lleva al carrito desde cualquier pantalla. ¡Listo para empezar!"
    )
)

@Composable
private fun BoxScope.HomeBottomFruits() {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .offset(y = 34.dp)
            .alpha(0.9f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        Image(painterResource(Res.drawable.platano), null, Modifier.size(72.dp), contentScale = ContentScale.Fit)
        Image(painterResource(Res.drawable.manzana_roja), null, Modifier.size(64.dp), contentScale = ContentScale.Fit)
        Image(painterResource(Res.drawable.naranja), null, Modifier.size(60.dp), contentScale = ContentScale.Fit)
        Image(painterResource(Res.drawable.palta_hass), null, Modifier.size(64.dp), contentScale = ContentScale.Fit)
    }
}

@Composable
private fun BoxScope.HomeLeaves() {
    Image(
        painter = painterResource(Res.drawable.hoja_decorativa),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.align(Alignment.TopStart).offset(x = (-28).dp, y = (-18).dp).size(96.dp).rotate(35f).alpha(0.4f)
    )
    Image(
        painter = painterResource(Res.drawable.hoja_decorativa),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.align(Alignment.TopEnd).offset(x = 26.dp, y = (-28).dp).size(84.dp).rotate(155f).alpha(0.4f)
    )
}

@Composable
private fun HomeHeader(modifier: Modifier = Modifier, onFavoritos: () -> Unit = {}, onNotificaciones: () -> Unit = {}, onCanastas: () -> Unit = {}, onBuscar: () -> Unit = {}) {
    // Cabecera con ola verde de marca: saludo en blanco sobre el verde y la búsqueda montada
    // sobre la curva (mismo lenguaje visual que el onboarding).
    Box(modifier = modifier.fillMaxWidth()) {
        BrandWaveHeader(modifier = Modifier.align(Alignment.TopCenter), height = 180.dp)
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = TokenStore.user?.name?.substringBefore(' ')?.let { "¡Hola, $it! 👋" } ?: "¡Hola! 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "¿Qué productos frescos buscas hoy?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                HeaderIcon(Icons.Default.FavoriteBorder, onClick = onFavoritos)
                Spacer(Modifier.width(8.dp))
                HeaderIcon(Icons.Filled.ShoppingBasket, onClick = onCanastas)
                Spacer(Modifier.width(8.dp))
                HeaderIcon(Icons.Default.Notifications, onClick = onNotificaciones, badge = cl.frutapp.app.data.NotificacionesStore.noLeidas)
            }
            Spacer(Modifier.height(18.dp))
            SearchBarMock(onClick = onBuscar, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}, badge: Int = 0) {
    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(18.dp)
                    .background(FrutAppColors.AmberCoin, CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (badge > 9) "9+" else "$badge",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
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
private fun SearchBarMock(onClick: () -> Unit = {}, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FrutAppColors.Cream)
            .clickable(onClick = onClick)
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

private data class BannerSlide(
    val titulo1: String,
    val titulo2: String,
    val cta: String,
    val image: DrawableResource,
    val imageSize: Dp,
    val c1: Color,
    val c2: Color,
    val onClick: () -> Unit,
    val fullBg: Boolean = false
)

@Composable
private fun HeroCarousel(onOfertas: () -> Unit, onFrutCoins: () -> Unit, onCanastas: () -> Unit, onReyVegetal: () -> Unit, modifier: Modifier = Modifier) {
    val slides = remember(onOfertas, onFrutCoins, onCanastas, onReyVegetal) {
        listOf(
            BannerSlide("Frescura que se nota,", "calidad que te acompaña", "Ver ofertas", Res.drawable.banner_frescos, 180.dp, FrutAppColors.Brand800, FrutAppColors.Brand600, onOfertas, fullBg = true),
            BannerSlide("Hasta 40% de", "descuento esta semana", "Ver ofertas", Res.drawable.banner_fruit, 150.dp, FrutAppColors.Brand800, FrutAppColors.Brand600, onOfertas, fullBg = true),
            BannerSlide("Mi canasta del mes,", "1 toque para volver a pedir", "Mis canastas", Res.drawable.canasta_frutas, 150.dp, FrutAppColors.Brand800, FrutAppColors.Brand400, onCanastas, fullBg = true),
            BannerSlide("👑 Rey Vegetal,", "compra gratis al mejor reciclador", "Ver ranking", Res.drawable.canasta_frutas, 120.dp, FrutAppColors.Brand800, FrutAppColors.AmberCoin, onReyVegetal, fullBg = true),
            BannerSlide("Junta FrutCoins", "en cada compra", "Ver FrutCoins", Res.drawable.banner_frutcoins, 120.dp, FrutAppColors.Brand800, FrutAppColors.Brand400, onFrutCoins, fullBg = true)
        )
    }
    val realCount = slides.size
    // Carrusel "infinito": muchas páginas virtuales pero ACOTADAS (no Int.MAX_VALUE, que
    // desborda el cálculo de offset del pager y cuelga la app). Arranca en el medio y
    // mapea page % realCount = slide real.
    val loops = 1000
    val startPage = remember(realCount) { realCount * (loops / 2) }
    val pagerState = rememberPagerState(initialPage = startPage, pageCount = { realCount * loops })
    // Auto-avance infinito (siempre hacia adelante, sin salto al volver al primero)
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4500)
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            BannerSlideView(slides[page % realCount])
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val current = pagerState.currentPage % realCount
            repeat(realCount) { i ->
                val sel = current == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (sel) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (sel) FrutAppColors.Brand600 else FrutAppColors.Brand100)
                )
            }
        }
    }
}

@Composable
private fun BannerSlideView(slide: BannerSlide) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(165.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = slide.onClick)
    ) {
        if (slide.fullBg) {
            // Imagen de banner completa (sin transparencia) como fondo + velo para el texto
            Image(
                painter = painterResource(slide.image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier.matchParentSize().background(
                    Brush.horizontalGradient(listOf(slide.c1.copy(alpha = 0.92f), slide.c1.copy(alpha = 0.10f)))
                )
            )
        } else {
            Box(modifier = Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(slide.c1, slide.c2))))
            Image(
                painter = painterResource(slide.image),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = 14.dp).size(slide.imageSize)
            )
            Image(
                painter = painterResource(Res.drawable.hoja_decorativa),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = (-8).dp).size(58.dp).rotate(25f).alpha(0.9f)
            )
            Image(
                painter = painterResource(Res.drawable.hoja_decorativa),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.BottomStart).offset(x = (-12).dp, y = 10.dp).size(48.dp).rotate(205f).alpha(0.45f)
            )
        }
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp, end = 8.dp).fillMaxWidth(0.6f)
        ) {
            Text(slide.titulo1, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text(slide.titulo2, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(onClick = slide.onClick)
                    .padding(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Text(slide.cta, style = MaterialTheme.typography.labelLarge, color = FrutAppColors.Brand600, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier, onVerTodo: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = FrutAppColors.Brand800, fontWeight = FontWeight.Bold)
        if (onVerTodo != null) {
            Text(
                "Ver todo",
                style = MaterialTheme.typography.labelLarge,
                color = FrutAppColors.Brand600,
                modifier = Modifier.clickable(onClick = onVerTodo)
            )
        }
    }
}

private data class CategoriaUi(val categoria: Categoria, val imagen: DrawableResource)

@Composable
private fun CategoriesRow(modifier: Modifier = Modifier, onCategoria: (Categoria) -> Unit = {}) {
    val categorias = listOf(
        CategoriaUi(Categoria.FRUTAS, Res.drawable.manzana_roja),
        CategoriaUi(Categoria.VERDURAS, Res.drawable.zanahoria),
        CategoriaUi(Categoria.HIERBAS, Res.drawable.cilantro),
        CategoriaUi(Categoria.ORGANICOS, Res.drawable.lechuga)
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        categorias.forEach { cat ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onCategoria(cat.categoria) }
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(FrutAppColors.Brand50),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(cat.imagen),
                        contentDescription = cat.categoria.label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(46.dp).padding(4.dp)
                    )
                }
                Text(
                    text = cat.categoria.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = FrutAppColors.Ink,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
