package cl.frutapp.app.navigation.shop

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.navigation.rewards.FrutCoinsScreen
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Pedido confirmado (mockup 11): confirmación con número de pedido, entrega estimada,
 * dirección, total y FrutCoins ganadas. El pedido ya está persistido en el backend.
 */
class OrderConfirmedScreen(
    private val orderId: String,
    private val numero: String,
    private val total: Int,
    private val coins: Int,
    private val direccion: String,
    private val entrega: String
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(96.dp).background(FrutAppColors.Brand50, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(60.dp))
                    }
                    Text("¡Pedido confirmado!", color = FrutAppColors.Brand800, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                    Text(
                        "Tu pedido está en camino. Te avisaremos cuando salga a reparto.",
                        color = FrutAppColors.InkMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                    InfoCard {
                        InfoRow("Número de pedido", numero)
                        InfoRow("Realizado", "Hoy")
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoCard {
                        IconLine(Icons.Filled.LocalShipping, "Entrega estimada", entrega)
                        Spacer(Modifier.height(10.dp))
                        IconLine(Icons.Filled.Place, "Dirección", direccion)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Cream, RoundedCornerShape(16.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total pagado", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(formatClp(total), color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().background(FrutAppColors.AmberSoft, RoundedCornerShape(16.dp))
                            .clickable { navigator.push(FrutCoinsScreen()) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = FrutAppColors.AmberCoin, modifier = Modifier.size(30.dp))
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text("¡Ganaste $coins FrutCoins!", color = FrutAppColors.AmberCoin, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Úsalas para descuentos en tus próximas compras.", color = FrutAppColors.InkMuted, fontSize = 12.sp)
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FrutButtonPrimary(
                        text = "Ver mi pedido",
                        onClick = { navigator.replace(OrderTrackingScreen(orderId = orderId)) }
                    )
                    FrutButtonOutline(text = "Seguir comprando", onClick = { navigator.popUntilRoot() })
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(16.dp)
    ) { content() }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = FrutAppColors.InkMuted, fontSize = 13.sp)
        Text(value, color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconLine(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(androidx.compose.ui.graphics.Color.White, CircleShape),
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
