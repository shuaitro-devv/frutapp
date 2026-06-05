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
import androidx.compose.ui.draw.shadow
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
import cl.frutapp.app.ui.theme.FrutAppColors

/** Item del bottom nav de perfiles staff. */
data class StaffTab(
    val id: String,
    val label: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
)

/** Acción rápida que aparece como pill flotante al hacer long-press en el botón central. */
data class StaffQuickAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/**
 * Botón central destacado del bottom nav (espejo conceptual del CartButton del cliente). Se
 * renderiza solo si se pasa al [StaffBottomNav]. Tap = [onClick]; long-press abre un menu
 * de [quickActions] arriba del botón.
 */
data class StaffCenterButton(
    val icon: ImageVector,
    val contentDescription: String,
    val selected: Boolean,
    val onClick: () -> Unit,
    val quickActions: List<StaffQuickAction> = emptyList()
)

/**
 * Bottom navigation reutilizable para perfiles staff (picker, repartidor, admin embebido).
 * Mismo lenguaje visual que el [FrutBottomNav] del cliente: misma altura, sombra, tipografia,
 * paleta de items y boton central elevado opcional con long-press → quick actions.
 *
 * Si se pasa [center], los tabs se reparten 2 + hueco + 2 (igual al cliente, que tiene 4 tabs
 * + carrito al medio = 5 slots). Sin [center], los tabs ocupan toda la fila parejos.
 */
@Composable
fun StaffBottomNav(
    tabs: List<StaffTab>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    center: StaffCenterButton? = null
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (center != null) {
                    // Misma logica visual que el cliente: mitad de los tabs a cada lado y
                    // un Spacer del ancho del boton central (76dp) en el medio. Si la lista
                    // tiene impar, el extra queda a la izquierda.
                    val izquierda = tabs.take((tabs.size + 1) / 2)
                    val derecha = tabs.drop((tabs.size + 1) / 2)
                    izquierda.forEach {
                        StaffNavItem(it, it.id == selectedId) { onSelect(it.id) }
                    }
                    Spacer(Modifier.size(width = 76.dp, height = 56.dp))
                    derecha.forEach {
                        StaffNavItem(it, it.id == selectedId) { onSelect(it.id) }
                    }
                } else {
                    tabs.forEach {
                        StaffNavItem(it, it.id == selectedId) { onSelect(it.id) }
                    }
                }
            }
        }
        // Boton central flotante (cuando se provee).
        if (center != null) {
            StaffCenterFab(
                config = center,
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-26).dp)
            )
        }
    }
}

@Composable
private fun StaffNavItem(tab: StaffTab, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) FrutAppColors.Brand400 else FrutAppColors.InkSoft
    Column(
        modifier = Modifier.size(width = 60.dp, height = 56.dp).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) tab.iconSelected else tab.iconUnselected,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = tab.label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Botón central destacado: círculo verde 72dp con sombra y borde blanco. Tap normal Y
 * long-press abren el menu de quickActions cuando existen — antes el tap normal llamaba
 * [config.onClick] que en picker/repartidor era solo un toast 'Próximamente', mientras
 * el long-press abria un menú real. Resultado: usuario tapeaba y veia placeholder, no
 * descubria que la accion real estaba a un long-press.
 *
 * Ahora: si hay quickActions → tap=menu, long-press=menu (toggle si ya abierto).
 *         si NO hay quickActions → tap = onClick (uso degradado, fallback).
 */
@Composable
private fun StaffCenterFab(config: StaffCenterButton, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    val gapPx = with(LocalDensity.current) { 14.dp.roundToPx() }
    val tieneQuickActions = config.quickActions.isNotEmpty()

    Box(modifier = modifier.size(72.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(10.dp, CircleShape)
                .border(4.dp, Color.White, CircleShape)
                .background(
                    color = if (config.selected || menuOpen) FrutAppColors.Brand600 else FrutAppColors.Brand400,
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            when {
                                menuOpen -> menuOpen = false
                                tieneQuickActions -> menuOpen = true
                                else -> config.onClick()
                            }
                        },
                        onLongPress = { if (tieneQuickActions) menuOpen = true }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = config.contentDescription,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        if (menuOpen && config.quickActions.isNotEmpty()) {
            Popup(
                popupPositionProvider = StaffAbovePopup(gapPx),
                onDismissRequest = { menuOpen = false },
                properties = PopupProperties(focusable = true)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    config.quickActions.forEach { action ->
                        QuickActionPill(action.icon, action.label) {
                            menuOpen = false
                            action.onClick()
                        }
                    }
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
        Text(
            text = label,
            color = FrutAppColors.Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

/** Posiciona el popup centrado y por encima del ancla (boton central). */
private class StaffAbovePopup(private val gapPx: Int) : PopupPositionProvider {
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
