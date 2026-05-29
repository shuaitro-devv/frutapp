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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
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
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.camion_reparto
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private enum class PasoEstado { COMPLETADO, ACTIVO, PENDIENTE }
private data class Paso(val titulo: String, val detalle: String, val estado: PasoEstado)

/**
 * Seguimiento de pedido (mockup 12): estado del pedido con ilustración del reparto y
 * timeline vertical. Estado simulado ("En camino") — aún sin backend de órdenes.
 */
class OrderTrackingScreen(
    private val numero: String,
    private val total: Int,
    private val direccion: String,
    private val entrega: String
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val pasos = listOf(
            Paso("Pedido confirmado", "Recibimos tu pedido", PasoEstado.COMPLETADO),
            Paso("Preparando tu pedido", "Seleccionando productos frescos", PasoEstado.COMPLETADO),
            Paso("En camino", "Tu pedido va hacia tu dirección", PasoEstado.ACTIVO),
            Paso("Entregado", "Disfruta tus productos", PasoEstado.PENDIENTE)
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TrackTopBar(onBack = { navigator.pop() })

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(numero, color = FrutAppColors.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier.background(FrutAppColors.Brand400, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text("En camino", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(170.dp).background(FrutAppColors.Brand50, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.camion_reparto),
                            contentDescription = "Reparto en camino",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(140.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Entrega estimada", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                            Text(entrega, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Estado del pedido", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                    pasos.forEachIndexed { i, paso ->
                        TimelineStep(paso, isLast = i == pasos.lastIndex)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Place, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("Dirección de entrega", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                            Text(direccion, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp).background(FrutAppColors.Cream, RoundedCornerShape(14.dp)).padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total pagado", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(formatClp(total), color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(20.dp))
                }

                FrutBottomNav(
                    selected = FrutTab.PEDIDOS,
                    onSelect = { tab -> if (tab != FrutTab.PEDIDOS) navigator.popUntilRoot() }
                )
            }
        }
    }
}

@Composable
private fun TrackTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
        }
        Text("Seguimiento de pedido", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { })
    }
}

@Composable
private fun TimelineStep(paso: Paso, isLast: Boolean) {
    val verde = paso.estado == PasoEstado.COMPLETADO || paso.estado == PasoEstado.ACTIVO
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(if (verde) FrutAppColors.Brand400 else FrutAppColors.Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (paso.estado == PasoEstado.COMPLETADO) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else if (paso.estado == PasoEstado.ACTIVO) {
                    Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
                }
            }
            if (!isLast) {
                Box(modifier = Modifier.width(2.dp).height(34.dp).background(if (paso.estado == PasoEstado.COMPLETADO) FrutAppColors.Brand400 else FrutAppColors.Brand100))
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp, bottom = if (isLast) 0.dp else 8.dp)) {
            Text(
                paso.titulo,
                color = if (paso.estado == PasoEstado.PENDIENTE) FrutAppColors.InkSoft else FrutAppColors.Ink,
                fontSize = 15.sp,
                fontWeight = if (paso.estado == PasoEstado.ACTIVO) FontWeight.Bold else FontWeight.SemiBold
            )
            Text(paso.detalle, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
        }
    }
}
