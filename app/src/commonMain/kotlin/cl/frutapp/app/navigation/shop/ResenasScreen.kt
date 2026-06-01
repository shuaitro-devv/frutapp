package cl.frutapp.app.navigation.shop

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.ResenasStore
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Lista completa de reseñas de un producto con filtros por cantidad de estrellas.
 * Reutiliza [ReviewCard] y [StarRow] del ProductDetailScreen.
 */
class ResenasScreen(
    private val productoId: String,
    private val productoNombre: String
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var filtroEstrellas by remember { mutableStateOf<Int?>(null) }

        val todas = ResenasStore.resenas(productoId)
        val resenas = remember(todas, filtroEstrellas) {
            if (filtroEstrellas == null) todas else todas.filter { it.estrellas == filtroEstrellas }
        }
        val promedio = if (todas.isEmpty()) 0.0 else todas.sumOf { it.estrellas } / todas.size.toDouble()
        val totalConBase = 80 + (productoId.hashCode() and 0x7FFFFFFF) % 140 + ResenasStore.extras(productoId)

        // Distribución por estrellas p/ los filtros (cuenta solo lo real, no la base ficticia).
        val contadorPorEstrellas = remember(todas) {
            (1..5).associateWith { e -> todas.count { it.estrellas == e } }
        }

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
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("Reseñas", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(productoNombre, color = FrutAppColors.InkMuted, fontSize = 12.sp)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
                ) {
                    // Resumen: rating promedio + total
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                                .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = FrutAppColors.AmberCoin, modifier = Modifier.size(22.dp))
                                    Text(
                                        formatRating(promedio),
                                        color = FrutAppColors.Brand800,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }
                                Text(
                                    "$totalConBase reseñas en total",
                                    color = FrutAppColors.InkMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            StarRow(rating = promedio, starSize = 18.dp)
                        }
                    }

                    // Filtros por estrellas
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FiltroChip("Todas", filtroEstrellas == null) { filtroEstrellas = null }
                            (5 downTo 1).forEach { e ->
                                FiltroChip("$e ★ (${contadorPorEstrellas[e] ?: 0})", filtroEstrellas == e) {
                                    filtroEstrellas = if (filtroEstrellas == e) null else e
                                }
                            }
                        }
                    }

                    if (resenas.isEmpty()) {
                        item {
                            Text(
                                if (filtroEstrellas == null) "Aún no hay reseñas para este producto."
                                else "No hay reseñas con $filtroEstrellas ★.",
                                color = FrutAppColors.InkMuted,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        items(resenas.size) { idx ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                ReviewCard(r = resenas[idx])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FiltroChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand50, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) Color.White else FrutAppColors.Brand800,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatRating(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}.0" else "$rounded"
}
