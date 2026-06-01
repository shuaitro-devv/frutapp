@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.offers

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import cl.frutapp.app.navigation.shop.ProductDetailScreen
import cl.frutapp.app.ui.comingSoon
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.canasta_frutas
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private data class Oferta(val producto: Producto, val descuento: Int) {
    val precioOferta: Int get() = (producto.precioClp * (100 - descuento) / 100)
}

data class Pack(
    val nombre: String,
    val detalle: String,
    val precio: Int,
    val antes: Int,
    val productosIds: List<String>
)

/**
 * Ofertas (mockup 17): banner hero, ofertas destacadas con descuento, countdown de
 * ofertas por tiempo limitado y packs. Descuentos sobre productos de [DemoCatalog].
 */
class OfertasScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val ofertas = remember {
            val byId = DemoCatalog.productos.associateBy { it.id }
            listOf(
                "palta-hass" to 20, "manzana-roja" to 30, "tomate" to 25,
                "naranja" to 35, "limon" to 20, "platano" to 15
            ).mapNotNull { (id, d) -> byId[id]?.let { Oferta(it, d) } }
        }
        val filtros = listOf("Todos", "Frutas", "Verduras")
        var filtroSel by remember { mutableStateOf(0) }
        val destacadas = ofertas.filter {
            when (filtroSel) {
                1 -> it.producto.categoria == Categoria.FRUTAS
                2 -> it.producto.categoria == Categoria.VERDURAS
                else -> true
            }
        }
        val packs = listOf(
            Pack(
                nombre = "Pack Saludable",
                detalle = "5 frutas + 3 verduras de temporada",
                precio = 8990,
                antes = 11990,
                productosIds = listOf("manzana-roja", "platano", "naranja", "kiwi", "frutilla", "lechuga", "zanahoria", "tomate")
            ),
            Pack(
                nombre = "Pack Familiar",
                detalle = "Canasta semanal para 4 personas",
                precio = 18990,
                antes = 24990,
                productosIds = listOf("palta-hass", "manzana-roja", "naranja", "platano", "sandia", "lechuga", "tomate", "papa", "cebolla", "zanahoria", "brocoli", "huevos")
            )
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ofertas", color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { navigator.push(cl.frutapp.app.navigation.profile.AyudaScreen()) })
                }

                LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                    item { HeroBanner(modifier = Modifier.padding(horizontal = 20.dp)) }
                    item {
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 14.dp)
                        ) {
                            items(filtros.size) { i ->
                                Chip(filtros[i], filtroSel == i) { filtroSel = i }
                            }
                        }
                    }
                    item { SectionTitle("Ofertas destacadas", Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 6.dp)) }
                    items(destacadas.chunked(2)) { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            fila.forEach { of ->
                                OfferCard(of, Modifier.weight(1f), onClick = { navigator.push(ProductDetailScreen(of.producto)) })
                            }
                            if (fila.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    item { LimitedOffer(modifier = Modifier.padding(20.dp)) }
                    item { SectionTitle("Packs con descuento", Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 6.dp)) }
                    items(packs.size) { i -> PackCard(packs[i], onClick = { navigator.push(PackDetailScreen(packs[i])) }) }
                }

                FrutBottomNav(selected = FrutTab.INICIO, onSelect = { navigator.popUntilRoot() })
            }
        }
    }
}

@Composable
private fun HeroBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(150.dp).background(
            Brush.horizontalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand400)), RoundedCornerShape(20.dp)
        )
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart).padding(20.dp).fillMaxWidth(0.6f)) {
            Box(modifier = Modifier.background(Color.White, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                Text("40% dcto", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("Ahora, come mejor", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Text("Aprovecha las mejores ofertas en frescos.", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Image(
            painter = painterResource(Res.drawable.canasta_frutas),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(120.dp)
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand50, CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Color.White else FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun OfferCard(oferta: Oferta, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = oferta.producto
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(18.dp)).clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(FrutAppColors.Brand50, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))) {
            Image(
                painter = painterResource(p.imagen),
                contentDescription = p.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(10.dp).height(100.dp)
            )
            Box(modifier = Modifier.align(Alignment.TopStart).padding(8.dp).background(FrutAppColors.Error, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("-${oferta.descuento}%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(p.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(formatClp(p.precioClp), color = FrutAppColors.InkSoft, fontSize = 11.sp, textDecoration = TextDecoration.LineThrough)
                Text(formatClp(oferta.precioOferta), color = FrutAppColors.Brand600, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            val defGramos = if (p.unidad == "kg") 1000 else null
            val pConDesc = p.copy(precioClp = oferta.precioOferta)
            val linea = CartStore.items.firstOrNull { it.producto.id == p.id && it.gramos == defGramos }
            val cantidad = linea?.cantidad ?: 0
            if (cantidad <= 0) {
                Box(
                    modifier = Modifier.size(34.dp).background(FrutAppColors.Brand400, CircleShape)
                        .clickable { CartStore.add(pConDesc, 1, defGramos) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            } else {
                Row(
                    modifier = Modifier.height(34.dp).background(FrutAppColors.Brand400, RoundedCornerShape(17.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clickable { linea?.let { CartStore.setCantidad(it, it.cantidad - 1) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Quitar uno", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Text("$cantidad", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                    Box(
                        modifier = Modifier.size(34.dp).clickable { CartStore.add(pConDesc, 1, defGramos) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar uno", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitedOffer(modifier: Modifier = Modifier) {
    var secs by remember { mutableStateOf(2 * 3600 + 45 * 60 + 30) }
    LaunchedEffect(Unit) { while (secs > 0) { delay(1000); secs -= 1 } }
    val hh = (secs / 3600).toString().padStart(2, '0')
    val mm = ((secs % 3600) / 60).toString().padStart(2, '0')
    val ss = (secs % 60).toString().padStart(2, '0')
    Row(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.AmberSoft, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Timer, contentDescription = null, tint = FrutAppColors.AmberCoin, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Ofertas por tiempo limitado", color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Termina pronto, aprovecha ya", color = FrutAppColors.InkMuted, fontSize = 12.sp)
        }
        Text("$hh:$mm:$ss", color = FrutAppColors.AmberCoin, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PackCard(pack: Pack, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(54.dp).background(Color.White, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(Res.drawable.canasta_frutas), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(42.dp).padding(4.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(pack.nombre, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(pack.detalle, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                Text(formatClp(pack.precio), color = FrutAppColors.Brand600, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(formatClp(pack.antes), color = FrutAppColors.InkSoft, fontSize = 11.sp, textDecoration = TextDecoration.LineThrough, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp))
            }
        }
    }
}
