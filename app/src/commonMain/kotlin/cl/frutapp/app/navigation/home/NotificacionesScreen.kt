package cl.frutapp.app.navigation.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.Notificacion
import cl.frutapp.app.data.NotificacionesStore
import cl.frutapp.app.ui.theme.FrutAppColors

/** Inbox de notificaciones del usuario. Al abrirse, marca todo como leído. */
class NotificacionesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Al abrir el inbox, las notificaciones quedan como leídas (badge se vacía).
        LaunchedEffect(Unit) { NotificacionesStore.marcarTodasLeidas() }

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
                    Text("Notificaciones", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (NotificacionesStore.items.isEmpty()) {
                        EmptyNotis()
                    } else {
                        NotificacionesStore.items.forEach { NotiRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotiRow(n: Notificacion) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
            .background(if (n.leida) FrutAppColors.Brand50 else FrutAppColors.AmberSoft.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(n.tipo.emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.titulo, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                if (!n.leida) {
                    Box(modifier = Modifier.padding(start = 8.dp).size(8.dp).background(FrutAppColors.AmberCoin, CircleShape))
                }
            }
            Text(n.detalle, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Text(n.cuando, color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun EmptyNotis() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔕", fontSize = 44.sp)
        Text("Sin notificaciones nuevas", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text("Te avisaremos cuando algo importante pase con tus pedidos, coins o huella.", color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
