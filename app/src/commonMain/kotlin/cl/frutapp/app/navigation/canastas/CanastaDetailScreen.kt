@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.canastas

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.Canasta
import cl.frutapp.app.data.CanastaItem
import cl.frutapp.app.data.CanastaStore
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.navigation.shop.CartScreen
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Detalle de una canasta: editable (si no es template), comprar = cargar al carrito, share,
 * toggle de recordatorio mensual y eliminar (si es propia).
 */
class CanastaDetailScreen(private val canastaId: Int) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val canasta = CanastaStore.get(canastaId)

        if (canasta == null) {
            // Caso defensivo: la canasta no existe (raro, salvo si la eliminaste).
            Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                Text("Canasta no encontrada.", color = FrutAppColors.InkMuted, fontSize = 14.sp)
            }
            return
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    canasta = canasta,
                    onBack = { navigator.pop() },
                    onShare = { navigator.push(CompartirCanastaScreen(canasta.id)) }
                )

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    HeaderCard(canasta)

                    Spacer(Modifier.height(18.dp))
                    Text("${canasta.cantidadProductos} producto${if (canasta.cantidadProductos != 1) "s" else ""}", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    canasta.items.forEach { item ->
                        ItemRow(
                            item = item,
                            esTemplate = canasta.esTemplate,
                            onIncrement = {
                                val nuevos = canasta.items.map {
                                    if (it === item) it.copy(cantidad = it.cantidad + 1) else it
                                }
                                CanastaStore.actualizar(canasta.id, items = nuevos)
                            },
                            onDecrement = {
                                val nuevos = canasta.items.mapNotNull {
                                    when {
                                        it !== item -> it
                                        it.cantidad > 1 -> it.copy(cantidad = it.cantidad - 1)
                                        else -> null
                                    }
                                }
                                CanastaStore.actualizar(canasta.id, items = nuevos)
                            }
                        )
                    }

                    if (!canasta.esTemplate) {
                        Spacer(Modifier.height(16.dp))
                        RecordatorioToggle(canasta)
                        Spacer(Modifier.height(12.dp))
                        EliminarRow {
                            CanastaStore.eliminar(canasta.id)
                            showToast("Canasta eliminada")
                            navigator.pop()
                        }
                    }
                    Spacer(Modifier.height(140.dp))
                }

                // Bottom bar: total + Comprar
                Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total estimado", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("~${formatClp(canasta.totalEstimado)}", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    FrutButtonPrimary(
                        text = if (canasta.esTemplate) "Comprar y guardar canasta" else "Comprar canasta",
                        onClick = {
                            // Si es template, copiarla a "Mis canastas" primero para que quede
                            // editable y no se pierda al modificarla en el carrito.
                            if (canasta.esTemplate) CanastaStore.copiarTemplate(canasta)
                            CartStore.clear()
                            canasta.items.forEach { CartStore.add(it.producto, it.cantidad, it.gramos) }
                            showToast("Canasta cargada en el carrito")
                            navigator.replace(CartScreen())
                        }
                    )
                }
            }
        }
    }
}

private fun formatItemQty(it: CanastaItem): String {
    val g = it.gramos
    return if (g != null) {
        val peso = if (g >= 1000) "${g / 1000} kg" else "$g g"
        "$peso × ${it.cantidad}"
    } else "${it.cantidad} ${it.producto.unidad}"
}

@Composable
private fun TopBar(canasta: Canasta, onBack: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
        }
        Text(canasta.nombre, color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 1)
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onShare),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Share, contentDescription = "Compartir", tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HeaderCard(canasta: Canasta) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(64.dp).background(Color.White, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(canasta.emoji, fontSize = 32.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(canasta.nombre, color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "${canasta.cantidadProductos} productos · ~${formatClp(canasta.totalEstimado)}",
                color = FrutAppColors.InkSoft, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (canasta.esTemplate) {
                Box(modifier = Modifier.padding(top = 6.dp).background(FrutAppColors.Brand400, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Canasta FrutApp · sugerida", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ItemRow(item: CanastaItem, esTemplate: Boolean, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(FrutAppColors.Brand50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painter = painterResource(item.producto.imagen), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(36.dp).padding(4.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(item.producto.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(formatItemQty(item), color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
        Text(formatClp(item.precioTotal), color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
        if (!esTemplate) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepBtn(Icons.Filled.Remove, onDecrement)
                Text("${item.cantidad}", color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                StepBtn(Icons.Filled.Add, onIncrement)
            }
        }
    }
}

@Composable
private fun StepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(28.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun RecordatorioToggle(canasta: Canasta) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(FrutAppColors.Cream, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔔", fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text("Pídenos que te avisemos cada mes", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Te recordamos para que la pidas con un clic", color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
        }
        Switch(
            checked = canasta.recordatorioMensual,
            onCheckedChange = { CanastaStore.actualizar(canasta.id, recordatorioMensual = it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FrutAppColors.Brand400,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = FrutAppColors.InkSoft
            )
        )
    }
}

@Composable
private fun EliminarRow(onEliminar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEliminar).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = FrutAppColors.Error, modifier = Modifier.size(18.dp))
        Text("Eliminar canasta", color = FrutAppColors.Error, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
    }
}
