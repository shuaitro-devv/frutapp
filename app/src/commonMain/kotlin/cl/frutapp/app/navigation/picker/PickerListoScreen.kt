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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
class PickerListoScreen(
    private val pedidoId: String,
    private val estados: Map<Int, EstadoItem> = emptyMap()
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var opcionesAbierto by remember { mutableStateOf(false) }
        // Desglose del pedido: completos / sustituidos / reducidos / faltantes. Si llegamos
        // sin estados (caso: vista de detalle del tab 'Listos' del historial), mostramos un
        // desglose default tipico para no quedar con todo en cero.
        val completos = if (estados.isEmpty()) 12 else estados.values.count { it == EstadoItem.COMPLETADO }
        val sustituidos = estados.values.count { it == EstadoItem.SUSTITUIDO }
        val reducidos = estados.values.count { it == EstadoItem.REDUCIDO }
        val faltantes = estados.values.count { it == EstadoItem.FALTANTE }
        val incidencias = sustituidos + reducidos + faltantes
        val total = if (estados.isEmpty()) 12 else estados.size
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
                IconButton(onClick = { opcionesAbierto = true }) {
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
                    text = if (incidencias == 0) "Todos los productos fueron preparados correctamente."
                           else "El pedido salió con $incidencias resoluciones registradas.",
                    color = FrutAppColors.InkMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(icon = Icons.Filled.Inventory2, valor = "$total", label = "Total")
                    StatItem(icon = Icons.Filled.CheckCircle, valor = "$completos de $total", label = "Completos")
                    StatItem(icon = Icons.Filled.AccessTime, valor = "18 min", label = "Duración")
                }
                // Desglose por tipo de resolucion solo aparece si hubo alguna distinta a COMPLETADO.
                if (incidencias > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (sustituidos > 0) ResolucionBox(label = "Sustituidos", valor = sustituidos, color = androidx.compose.ui.graphics.Color(0xFF3B82F6), modifier = Modifier.weight(1f))
                        if (reducidos > 0) ResolucionBox(label = "Reducidos", valor = reducidos, color = androidx.compose.ui.graphics.Color(0xFFD97706), modifier = Modifier.weight(1f))
                        if (faltantes > 0) ResolucionBox(label = "Faltantes", valor = faltantes, color = androidx.compose.ui.graphics.Color(0xFFB91C1C), modifier = Modifier.weight(1f))
                    }
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
                        Text(
                            text = if (incidencias == 0) "0 incidencias" else "$incidencias ${if (incidencias == 1) "incidencia" else "incidencias"}",
                            color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
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
                FrutButtonOutline(
                    text = "Ver detalle",
                    onClick = { navigator.push(PickerDetalleHandoffScreen(pedidoId, estados)) }
                )
            }
        }
        if (opcionesAbierto) {
            PickerOpcionesSheet(
                onCerrar = { opcionesAbierto = false },
                onElegir = { opcion ->
                    when (opcion) {
                        PickerOpcion.PAUSAR -> {
                            showToast("Pedido pausado - vuelto a la cola")
                            navigator.popUntilRoot()
                        }
                        PickerOpcion.REPORTAR -> {
                            navigator.push(PickerIncidenciaScreen(pedidoId))
                        }
                        PickerOpcion.CANCELAR -> {
                            showToast("Cancelado (mock)")
                            navigator.popUntilRoot()
                        }
                        else -> showToast("${opcion.titulo} - Próximamente")
                    }
                }
            )
        }
    }
}

@Composable
private fun ResolucionBox(label: String, valor: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$valor", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
