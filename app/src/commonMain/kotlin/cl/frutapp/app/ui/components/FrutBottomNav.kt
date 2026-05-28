package cl.frutapp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NavItem("Inicio", Icons.Filled.Home, Icons.Outlined.Home, selected == FrutTab.INICIO) { onSelect(FrutTab.INICIO) }
            NavItem("Explorar", Icons.Filled.GridView, Icons.Outlined.GridView, selected == FrutTab.EXPLORAR) { onSelect(FrutTab.EXPLORAR) }
            CartItem(selected == FrutTab.CARRITO) { onSelect(FrutTab.CARRITO) }
            NavItem("Pedidos", Icons.Filled.Receipt, Icons.Outlined.Receipt, selected == FrutTab.PEDIDOS) { onSelect(FrutTab.PEDIDOS) }
            NavItem("Perfil", Icons.Filled.Person, Icons.Outlined.Person, selected == FrutTab.PERFIL) { onSelect(FrutTab.PERFIL) }
        }
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
private fun CartItem(selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.size(width = 60.dp, height = 56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = "Carrito", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}
