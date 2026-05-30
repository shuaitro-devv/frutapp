@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.ResenasStore
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutLoader
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.OrderItemDto
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Califica tu compra (dummy): a partir de los ítems de un pedido entregado, mapea cada uno
 * al catálogo (por imageKey) y deja calificar producto por producto. Cada calificación se
 * guarda como reseña del producto, así aparece luego en su detalle.
 */
class CalificarPedidoScreen(private val items: List<OrderItemDto>) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var productos by remember { mutableStateOf<List<Producto>?>(null) }
        var error by remember { mutableStateOf(false) }
        val estrellas = remember { mutableStateMapOf<String, Int>() }
        val comentarios = remember { mutableStateMapOf<String, String>() }
        val autor = TokenStore.user?.name?.takeIf { it.isNotBlank() } ?: "Tú"

        LaunchedEffect(Unit) {
            val catalogo = runCatching { CatalogApi().products() }.getOrNull()
            if (catalogo == null) {
                error = true
                return@LaunchedEffect
            }
            val porImagen = catalogo.associateBy { it.imageKey }
            val mapeados = items.mapNotNull { porImagen[it.imageKey]?.toProducto() }.distinctBy { it.id }
            // Precarga: si ya calificaste un producto, mostramos tus estrellas y comentario.
            mapeados.forEach { p ->
                ResenasStore.miResena(p.id)?.let { mia ->
                    estrellas[p.id] = mia.estrellas
                    if (mia.texto.isNotBlank()) comentarios[p.id] = mia.texto
                }
            }
            productos = mapeados
        }

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
                    Text("Califica tu compra", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                val lista = productos
                when {
                    error -> Centrado("No pudimos cargar los productos.")
                    lista == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { FrutLoader() }
                    lista.isEmpty() -> Centrado("No hay productos para calificar.")
                    else -> {
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                            Text(
                                "¿Cómo estuvo tu pedido? Tu opinión ayuda a otros vecinos.",
                                color = FrutAppColors.InkMuted, fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            lista.forEach { p ->
                                ProductoCalificable(
                                    producto = p,
                                    estrellas = estrellas[p.id] ?: 0,
                                    onEstrellas = { estrellas[p.id] = it },
                                    comentario = comentarios[p.id] ?: "",
                                    onComentario = { comentarios[p.id] = it }
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        val hayCalificacion = lista.any { (estrellas[it.id] ?: 0) > 0 }
                        Box(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                            FrutButtonPrimary(
                                text = "Enviar calificación",
                                enabled = hayCalificacion,
                                onClick = {
                                    var n = 0
                                    lista.forEach { p ->
                                        val s = estrellas[p.id] ?: 0
                                        if (s > 0) {
                                            ResenasStore.guardar(p.id, autor, s, comentarios[p.id]?.trim() ?: "")
                                            n++
                                        }
                                    }
                                    showToast("¡Gracias por calificar! ($n)")
                                    navigator.pop()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoCalificable(
    producto: Producto,
    estrellas: Int,
    onEstrellas: (Int) -> Unit,
    comentario: String,
    onComentario: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(producto.imagen),
                    contentDescription = producto.nombre,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(38.dp).padding(4.dp)
                )
            }
            Text(producto.nombre, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp))
        }
        Row(modifier = Modifier.padding(top = 10.dp)) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= estrellas) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i estrella(s)",
                    tint = FrutAppColors.AmberCoin,
                    modifier = Modifier.size(32.dp).clickable { onEstrellas(i) }.padding(end = 6.dp)
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(64.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (comentario.isEmpty()) {
                Text("Comentario (opcional)", color = FrutAppColors.InkSoft, fontSize = 13.sp)
            }
            BasicTextField(
                value = comentario,
                onValueChange = onComentario,
                textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 13.sp),
                cursorBrush = SolidColor(FrutAppColors.Brand400),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Centrado(texto: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(texto, color = FrutAppColors.InkMuted, fontSize = 14.sp)
    }
}
