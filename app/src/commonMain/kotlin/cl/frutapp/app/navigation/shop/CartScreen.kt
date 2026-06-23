@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.shop

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartItem
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.mascota_palta_racha
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Carrito (mockup 09): banner de envío gratis con progreso, lista de items con stepper,
 * resumen (subtotal/envío/total) y botón de pago. Estado desde [CartStore] (cliente).
 */
class CartScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var mostrarSelectorCanasta by remember { mutableStateOf(false) }
        val items = CartStore.items
        val subtotal = CartStore.subtotal
        val envio = CartStore.envio
        val total = CartStore.total

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CartHeader(
                    onBack = { navigator.pop() },
                    canClear = items.isNotEmpty(),
                    onClear = { CartStore.clear() }
                )

                if (items.isEmpty()) {
                    EmptyCart(modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                    ) {
                        item { FreeShippingBanner(subtotal = subtotal, modifier = Modifier.padding(20.dp)) }
                        items.forEach { item ->
                            item(key = "${item.producto.id}-${item.gramos}") {
                                CartRow(
                                    item = item,
                                    onEdit = { navigator.push(ProductDetailScreen(producto = item.producto, editing = item)) },
                                    onMinus = { CartStore.setCantidad(item, item.cantidad - 1) },
                                    onPlus = { CartStore.setCantidad(item, item.cantidad + 1) },
                                    onDelete = { CartStore.remove(item) }
                                )
                            }
                        }
                        item { Summary(subtotal = subtotal, envio = envio, total = total, modifier = Modifier.padding(20.dp)) }
                    }

                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        FrutButtonPrimary(
                            text = "Proceder al pago · ${formatClp(total)}",
                            onClick = { navigator.push(CheckoutScreen()) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(14.dp))
                                Text("Pago seguro", color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                            Row(
                                modifier = Modifier.clickable { mostrarSelectorCanasta = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.ShoppingBasket, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(14.dp))
                                Text("Guardar como canasta", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }

                FrutBottomNav(
                    selected = FrutTab.CARRITO,
                    onSelect = { tab -> if (tab != FrutTab.CARRITO) navigator.popUntilRoot() }
                )
            }

            if (mostrarSelectorCanasta) {
                val scopeCanasta = androidx.compose.runtime.rememberCoroutineScope()
                SelectorCanastaCart(
                    onDismiss = { mostrarSelectorCanasta = false },
                    onSeleccionar = { canastaId ->
                        mostrarSelectorCanasta = false
                        scopeCanasta.launch {
                            var exitos = 0
                            CartStore.items.forEach { item ->
                                val ok = cl.frutapp.app.data.CanastaStore.agregarProducto(canastaId, item.producto, item.cantidad, item.gramos)
                                if (ok) exitos++
                            }
                            cl.frutapp.app.ui.showToast(
                                if (exitos == CartStore.items.size) "Productos agregados a la canasta"
                                else "Agregados $exitos de ${CartStore.items.size}"
                            )
                        }
                    },
                    onNueva = {
                        mostrarSelectorCanasta = false
                        val items = CartStore.items.map { cl.frutapp.app.data.CanastaItem(it.producto, it.cantidad, it.gramos) }
                        navigator.push(cl.frutapp.app.navigation.canastas.NuevaCanastaScreen(itemsIniciales = items))
                    }
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCanastaCart(
    onDismiss: () -> Unit,
    onSeleccionar: (String) -> Unit,
    onNueva: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
            Text("Guardar carrito como canasta", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Agrega los productos a una canasta existente o crea una nueva.",
                color = FrutAppColors.InkMuted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            if (cl.frutapp.app.data.CanastaStore.items.isEmpty()) {
                Text(
                    "Aún no tienes canastas propias.",
                    color = FrutAppColors.InkSoft, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                cl.frutapp.app.data.CanastaStore.items.forEach { c ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(FrutAppColors.Brand50, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .clickable { onSeleccionar(c.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(38.dp).background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(c.emoji, fontSize = 20.sp)
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(c.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${c.cantidadProductos} producto(s)", color = FrutAppColors.InkSoft, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    .background(FrutAppColors.Brand400, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable(onClick = onNueva)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Text("Crear canasta nueva", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun CartHeader(onBack: () -> Unit, canClear: Boolean, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
        }
        Text("Carrito", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
        if (canClear) {
            Text("Vaciar carrito", color = FrutAppColors.Error, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.clickable(onClick = onClear))
        }
    }
}

@Composable
private fun FreeShippingBanner(subtotal: Int, modifier: Modifier = Modifier) {
    val falta = (CartStore.envioGratisDesde - subtotal).coerceAtLeast(0)
    val progreso = (subtotal.toFloat() / CartStore.envioGratisDesde).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = if (falta == 0) "¡Tienes envío gratis! 🎉" else "¡Falta poco! Te faltan ${formatClp(falta)} para envío gratis",
            color = FrutAppColors.Brand800,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(7.dp)
                .clip(CircleShape)
                .background(FrutAppColors.Brand100)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progreso)
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(FrutAppColors.Brand400)
            )
        }
    }
}

@Composable
private fun CartRow(item: CartItem, onEdit: () -> Unit, onMinus: () -> Unit, onPlus: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tocar el producto (imagen + datos) abre el detalle para editar peso/cantidad.
        Row(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(onClick = onEdit),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.producto.imagen),
                    contentDescription = item.producto.nombre,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(52.dp).padding(6.dp)
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(item.producto.nombre, color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(item.detalle, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                Text(formatClp(item.precioTotal), color = FrutAppColors.Brand600, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "Eliminar",
                tint = FrutAppColors.InkSoft,
                modifier = Modifier.size(20.dp).clickable(onClick = onDelete)
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                MiniStep(Icons.Filled.Remove, onMinus)
                Text("${item.cantidad}", color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
                MiniStep(Icons.Filled.Add, onPlus)
            }
        }
    }
}

@Composable
private fun MiniStep(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(30.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun Summary(subtotal: Int, envio: Int, total: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.Cream, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        SummaryRow("Subtotal", formatClp(subtotal))
        SummaryRow("Envío", if (envio == 0) "Gratis" else formatClp(envio))
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(formatClp(total), color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = FrutAppColors.InkMuted, fontSize = 14.sp)
        Text(value, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyCart(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(140.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(Res.drawable.mascota_palta_racha),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.size(118.dp)
            )
        }
        Text("Tu carrito está esperando", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text("Agrégame algo fresco y te ayudo a llevarlo.", color = FrutAppColors.InkMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
