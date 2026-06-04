package cl.frutapp.app.ui.components

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

/**
 * Item del bottom nav de perfiles staff (picker/repartidor/admin). No usa el patron del
 * FrutBottomNav del cliente porque ese tiene el boton central de carrito (cliente-only).
 * Los staff bottoms son una grilla 4 tabs plana, mas sobria.
 */
data class StaffTab(
    val id: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

/**
 * Bottom navigation reutilizable para perfiles staff (picker, repartidor, admin embebido).
 * Recibe la lista de tabs como dato — cada perfil define las suyas. Sin items destacados
 * ni overlays: una sola fila plana, item activo en verde marca.
 */
@Composable
fun StaffBottomNav(
    tabs: List<StaffTab>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEach { tab ->
                    StaffNavItem(
                        label = tab.label,
                        iconSelected = tab.iconSelected,
                        iconUnselected = tab.iconUnselected,
                        selected = tab.id == selectedId,
                        onClick = { onSelect(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffNavItem(
    label: String,
    iconSelected: ImageVector,
    iconUnselected: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) FrutAppColors.Brand400 else FrutAppColors.InkSoft
    Column(
        modifier = Modifier.size(width = 72.dp, height = 56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) iconSelected else iconUnselected,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
