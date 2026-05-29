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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import cl.frutapp.app.data.ClientInfo
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.ClientContextDto
import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.OrderItemRequest
import cl.frutapp.shared.dto.PaymentInput
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

private data class PayMethod(val label: String, val icon: ImageVector, val code: String)

private const val DIRECCION_DEMO = "Av. Siempre Viva 742, Santiago"
private const val ENTREGA_DEMO = "Hoy 10:00 - 12:00"
private const val SUCURSAL_DEMO = "Sucursal Lo Valledor"

/**
 * Checkout (mockup 10): dirección de entrega, resumen del pedido y método de pago.
 * Pago simulado (sin pasarela real): al "Pagar" genera un pedido y va a confirmación.
 */
class CheckoutScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val metodos = listOf(
            PayMethod("Tarjeta de crédito/débito", Icons.Filled.CreditCard, "TARJETA"),
            PayMethod("Mercado Pago", Icons.Filled.AccountBalanceWallet, "MERCADO_PAGO"),
            PayMethod("Webpay", Icons.Filled.CreditCard, "WEBPAY")
        )
        var metodoSel by remember { mutableStateOf(0) }
        var esRetiro by remember { mutableStateOf(false) }
        var usarCoins by remember { mutableStateOf(false) }
        var direccion by remember { mutableStateOf(DIRECCION_DEMO) }
        var editandoDir by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        // Saldo de FrutCoins (para ofrecer pagar con ellos). Fuente de verdad: backend.
        LaunchedEffect(Unit) {
            runCatching { OrderApi().frutCoins() }.onSuccess { RewardsStore.set(it.balance) }
        }

        // Estimación local SOLO para mostrar (retiro = sin envío). El backend reprecia.
        val envioLocal = if (esRetiro) 0 else CartStore.envio
        val totalLocal = CartStore.subtotal + envioLocal

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CheckoutTopBar(onBack = { navigator.pop() })
                Stepper()

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    SectionTitle("Modalidad de entrega", Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp))
                    ModalidadSelector(esRetiro = esRetiro, onSelect = { esRetiro = it }, Modifier.padding(horizontal = 20.dp))

                    SectionTitle(if (esRetiro) "Sucursal de retiro" else "Dirección de entrega", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    if (esRetiro) SucursalCard(Modifier.padding(horizontal = 20.dp))
                    else AddressCard(
                        direccion = direccion,
                        editando = editandoDir,
                        onToggleEdit = { editandoDir = !editandoDir },
                        onChange = { direccion = it },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    SectionTitle("Resumen del pedido", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    OrderSummary(envio = envioLocal, total = totalLocal, modifier = Modifier.padding(horizontal = 20.dp))

                    if (RewardsStore.balance > 0) {
                        SectionTitle("FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                        FrutCoinsToggle(saldo = RewardsStore.balance, usar = usarCoins, onToggle = { usarCoins = it }, Modifier.padding(horizontal = 20.dp))
                    }

                    SectionTitle("Método de pago", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        metodos.forEachIndexed { i, m ->
                            PayOption(method = m, selected = metodoSel == i, onClick = { metodoSel = i })
                        }
                    }
                    Spacer(Modifier.height(100.dp))
                }

                Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp)) {
                    if (error != null) {
                        Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    FrutButtonPrimary(
                        text = if (loading) "Procesando…" else "Pagar ${formatClp(totalLocal)}",
                        enabled = !loading && !CartStore.isEmpty,
                        onClick = {
                            error = null
                            loading = true
                            scope.launch {
                                runCatching {
                                    // El backend re-precia y calcula todo; el front solo manda qué quiere.
                                    val pagos = buildList {
                                        if (usarCoins && RewardsStore.balance > 0) {
                                            add(PaymentInput(method = "FRUTCOINS", monto = RewardsStore.balance))
                                        }
                                        add(PaymentInput(method = metodos[metodoSel].code))
                                    }
                                    OrderApi().create(
                                        CreateOrderRequest(
                                            items = CartStore.items.map {
                                                OrderItemRequest(productId = it.producto.id, cantidad = it.cantidad, gramos = it.gramos)
                                            },
                                            fulfillmentType = if (esRetiro) "RETIRO" else "DELIVERY",
                                            sucursal = if (esRetiro) SUCURSAL_DEMO else null,
                                            direccion = if (esRetiro) null else direccion.trim().ifBlank { null },
                                            payments = pagos,
                                            context = ClientContextDto(
                                                channel = ClientInfo.channel,
                                                appVersion = ClientInfo.appVersion,
                                                deviceModel = ClientInfo.deviceModel,
                                                osVersion = ClientInfo.osVersion,
                                                locale = ClientInfo.locale
                                            )
                                        )
                                    )
                                }.onSuccess { dto ->
                                    CartStore.clear()
                                    navigator.replace(
                                        OrderConfirmedScreen(
                                            orderId = dto.id,
                                            numero = dto.numero,
                                            total = dto.totalFinal ?: dto.totalEstimado,
                                            coins = dto.frutcoinsGanadas,
                                            direccion = dto.direccion,
                                            entrega = dto.entrega,
                                            fulfillmentType = dto.fulfillmentType,
                                            payments = dto.payments
                                        )
                                    )
                                }.onFailure {
                                    error = "No pudimos crear el pedido. Revisa tu conexión e inténtalo de nuevo."
                                }
                                loading = false
                            }
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
private fun AddressCard(
    direccion: String,
    editando: Boolean,
    onToggleEdit: () -> Unit,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                if (!editando) {
                    Text(direccion, color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Text(
                if (editando) "Listo" else "Cambiar",
                color = FrutAppColors.Brand600,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onToggleEdit)
            )
        }
        if (editando) {
            OutlinedTextField(
                value = direccion,
                onValueChange = onChange,
                singleLine = true,
                placeholder = { Text("Calle, número, comuna") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FrutAppColors.Brand400,
                    unfocusedBorderColor = FrutAppColors.Brand100,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun OrderSummary(envio: Int, total: Int, modifier: Modifier = Modifier) {
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
        SummaryLine("Envío", if (envio == 0) "Gratis" else formatClp(envio))
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(formatClp(total), color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ModalidadSelector(esRetiro: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ModalidadOption(
            icon = Icons.Filled.LocalShipping,
            label = "Despacho",
            selected = !esRetiro,
            onClick = { onSelect(false) },
            modifier = Modifier.weight(1f)
        )
        ModalidadOption(
            icon = Icons.Filled.Storefront,
            label = "Retiro",
            selected = esRetiro,
            onClick = { onSelect(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ModalidadOption(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(if (selected) FrutAppColors.Brand50 else Color.White, RoundedCornerShape(14.dp))
            .border(1.5.dp, if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) FrutAppColors.Brand600 else FrutAppColors.InkSoft, modifier = Modifier.size(24.dp))
        Text(label, color = if (selected) FrutAppColors.Brand800 else FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SucursalCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Storefront, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(SUCURSAL_DEMO, color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Retiras tu pedido sin costo de envío", color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun FrutCoinsToggle(saldo: Int, usar: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.AmberSoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Usar mis FrutCoins", color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("Tienes $saldo · se aplicará el máximo permitido", color = FrutAppColors.AmberCoin, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = usar,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FrutAppColors.Brand400)
        )
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
