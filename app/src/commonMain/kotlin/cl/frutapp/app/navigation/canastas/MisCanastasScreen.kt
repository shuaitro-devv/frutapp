package cl.frutapp.app.navigation.canastas

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
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
import cl.frutapp.app.data.DemoCatalog
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.pedidoToCanastaItems
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.OrderSummaryDto
import kotlinx.coroutines.launch

/** Mis canastas: lista de canastas creadas por el usuario + sugeridas FrutApp + crear nueva. */
class MisCanastasScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Hidrata el cache desde el backend al entrar. Antes carga el catalogo
        // para que el resolver (productoId UUID → Producto UI) este listo
        // cuando llegue la lista de canastas con sus items.
        LaunchedEffect(Unit) {
            val productos = runCatching { CatalogApi().products() }.getOrNull()
                ?.map { it.toProducto() }
                ?: DemoCatalog.productos
            val porBackendId: Map<String, Producto> = productos.mapNotNull { p ->
                p.backendId?.let { it to p }
            }.toMap()
            CanastaStore.catalogoResolver = { uuid -> porBackendId[uuid] }
            CanastaStore.cargar()
        }

        val scope = rememberCoroutineScope()
        var mostrarSelectorPedido by remember { mutableStateOf(false) }
        var pedidosSelectorState by remember { mutableStateOf<List<OrderSummaryDto>?>(null) }
        var cargandoDesdePedido by remember { mutableStateOf<String?>(null) }
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(onBack = { navigator.pop() })

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (CanastaStore.items.isEmpty()) {
                        EmptyState(
                            onDesdePedido = {
                                mostrarSelectorPedido = true
                                if (pedidosSelectorState == null) {
                                    scope.launch {
                                        pedidosSelectorState = runCatching { OrderApi().list() }.getOrNull().orEmpty()
                                    }
                                }
                            },
                            onDesdeCarrito = {
                                val items = CartStore.items.map { CanastaItem(it.producto, it.cantidad, it.gramos) }
                                if (items.isEmpty()) showToast("Tu carrito esta vacio.")
                                else navigator.push(NuevaCanastaScreen(itemsIniciales = items))
                            },
                            onSugerida = {
                                CanastaStore.templates.firstOrNull()?.let { tpl ->
                                    navigator.push(CanastaDetailScreen(tpl.id))
                                }
                            },
                        )
                    } else {
                        SectionTitle("Mis canastas", Modifier.padding(top = 8.dp, bottom = 8.dp))
                        CanastaStore.items.forEach { c ->
                            CanastaRow(c, onClick = { navigator.push(CanastaDetailScreen(c.id)) })
                        }
                    }

                    SectionTitle("Canastas FrutApp", Modifier.padding(top = 24.dp, bottom = 8.dp))
                    Text(
                        "Listas pre-armadas para que partas rápido. Tócalas para ver y comprarlas.",
                        color = FrutAppColors.InkMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CanastaStore.templates.forEach { t ->
                        CanastaRow(t, onClick = { navigator.push(CanastaDetailScreen(t.id)) })
                    }

                    Spacer(Modifier.height(80.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    FrutButtonPrimary(
                        text = "Nueva canasta",
                        leadingIcon = Icons.Filled.Add,
                        onClick = { navigator.push(NuevaCanastaScreen()) }
                    )
                }
            }

            if (mostrarSelectorPedido) {
                SelectorPedidoSheet(
                    pedidos = pedidosSelectorState,
                    cargandoId = cargandoDesdePedido,
                    onCerrar = { mostrarSelectorPedido = false },
                    onElegir = { pedido ->
                        cargandoDesdePedido = pedido.id
                        scope.launch {
                            val dto = runCatching { OrderApi().get(pedido.id) }.getOrNull()
                            val itemsCanasta = if (dto != null) pedidoToCanastaItems(dto.items) else emptyList()
                            cargandoDesdePedido = null
                            mostrarSelectorPedido = false
                            if (itemsCanasta.isEmpty()) showToast("No pudimos cargar el pedido.")
                            else navigator.push(NuevaCanastaScreen(itemsIniciales = itemsCanasta))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
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
        Text("Mis canastas", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun CanastaRow(c: Canasta, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(Color.White, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(c.emoji, fontSize = 22.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.nombre, color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (c.esTemplate) {
                    Box(modifier = Modifier.padding(start = 6.dp).background(FrutAppColors.Brand400, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text("FrutApp", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (c.recordatorioMensual) {
                    Text(" · 🔔", fontSize = 11.sp)
                }
            }
            Text(
                "${c.cantidadProductos} producto${if (c.cantidadProductos != 1) "s" else ""} · ~${formatClp(c.totalEstimado)}",
                color = FrutAppColors.InkSoft, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(22.dp))
    }
}

/** Sheet con los pedidos del cliente para elegir uno y copiar sus productos
 *  a una canasta nueva. Solo muestra los ultimos 10 (suficiente en la
 *  practica; el cliente recurrente recuerda los recientes). */
@androidx.compose.runtime.Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun SelectorPedidoSheet(
    pedidos: List<OrderSummaryDto>?,
    cargandoId: String?,
    onCerrar: () -> Unit,
    onElegir: (OrderSummaryDto) -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onCerrar,
        containerColor = Color.White,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Text("Elegí un pedido", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(
                "Vamos a copiar sus productos a tu canasta nueva. Después podés ajustarla.",
                color = FrutAppColors.InkMuted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            when {
                pedidos == null -> Column(modifier = Modifier.fillMaxWidth()) {
                    repeat(3) {
                        cl.frutapp.app.ui.components.SkeletonBox(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp).height(56.dp),
                            RoundedCornerShape(12.dp),
                        )
                    }
                }
                pedidos.isEmpty() -> Text(
                    "Todavia no tenes pedidos. Hace tu primera compra y volve aca.",
                    color = FrutAppColors.InkMuted, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                else -> pedidos.take(10).forEach { p ->
                    val cargando = cargandoId == p.id
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(FrutAppColors.Brand50, RoundedCornerShape(12.dp))
                            .clickable(enabled = !cargando) { onElegir(p) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.numero, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("${p.itemsCount} producto(s) · ${formatClp(p.total)}", color = FrutAppColors.InkSoft, fontSize = 11.sp)
                        }
                        if (cargando) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = FrutAppColors.Brand400,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Empty state con 3 atajos visuales para crear la primera canasta sin
 *  perderse explorando el catalogo: a) copiar un pedido pasado, b) usar lo
 *  que hay en el carrito ahora, c) elegir una sugerida. La opcion "desde
 *  cero" la cubre el boton "Nueva canasta" del bottom de la pantalla. */
@Composable
private fun EmptyState(
    onDesdePedido: () -> Unit,
    onDesdeCarrito: () -> Unit,
    onSugerida: () -> Unit,
) {
    val tieneCarrito = cl.frutapp.app.data.CartStore.items.isNotEmpty()
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🧺", fontSize = 36.sp)
        }
        Text(
            "Aún no tienes canastas",
            color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            "Elige cómo empezar:",
            color = FrutAppColors.InkMuted, fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        OpcionEmpezar(
            emoji = "📋",
            titulo = "Desde un pedido tuyo",
            subtitulo = "Copiamos los productos de un pedido pasado",
            onClick = onDesdePedido,
        )
        if (tieneCarrito) {
            Spacer(Modifier.height(10.dp))
            OpcionEmpezar(
                emoji = "🛒",
                titulo = "Desde tu carrito actual",
                subtitulo = "Tienes ${cl.frutapp.app.data.CartStore.items.size} producto(s) listos",
                onClick = onDesdeCarrito,
            )
        }
        Spacer(Modifier.height(10.dp))
        OpcionEmpezar(
            emoji = "✨",
            titulo = "Probá una sugerida",
            subtitulo = "Canastas pre-armadas por FrutApp",
            onClick = onSugerida,
        )
    }
}

@Composable
private fun OpcionEmpezar(emoji: String, titulo: String, subtitulo: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(Color.White, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 22.sp) }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(titulo, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitulo, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(22.dp))
    }
}
