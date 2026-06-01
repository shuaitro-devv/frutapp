@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package cl.frutapp.app.navigation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import cl.frutapp.app.data.NotifPrefsStore
import cl.frutapp.app.ui.theme.FrutAppColors

/** Preferencias de notificaciones. Persistido local en [NotifPrefsStore]. */
class NotificacionesPrefsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "Notificaciones",
                        color = FrutAppColors.Brand800,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Text(
                    "Elige qué te avisamos. Puedes cambiarlo cuando quieras.",
                    color = FrutAppColors.InkMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 18.dp)
                )

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    ToggleRow(
                        icon = Icons.Filled.Receipt,
                        titulo = "Pedidos",
                        detalle = "Confirmaciones, estado del despacho y entrega.",
                        valor = NotifPrefsStore.pedidos,
                        onChange = { NotifPrefsStore.pedidos = it }
                    )
                    ToggleRow(
                        icon = Icons.Filled.LocalOffer,
                        titulo = "Ofertas y promociones",
                        detalle = "Descuentos, packs nuevos y combos de temporada.",
                        valor = NotifPrefsStore.ofertas,
                        onChange = { NotifPrefsStore.ofertas = it }
                    )
                    ToggleRow(
                        icon = Icons.Filled.MonetizationOn,
                        titulo = "FrutCoins",
                        detalle = "Cuando ganas, vencen o tienes una recompensa nueva.",
                        valor = NotifPrefsStore.frutcoins,
                        onChange = { NotifPrefsStore.frutcoins = it }
                    )
                    ToggleRow(
                        icon = Icons.Filled.Recycling,
                        titulo = "Reciclaje",
                        detalle = "Recordatorios para devolver envases y agradecimientos.",
                        valor = NotifPrefsStore.reciclaje,
                        onChange = { NotifPrefsStore.reciclaje = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, titulo: String, detalle: String, valor: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .clickable { onChange(!valor) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(FrutAppColors.Brand400, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(titulo, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(detalle, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Switch(
            checked = valor,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = FrutAppColors.Brand600,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = FrutAppColors.InkSoft
            )
        )
    }
}
