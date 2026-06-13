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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
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
import cl.frutapp.app.data.ConfigStore
import cl.frutapp.app.data.remote.PricingChangedAppException
import cl.frutapp.app.data.remote.ProductosAgotadosAppException
import cl.frutapp.app.data.ClientInfo
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.data.isUuidLike
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutLoader
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.LocalBrand
import cl.frutapp.app.ui.theme.SofrucoBrand
import cl.frutapp.shared.dto.ClientContextDto
import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.OrderItemRequest
import cl.frutapp.shared.dto.PaymentInput
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
        var mensajePagando by remember { mutableStateOf("Confirmando tu pedido…") }
        // Demo Sofruco: el catalogo viene del scrape (no del backend), asi que
        // cortamos el checkout antes de pegarle al API y mostramos un modal
        // honesto. brandActualId se lee desde LocalBrand para que el switch
        // sea reactivo al toggle "Modo de tienda" en Perfil.
        val brandActualId = LocalBrand.current.id
        var demoModalAbierto by remember { mutableStateOf(false) }
        // Cuando el backend devuelve 409 pricing_changed (alguien edito el envio
        // mientras el cliente armaba el carrito), guardamos el delta para mostrar
        // un AlertDialog amigable con "Continuar" / "Cancelar".
        var cambioPrecio by remember { mutableStateOf<PricingChangedAppException?>(null) }
        // 409 products_unavailable: algun producto se agoto entre que armo el carrito
        // y aprieta pagar. Mostramos AlertDialog con los nombres y removemos del carrito
        // para que pueda re-confirmar con lo disponible.
        var agotadosError by remember { mutableStateOf<ProductosAgotadosAppException?>(null) }
        // El boton Pagar queda disabled mientras corre el refresh forzado del config
        // al entrar al checkout. Sin esto, si el cliente toca Pagar antes de que
        // termine el refresh, mandamos un snapshot viejo y el backend devuelve 409 —
        // justo el caso que el refresh intentaba evitar.
        var configRefreshing by remember { mutableStateOf(true) }

        // Saldo de FrutCoins (para ofrecer pagar con ellos). Fuente de verdad: backend.
        LaunchedEffect(Unit) {
            runCatching { OrderApi().frutCoins() }.onSuccess { RewardsStore.set(it.balance) }
        }

        // Refresh forzado del config (envio, FrutCoins) AL ENTRAR al checkout. El
        // resto de pantallas usan stale-while-revalidate con TTL 30min, pero aca
        // mostrar un total que despues el backend rechaza con 409 es UX rota. Si la
        // red falla seguimos con cache + el snapshot del create-order como red de seguridad.
        // Mientras corre, deshabilitamos el boton Pagar para evitar la carrera donde el
        // cliente envia un snapshot viejo y recibe el 409 que el refresh queria evitar.
        LaunchedEffect(Unit) {
            ConfigStore.refreshNow()
            configRefreshing = false
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

                    if (RewardsStore.balance > 0 && !esRetiro && envioLocal > 0) {
                        SectionTitle("FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                        FrutCoinsToggle(
                            saldo = RewardsStore.balance,
                            envio = envioLocal,
                            usar = usarCoins,
                            onToggle = { usarCoins = it },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    SectionTitle("Método de pago", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        metodos.forEachIndexed { i, m ->
                            PayOption(method = m, selected = metodoSel == i, onClick = { metodoSel = i })
                        }
                    }
                    Spacer(Modifier.height(100.dp))
                }

                Column(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    if (error != null) {
                        Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    FrutButtonPrimary(
                        text = when {
                            configRefreshing -> "Validando precio…"
                            loading -> "Procesando…"
                            else -> "Pagar ${formatClp(totalLocal)}"
                        },
                        enabled = !loading && !configRefreshing && !CartStore.isEmpty,
                        onClick = {
                            // White-label demo: en modo Sofruco el checkout real no
                            // existe (el catalogo es hardcoded del scrape, no esta
                            // en el backend). Abrimos un modal honesto y cortamos
                            // el flow antes de pegarle al backend.
                            if (brandActualId == SofrucoBrand.id) {
                                demoModalAbierto = true
                                return@FrutButtonPrimary
                            }
                            error = null
                            loading = true
                            mensajePagando = "Confirmando tu pedido…"
                            scope.launch {
                                coroutineScope {
                                    val apiJob = async {
                                        runCatching {
                                            // El backend re-precia y calcula todo; el front solo manda qué quiere.
                                            // Si algún ítem trae un slug del DemoCatalog (no UUID), lo resolvemos
                                            // al uuid real del backend por nombre — el backend valida UUID estricto.
                                            val items = CartStore.items.toList()
                                            val porNombre = if (items.any { !it.producto.id.isUuidLike() }) {
                                                CatalogApi().products().associateBy { it.name.trim().lowercase() }
                                            } else emptyMap()
                                            val orderItems = items.map { ci ->
                                                val id = if (ci.producto.id.isUuidLike()) ci.producto.id
                                                    else porNombre[ci.producto.nombre.trim().lowercase()]?.id
                                                        ?: error("Producto no disponible en el catálogo: ${ci.producto.nombre}")
                                                OrderItemRequest(productId = id, cantidad = ci.cantidad, gramos = ci.gramos)
                                            }
                                            val pagos = buildList {
                                                if (usarCoins && RewardsStore.balance > 0) {
                                                    add(PaymentInput(method = "FRUTCOINS", monto = RewardsStore.balance))
                                                }
                                                add(PaymentInput(method = metodos[metodoSel].code))
                                            }
                                            OrderApi().create(
                                                CreateOrderRequest(
                                                    items = orderItems,
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
                                                    ),
                                                    // Snapshot del config con el que armamos el total que el
                                                    // cliente ve en pantalla. El backend lo compara contra su
                                                    // cache y devuelve 409 si difiere — nunca cobramos algo
                                                    // distinto a lo mostrado.
                                                    configSnapshot = ConfigStore.snapshot()
                                                )
                                            )
                                        }
                                    }
                                    // Mensajes rotativos mientras la API trabaja (para que sienta "vivo").
                                    launch {
                                        listOf("Reservando frescos en feria…", "Coordinando con tu feriante…").forEach { m ->
                                            delay(900)
                                            if (!apiJob.isCompleted) mensajePagando = m
                                        }
                                    }
                                    delay(1500) // duración mínima de la animación
                                    apiJob.await().onSuccess { dto ->
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
                                    }.onFailure { e ->
                                        // Caso especial: cambio de precio durante el armado del carrito.
                                        // El backend nos devolvio los nuevos valores; refrescamos el
                                        // ConfigStore para que el total local se re-pinte y abrimos un
                                        // dialogo amigable explicando que paso, antes que un error tecnico.
                                        if (e is PricingChangedAppException) {
                                            // NO refrescar ConfigStore aca: si lo hicieramos,
                                            // el diálogo recompondria con `envioLocal` ya
                                            // actualizado al nuevo precio, y el "Antes: $X"
                                            // mostraria el nuevo precio (mentira al cliente).
                                            // El refresh corre al cerrar el dialogo.
                                            cambioPrecio = e
                                            loading = false
                                            return@onFailure
                                        }
                                        if (e is ProductosAgotadosAppException) {
                                            agotadosError = e
                                            loading = false
                                            return@onFailure
                                        }
                                        // Sesion expirada la maneja el LaunchedEffect global en App.kt
                                        // (replaceAll a Login + toast). Aqui solo nos preocupamos del
                                        // error visible al cliente: mensaje amigable in-screen sin
                                        // tocar TokenStore (cualquier doble clear/navigate generaba
                                        // race con el handler global).
                                        ErrorReporter.report(screen = "Checkout", action = "create_order", error = e)
                                        error = mensajeAmigable(e, "crear el pedido")
                                        loading = false
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Overlay "Pagando…": cubre el checkout durante el proceso para que se sienta
            // como una transición intencional (no un spinner pelado).
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FrutLoader(dotSize = 18.dp)
                        Spacer(Modifier.height(28.dp))
                        Text(
                            mensajePagando,
                            color = FrutAppColors.Brand800,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Procesando tu pedido de forma segura.",
                            color = FrutAppColors.InkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Dialogo amigable cuando el envio cambio mientras armaban el carrito.
        // Mostramos cuanto era antes vs cuanto es ahora para que la decision sea
        // explicita. "Revisar" cierra el dialogo (el total ya se re-pinto al refrescar
        // el ConfigStore en el catch); "Cancelar" deja el cliente decidir mas tarde.
        cambioPrecio?.let { delta ->
            val antesCosto = formatClp(envioLocal)
            val ahoraCosto = formatClp(delta.nuevoCostoEnvio)
            val mensajeDelta = if (CartStore.subtotal >= delta.nuevoEnvioGratisDesde) {
                "Tu carrito ahora califica para envío gratis (sobre ${formatClp(delta.nuevoEnvioGratisDesde)})."
            } else {
                "Antes: $antesCosto. Ahora: $ahoraCosto. Envío gratis a partir de ${formatClp(delta.nuevoEnvioGratisDesde)}."
            }
            AlertDialog(
                onDismissRequest = { cambioPrecio = null },
                title = {
                    Text(
                        "Actualizamos el envío",
                        color = FrutAppColors.Brand800,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            delta.mensaje,
                            color = FrutAppColors.InkSoft,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            mensajeDelta,
                            color = FrutAppColors.InkMuted,
                            fontSize = 13.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        cambioPrecio = null
                        // Recien ahora refrescamos: el dialogo ya cerro, el `envioLocal`
                        // se recomputa con los nuevos valores y el total visible
                        // en el checkout queda alineado con el backend para el proximo
                        // intento de Pagar.
                        scope.launch { ConfigStore.refreshNow() }
                    }) {
                        Text("Revisar mi pedido", color = FrutAppColors.Brand600, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Diálogo amigable cuando algún producto se agotó durante el armado.
        // Lista los nombres y los descarta del carrito al cerrar, asi el cliente
        // puede re-confirmar con lo que quede disponible sin más fricción.
        agotadosError?.let { err ->
            AlertDialog(
                onDismissRequest = { agotadosError = null },
                title = {
                    Text(
                        "Algunos productos se agotaron",
                        color = FrutAppColors.Brand800,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            err.mensaje,
                            color = FrutAppColors.InkSoft,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        err.agotados.forEach { nombre ->
                            Text(
                                "• $nombre",
                                color = FrutAppColors.Brand800,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Los quitamos del carrito. Podés seguir con el resto.",
                            color = FrutAppColors.InkMuted,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        // Borrar TODAS las lineas con un nombre listado por el backend.
                        // El producto esta agotado a nivel SKU (no por gramaje), asi
                        // que si el cliente tenia "Palta Hass 500g" y "Palta Hass 1kg",
                        // ambas se descartan. Antes borrabamos solo una y el cliente
                        // entraba en loop: reintentaba, el backend devolvia el mismo
                        // agotados, se borraba otra, etc., hasta que volvia al carrito
                        // a mano.
                        val nombresAgotados = err.agotados.toSet()
                        CartStore.items.removeAll { it.producto.nombre in nombresAgotados }
                        agotadosError = null
                    }) {
                        Text("Quitar y continuar", color = FrutAppColors.Brand600, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    // Segundo CTA explicito: vuelve al carrito si el cliente quiere
                    // revisar/reemplazar los agotados antes de pagar. Antes era
                    // descarte sin opcion de cancelar — trampa UX cuando muchos
                    // items se agotaban.
                    TextButton(onClick = {
                        agotadosError = null
                        navigator.pop()
                    }) {
                        Text("Volver al carrito", color = FrutAppColors.InkSoft)
                    }
                }
            )
        }

        if (demoModalAbierto) {
            AlertDialog(
                onDismissRequest = { demoModalAbierto = false },
                title = {
                    Text(
                        "Vista demo Sofruco",
                        color = FrutAppColors.Brand800,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Estás navegando una edición demo de la app con el catálogo de Sofruco. " +
                            "El checkout real estará disponible cuando integremos su catálogo al backend de pedidos. " +
                            "Por ahora podés explorar el flujo completo (productos, carrito, despacho) sin generar un pedido.",
                        color = FrutAppColors.InkMuted,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        demoModalAbierto = false
                        CartStore.clear()
                        navigator.popUntilRoot()
                    }) {
                        Text("Entendido", color = FrutAppColors.Brand600, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { demoModalAbierto = false }) {
                        Text("Seguir explorando", color = FrutAppColors.InkSoft)
                    }
                }
            )
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
private fun FrutCoinsToggle(saldo: Int, envio: Int, usar: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val cubierto = minOf(saldo, envio)
    Row(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.AmberSoft, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Cubre tu despacho con FrutCoins", color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            val msg = if (saldo >= envio)
                "Tienes $saldo · cubrimos los ${formatClp(envio)} del despacho"
            else
                "Tienes $saldo · cubrimos ${formatClp(cubierto)} del despacho (${formatClp(envio - cubierto)} restante)"
            Text(msg, color = FrutAppColors.AmberCoin, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
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

// isUuidLike() vive en cl.frutapp.app.data.UuidExt (extension compartida).
