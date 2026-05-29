@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartItem
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.DemoCatalog
import cl.frutapp.app.data.FavoritesStore
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.Resena
import cl.frutapp.app.data.ResenasStore
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.ui.comingSoon
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlin.math.abs
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Detalle de producto (mockup 08): imagen grande, precio, selector de gramaje + cantidad,
 * badge de frescura, detalles, beneficios, relacionados y barra fija "Agregar al carrito".
 */
class ProductDetailScreen(
    private val producto: Producto,
    /** Si viene, estamos EDITANDO esa línea del carrito (prefill + "Actualizar"). */
    private val editing: CartItem? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val esKg = producto.unidad == "kg"
        var gramos by remember { mutableStateOf(editing?.gramos ?: 1000) }
        var cantidad by remember { mutableStateOf(editing?.cantidad ?: 1) }
        val favorito = FavoritesStore.isFavorite(producto.id)

        val totalSel = if (esKg) (producto.precioClp * gramos / 1000.0).toInt() * cantidad
        else producto.precioClp * cantidad

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    TopBar(
                        favorito = favorito,
                        onBack = { navigator.pop() },
                        onFav = { FavoritesStore.toggle(producto.id) }
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .background(FrutAppColors.Brand50),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(producto.imagen),
                            contentDescription = producto.nombre,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().padding(24.dp).height(210.dp)
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                        Text(producto.nombre, color = FrutAppColors.Brand800, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(producto.categoria.label, color = FrutAppColors.InkSoft, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
                            Text(formatClp(producto.precioClp), color = FrutAppColors.Brand600, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("/${producto.unidad}", color = FrutAppColors.InkMuted, fontSize = 14.sp, modifier = Modifier.padding(start = 3.dp, bottom = 3.dp))
                        }
                        Text(
                            "Seleccionado en feria, fresco y de temporada. Llega del productor directo a tu mesa.",
                            color = FrutAppColors.InkMuted,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )

                        if (esKg) {
                            Text("Cantidad", color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(250, 500, 1000).forEach { g ->
                                    GramajeChip(
                                        label = if (g >= 1000) "1 kg" else "$g g",
                                        selected = gramos == g,
                                        onClick = { gramos = g }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (esKg) "Unidades" else "Cantidad", color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Stepper(value = cantidad, onMinus = { if (cantidad > 1) cantidad-- }, onPlus = { cantidad++ })
                        }
                    }
                }
                item { FreshBadge(modifier = Modifier.padding(horizontal = 20.dp)) }
                item {
                    SectionTitle("Detalles del producto", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp))
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        DetailRow(Icons.Filled.Place, "Origen", "Productores locales, Chile")
                        DetailRow(Icons.Filled.AcUnit, "Conservación", "Refrigerar entre 2° y 8°")
                        DetailRow(Icons.Filled.CalendarMonth, "Temporada", "Disponible todo el año")
                    }
                }
                item {
                    SectionTitle("Beneficios", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BenefitCard(Icons.Filled.Spa, "Fibra", Modifier.weight(1f))
                        BenefitCard(Icons.Filled.Bolt, "Vitaminas", Modifier.weight(1f))
                        BenefitCard(Icons.Filled.Favorite, "Antioxidantes", Modifier.weight(1f))
                    }
                }
                item { ReviewsSection(producto, onVerTodas = { comingSoon() }) }
                item {
                    SectionTitle("También te puede gustar", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp))
                }
                item {
                    val relacionados = DemoCatalog.productos.filter { it.id != producto.id }.take(8)
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(relacionados) { rel ->
                            RelatedCard(rel, onClick = { navigator.replace(ProductDetailScreen(rel)) })
                        }
                    }
                    Spacer(Modifier.height(110.dp))
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                FrutButtonPrimary(
                    text = (if (editing != null) "Actualizar carrito · " else "Agregar al carrito · ") + formatClp(totalSel),
                    onClick = {
                        if (editing != null) {
                            // Editar = reemplazar la línea (maneja cambio de gramaje) y volver al carrito.
                            CartStore.remove(editing)
                            CartStore.add(producto, cantidad, if (esKg) gramos else null)
                            navigator.pop()
                        } else {
                            CartStore.add(producto, cantidad, if (esKg) gramos else null)
                            navigator.push(CartScreen())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(favorito: Boolean, onBack: () -> Unit, onFav: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        CircleIcon(Icons.Filled.ArrowBack, "Volver", FrutAppColors.Ink, onBack)
        Text("Detalle del producto", color = FrutAppColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        CircleIcon(
            icon = if (favorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            desc = "Favorito",
            tint = if (favorito) FrutAppColors.Error else FrutAppColors.Ink,
            onClick = onFav
        )
    }
}

@Composable
private fun CircleIcon(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun GramajeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) FrutAppColors.Brand400 else Color.White, CircleShape)
            .border(1.dp, if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand100, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (selected) Color.White else FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun Stepper(value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn(Icons.Filled.Remove, onMinus)
        Text("$value", color = FrutAppColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 16.dp))
        StepBtn(Icons.Filled.Add, onPlus)
    }
}

@Composable
private fun StepBtn(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(34.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FreshBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Spa, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(22.dp))
        Text("Producto fresco y de temporada", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label, color = FrutAppColors.InkSoft, fontSize = 12.sp)
            Text(value, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BenefitCard(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(26.dp))
        Text(label, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun ReviewsSection(producto: Producto, onVerTodas: () -> Unit) {
    val resenas = ResenasStore.resenas(producto.id)
    // Promedio dummy estable por producto (derivado del id). El conteo headline suma las
    // reseñas que el usuario haya agregado en la sesión para que se sienta "vivo".
    val h = abs(producto.id.hashCode())
    val promedio = listOf(4.6, 4.7, 4.8, 4.9)[h % 4]
    val total = 80 + h % 140 + ResenasStore.extras(producto.id)

    var escribiendo by rememberSaveable(producto.id) { mutableStateOf(false) }
    var estrellas by rememberSaveable(producto.id) { mutableStateOf(5) }
    var texto by rememberSaveable(producto.id) { mutableStateOf("") }
    // null = estamos creando una reseña nueva; con id = estamos editando esa reseña.
    var editandoId by rememberSaveable(producto.id) { mutableStateOf<Int?>(null) }
    val autor = TokenStore.user?.name?.takeIf { it.isNotBlank() } ?: "Tú"

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle("Reseñas")
            Text("Ver todas", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onVerTodas))
        }
        Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(promedio.toString().replace('.', ','), color = FrutAppColors.Brand800, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                StarRow(promedio, starSize = 18.dp)
                Text("$total reseñas", color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }

        if (escribiendo) {
            EscribirResenaForm(
                modoEdicion = editandoId != null,
                estrellas = estrellas,
                onEstrellas = { estrellas = it },
                texto = texto,
                onTexto = { texto = it },
                onConfirmar = {
                    val id = editandoId
                    if (id != null) {
                        ResenasStore.editar(producto.id, id, estrellas, texto)
                        showToast("Reseña actualizada")
                    } else {
                        ResenasStore.agregar(producto.id, autor, estrellas, texto)
                        showToast("¡Gracias por tu reseña!")
                    }
                    texto = ""
                    estrellas = 5
                    editandoId = null
                    escribiendo = false
                },
                onCancelar = {
                    texto = ""
                    estrellas = 5
                    editandoId = null
                    escribiendo = false
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    .border(1.dp, FrutAppColors.Brand400, RoundedCornerShape(12.dp))
                    .clickable {
                        editandoId = null
                        estrellas = 5
                        texto = ""
                        escribiendo = true
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
                    Text("Escribir una reseña", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        resenas.forEach { r ->
            ReviewCard(
                r = r,
                onEditar = if (r.propia) {
                    {
                        editandoId = r.id
                        estrellas = r.estrellas
                        texto = r.texto
                        escribiendo = true
                    }
                } else null
            )
        }
    }
}

@Composable
private fun EscribirResenaForm(
    modoEdicion: Boolean,
    estrellas: Int,
    onEstrellas: (Int) -> Unit,
    texto: String,
    onTexto: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Text(if (modoEdicion) "Edita tu reseña" else "Tu calificación", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        StarSelector(rating = estrellas, onRating = onEstrellas)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(92.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (texto.isEmpty()) {
                Text("Cuéntanos cómo estuvo tu experiencia…", color = FrutAppColors.InkSoft, fontSize = 14.sp)
            }
            BasicTextField(
                value = texto,
                onValueChange = onTexto,
                textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 14.sp),
                cursorBrush = SolidColor(FrutAppColors.Brand400),
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(14.dp))
        FrutButtonPrimary(text = if (modoEdicion) "Guardar cambios" else "Publicar reseña", onClick = onConfirmar, enabled = texto.isNotBlank())
        Text(
            "Cancelar",
            color = FrutAppColors.InkSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onCancelar),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun StarSelector(rating: Int, onRating: (Int) -> Unit) {
    Row(modifier = Modifier.padding(top = 6.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = "$i estrella(s)",
                tint = FrutAppColors.AmberCoin,
                modifier = Modifier.size(36.dp).clickable { onRating(i) }.padding(end = 6.dp)
            )
        }
    }
}

@Composable
private fun StarRow(rating: Double, starSize: Dp) {
    Row {
        for (i in 1..5) {
            val icon = when {
                rating >= i -> Icons.Filled.Star
                rating >= i - 0.5 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(icon, contentDescription = null, tint = FrutAppColors.AmberCoin, modifier = Modifier.size(starSize))
        }
    }
}

@Composable
private fun ReviewCard(r: Resena, onEditar: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(FrutAppColors.Brand400, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(r.nombre.take(1), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    if (r.propia) {
                        Box(modifier = Modifier.padding(start = 6.dp).background(FrutAppColors.Brand400, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                            Text("Tú", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                StarRow(r.estrellas.toDouble(), starSize = 13.dp)
            }
            Text(r.fecha, color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
        if (r.texto.isNotBlank()) {
            Text(r.texto, color = FrutAppColors.Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        if (onEditar != null) {
            Text(
                "Editar",
                color = FrutAppColors.Brand600,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp).clickable(onClick = onEditar)
            )
        }
    }
}

@Composable
private fun RelatedCard(producto: Producto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(90.dp).background(FrutAppColors.Brand50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(producto.imagen),
                contentDescription = producto.nombre,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(8.dp).height(74.dp)
            )
        }
        Text(producto.nombre, color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
        Text("${formatClp(producto.precioClp)}/${producto.unidad}", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}
