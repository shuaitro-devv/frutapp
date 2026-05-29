@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.shop

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlin.random.Random
import org.jetbrains.compose.resources.ExperimentalResourceApi

private data class PayMethod(val label: String, val icon: ImageVector)

private const val DIRECCION_DEMO = "Av. Siempre Viva 742, Santiago"
private const val ENTREGA_DEMO = "Hoy 10:00 - 12:00"

/**
 * Checkout (mockup 10): dirección de entrega, resumen del pedido y método de pago.
 * Pago simulado (sin pasarela real): al "Pagar" genera un pedido y va a confirmación.
 */
class CheckoutScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val total = CartStore.total
        val metodos = listOf(
            PayMethod("Tarjeta de crédito/débito", Icons.Filled.CreditCard),
            PayMethod("Mercado Pago", Icons.Filled.AccountBalanceWallet),
            PayMethod("Webpay", Icons.Filled.CreditCard)
        )
        var metodoSel by remember { mutableStateOf(0) }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CheckoutTopBar(onBack = { navigator.pop() })
                Stepper()

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    SectionTitle("Dirección de entrega", Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp))
                    AddressCard(Modifier.padding(horizontal = 20.dp))

                    SectionTitle("Resumen del pedido", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    OrderSummary(Modifier.padding(horizontal = 20.dp))

                    SectionTitle("Método de pago", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        metodos.forEachIndexed { i, m ->
                            PayOption(method = m, selected = metodoSel == i, onClick = { metodoSel = i })
                        }
                    }
                    Spacer(Modifier.height(100.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp)) {
                    FrutButtonPrimary(
                        text = "Pagar ${formatClp(total)}",
                        onClick = {
                            val numero = "#FRU-2026-${Random.nextInt(100000, 999999)}"
                            val coins = total / 100
                            RewardsStore.add(coins)
                            CartStore.clear()
                            navigator.replace(
                                OrderConfirmedScreen(
                                    numero = numero,
                                    total = total,
                                    coins = coins,
                                    direccion = DIRECCION_DEMO,
                                    entrega = ENTREGA_DEMO
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutTopBar(onBack: () -> Unit) {
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
        Text("Checkout", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun Stepper() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepDot(1, "Dirección", active = true)
        StepLine()
        StepDot(2, "Pago", active = true)
        StepLine()
        StepDot(3, "Confirmación", active = false)
    }
}

@Composable
private fun StepDot(n: Int, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(28.dp).background(if (active) FrutAppColors.Brand400 else FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$n", color = if (active) Color.White else FrutAppColors.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = if (active) FrutAppColors.Brand800 else FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StepLine() {
    Box(modifier = Modifier.weight(1f).height(2.dp).padding(horizontal = 6.dp).background(FrutAppColors.Brand100))
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun AddressCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Casa", color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier.padding(start = 8.dp).background(FrutAppColors.Brand200, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Predeterminada", color = FrutAppColors.Brand800, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(DIRECCION_DEMO, color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("Cambiar", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { })
    }
}

@Composable
private fun OrderSummary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.Cream, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        CartStore.items.forEach { item ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${item.producto.nombre}  ", color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(item.detalle, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(formatClp(item.precioTotal), color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        SummaryLine("Subtotal", formatClp(CartStore.subtotal))
        SummaryLine("Envío", if (CartStore.envio == 0) "Gratis" else formatClp(CartStore.envio))
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(formatClp(CartStore.total), color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = FrutAppColors.InkMuted, fontSize = 13.sp)
        Text(value, color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PayOption(method: PayMethod, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) FrutAppColors.Brand50 else Color.White, RoundedCornerShape(14.dp))
            .border(1.5.dp, if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(method.icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(24.dp))
        Text(method.label, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Box(
            modifier = Modifier.size(20.dp).background(Color.White, CircleShape).border(2.dp, if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Box(modifier = Modifier.size(10.dp).background(FrutAppColors.Brand400, CircleShape))
        }
    }
}
