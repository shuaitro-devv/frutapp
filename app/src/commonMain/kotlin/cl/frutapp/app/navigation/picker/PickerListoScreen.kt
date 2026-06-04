package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * picker-05 — pedido listo / handoff. Pantalla de confirmacion tras completar el picklist:
 * hero con check grande, stats finales del trabajo, destino+picker, chip de incidencias,
 * card de proximo paso (entregar al equipo de despacho) y botones de listo/ver detalle.
 */
class PickerListoScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.popUntilRoot() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Text(
                    text = pedidoId,
                    color = FrutAppColors.Brand800,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .background(FrutAppColors.Brand400, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Completado", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { showToast("Más opciones - Próximamente") }) {
                    Icon(Icons.Filled.MoreVert, "Más", tint = FrutAppColors.Brand800)
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(28.dp))
                // Hero check
                Box(
                    modifier = Modifier.size(120.dp).background(FrutAppColors.Brand50, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(82.dp).background(FrutAppColors.Brand400, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Pedido listo", color = FrutAppColors.Brand800, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Todos los productos fueron preparados correctamente.",
                    color = FrutAppColors.InkMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(20.dp))
                // Stats
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(icon = Icons.Filled.Inventory2, valor = "12", label = "Total")
                    StatItem(icon = Icons.Filled.CheckCircle, valor = "12 de 12", label = "Progreso")
                    StatItem(icon = Icons.Filled.AccessTime, valor = "18 min", label = "Duración")
                }
                Spacer(Modifier.height(12.dp))
                // Destino + picker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    InfoLinea(icon = Icons.Filled.LocationOn, label = "Destino", valor = "Sector Norte / Restaurante Verde")
                    Spacer(Modifier.height(10.dp))
                    InfoLinea(icon = Icons.Filled.Person, label = "Picker", valor = "Camila R.")
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .background(FrutAppColors.Brand50, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Check, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("0 incidencias", color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Proximo paso
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                        .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(FrutAppColors.Brand100, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.LocalShipping, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Próximo paso", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Entrega este pedido al equipo de despacho para continuar el proceso.",
                            color = FrutAppColors.InkSoft,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FrutButtonPrimary(text = "Listo para despacho", onClick = { navigator.popUntilRoot() })
                FrutButtonOutline(text = "Ver detalle", onClick = { navigator.pop() })
            }
        }
    }
}

@Composable
private fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, valor: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(valor, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = FrutAppColors.InkMuted, fontSize = 11.sp)
    }
}

@Composable
private fun InfoLinea(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = FrutAppColors.InkMuted, fontSize = 11.sp)
            Text(valor, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
