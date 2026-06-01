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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
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
import cl.frutapp.app.data.DemoCatalog
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.canasta_frutas
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Detalle de un Pack de Ofertas: hero + listado de productos incluidos + cálculo de
 * ahorro + botón "Agregar todo al carrito" que mete cada producto en una sola pasada.
 */
class PackDetailScreen(private val pack: Pack) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Resuelve cada productosId del Pack contra el catálogo real (backend con fallback
        // a DemoCatalog). Si un id no existe lo descartamos en silencio — no rompe la UI.
        var catalogo by remember { mutableStateOf<List<Producto>>(DemoCatalog.productos) }
        LaunchedEffect(Unit) {
            runCatching { CatalogApi().products() }
                .onSuccess { dtos -> if (dtos.isNotEmpty()) catalogo = dtos.map { it.toProducto() } }
                .onFailure { e -> cl.frutapp.app.ui.ErrorReporter.report(screen = "PackDetail", action = "load_catalog", error = e) }
        }
        val productos = remember(catalogo) {
            pack.productosIds.mapNotNull { id -> catalogo.firstOrNull { it.id == id } }
        }
        val ahorro = pack.antes - pack.precio
        val porcDesc = if (pack.antes > 0) ((ahorro.toDouble() / pack.antes) * 100).toInt() else 0

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
                    Text(
                        "Pack",
                        color = FrutAppColors.Brand800,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 8.dp)) {
                    // Hero del pack
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                                .background(
                                    Brush.verticalGradient(listOf(FrutAppColors.Brand400, FrutAppColors.Brand600)),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(72.dp).background(Color.White, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(painter = painterResource(Res.drawable.canasta_frutas), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(58.dp).padding(4.dp))
                                    }
                                    Column(modifier = Modifier.padding(start = 14.dp)) {
                                        Box(
                                            modifier = Modifier.background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("-$porcDesc%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(pack.nombre, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                        Text(pack.detalle, color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }
                                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 14.dp)) {
                                    Text(formatClp(pack.precio), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        formatClp(pack.antes),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textDecoration = TextDecoration.LineThrough,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text("Ahorras ${formatClp(ahorro)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                                }
                            }
                        }
                    }

                    // Beneficios
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
                            Text(
                                "Despacho incluido a Región Metropolitana",
                                color = FrutAppColors.Brand800,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                    }

                    item {
                        Text(
                            "Incluye ${productos.size} producto${if (productos.size != 1) "s" else ""}",
                            color = FrutAppColors.Brand800,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 4.dp)
                        )
                    }

                    items(productos.size) { idx ->
                        val p = productos[idx]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp).background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(painter = painterResource(p.imagen), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(44.dp).padding(4.dp))
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(p.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text(p.categoria.label, color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // CTA fijo abajo
                Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp)) {
                    FrutButtonPrimary(
                        text = "Agregar pack al carrito · ${formatClp(pack.precio)}",
                        enabled = productos.isNotEmpty(),
                        onClick = {
                            productos.forEach { p ->
                                val defGramos = if (p.unidad == "kg") 1000 else null
                                CartStore.add(p, 1, defGramos)
                            }
                            showToast("Pack agregado al carrito")
                            navigator.pop()
                        }
                    )
                }
            }
        }
    }
}
