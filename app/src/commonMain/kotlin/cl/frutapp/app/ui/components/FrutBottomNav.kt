package cl.frutapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import cafe.adriel.voyager.navigator.LocalNavigator
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.navigation.offers.OfertasScreen
import cl.frutapp.app.navigation.rewards.FrutCoinsScreen
import cl.frutapp.app.navigation.shop.CheckoutScreen
import cl.frutapp.app.ui.theme.FrutAppColors

enum class FrutTab { INICIO, EXPLORAR, CARRITO, PEDIDOS, PERFIL }

/**
 * Bottom navigation FrutApp: 5 ítems, carrito al centro destacado (botón verde elevado),
 * ítem activo en verde.
 */
@Composable
fun FrutBottomNav(
    selected: FrutTab,
    onSelect: (FrutTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // navigationBarsPadding en el Box raíz: el CartButton flotante también respeta
    // la barra de navegación del sistema (gesture pill o 3 botones). Antes solo el
    // Row interno tenía el padding, así que el CartButton quedaba sobre la nav bar
    // en celulares con 3-button navigation.
    Box(modifier = modifier.fillMaxWidth().navigationBarsPadding()) {
        Surface(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NavItem("Inicio", Icons.Filled.Home, Icons.Outlined.Home, selected == FrutTab.INICIO) { onSelect(FrutTab.INICIO) }
                NavItem("Explorar", Icons.Filled.GridView, Icons.Outlined.GridView, selected == FrutTab.EXPLORAR) { onSelect(FrutTab.EXPLORAR) }
                Spacer(Modifier.size(width = 76.dp, height = 56.dp)) // hueco para el botón central
                NavItem("Pedidos", Icons.Filled.Receipt, Icons.Outlined.Receipt, selected == FrutTab.PEDIDOS) { onSelect(FrutTab.PEDIDOS) }
                NavItem("Perfil", Icons.Filled.Person, Icons.Outlined.Person, selected == FrutTab.PERFIL) { onSelect(FrutTab.PERFIL) }
            }
        }
        // Botón central destacado: más grande y sobresale por encima de la barra.
        CartButton(
            selected = selected == FrutTab.CARRITO,
            onClick = { onSelect(FrutTab.CARRITO) },
            modifier = Modifier.align(Alignment.TopCenter).offset(y = (-26).dp)
        )
    }
}

@Composable
private fun NavItem(
    label: String,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) FrutAppColors.Brand400 else FrutAppColors.InkSoft
    Column(
        modifier = Modifier.size(width = 60.dp, height = 56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(if (selected) iconSelected else iconUnselected, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CartButton(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val count = CartStore.cantidadTotal
    val navigator = LocalNavigator.current
    var menuOpen by remember { mutableStateOf(false) }
    val gapPx = with(LocalDensity.current) { 14.dp.roundToPx() }

    Box(modifier = modifier.size(72.dp), contentAlignment = Alignment.TopEnd) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(10.dp, CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .background(if (selected || menuOpen) FrutAppColors.Brand600 else FrutAppColors.Brand400, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { if (menuOpen) menuOpen = false else onClick() },
                        onLongPress = { menuOpen = true }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(FrutAppColors.Error, CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 9) "9+" else "$count",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Long-press: menú de accesos rápidos que sube y SE MANTIENE (cierra al tocar
        // afuera, al re-tocar el botón, o al elegir una opción). Base para más menús de demo.
        if (menuOpen) {
            Popup(
                popupPositionProvider = AbovePopup(gapPx),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickActionPill(Icons.Filled.LocalOffer, "Ofertas") { menuOpen = false; navigator?.push(OfertasScreen()) }
                    QuickActionPill(Icons.Filled.MonetizationOn, "FrutCoins") { menuOpen = false; navigator?.push(FrutCoinsScreen()) }
                    QuickActionPill(Icons.Filled.Payment, "Ir a pagar") { menuOpen = false; navigator?.push(CheckoutScreen()) }
                }
            }
        }
    }
}

@Composable
private fun QuickActionPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(24.dp))
            .background(Color.White, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        Text(label, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
    }
}

/** Posiciona el popup CENTRADO horizontalmente sobre el botón ancla y por ENCIMA de él. */
private class AbovePopup(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val y = anchorBounds.top - popupContentSize.height - gapPx
        return IntOffset(x, y)
    }
}
