package cl.frutapp.app.navigation.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.FrutCoinsEntryDto

private enum class HistorialFiltro(val label: String) {
    TODOS("Todos"),
    GANADOS("Ganados"),
    CANJEADOS("Canjeados")
}

/**
 * Historial completo de movimientos de FrutCoins. Resumen (balance + total ganado +
 * total canjeado) + filtro chip + lista con MovimientoRow (reutilizado del
 * FrutCoinsScreen). Carga del backend con la misma llamada que la pantalla padre.
 */
class HistorialCoinsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var movimientos by remember { mutableStateOf<List<FrutCoinsEntryDto>>(emptyList()) }
        var cargando by remember { mutableStateOf(true) }
        var filtro by remember { mutableStateOf(HistorialFiltro.TODOS) }

        LaunchedEffect(Unit) {
            runCatching { OrderApi().frutCoins() }
                .onSuccess {
                    movimientos = it.movimientos
                    RewardsStore.set(it.balance)
                }
                .onFailure { e -> cl.frutapp.app.ui.ErrorReporter.report(screen = "HistorialCoins", action = "load_history", error = e) }
            cargando = false
        }

        val ganadoTotal = movimientos.filter { it.delta > 0 }.sumOf { it.delta }
        val canjeadoTotal = movimientos.filter { it.delta < 0 }.sumOf { -it.delta }
        val visibles = remember(movimientos, filtro) {
            when (filtro) {
                HistorialFiltro.TODOS -> movimientos
                HistorialFiltro.GANADOS -> movimientos.filter { it.delta > 0 }
                HistorialFiltro.CANJEADOS -> movimientos.filter { it.delta < 0 }
            }
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
                    Text(
                        "Historial de FrutCoins",
                        color = FrutAppColors.Brand800,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)
                ) {
                    item {
                        ResumenCard(
                            balance = RewardsStore.balance,
                            ganadoTotal = ganadoTotal,
                            canjeadoTotal = canjeadoTotal
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HistorialFiltro.values().forEach { f ->
                                FiltroChipHistorial(f.label, filtro == f) { filtro = f }
                            }
                        }
                    }

                    when {
                        cargando -> item {
                            Text(
                                "Cargando movimientos…",
                                color = FrutAppColors.InkMuted,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                        visibles.isEmpty() -> item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when (filtro) {
                                        HistorialFiltro.GANADOS -> "Aún no ganaste FrutCoins."
                                        HistorialFiltro.CANJEADOS -> "Aún no canjeaste FrutCoins."
                                        HistorialFiltro.TODOS -> "Aún no tienes movimientos."
                                    },
                                    color = FrutAppColors.InkMuted,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        else -> items(visibles.size) { idx ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                MovimientoRow(visibles[idx])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenCard(balance: Int, ganadoTotal: Int, canjeadoTotal: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
            .background(
                Brush.verticalGradient(listOf(FrutAppColors.AmberCoin, FrutAppColors.AmberCoin.copy(alpha = 0.75f))),
                RoundedCornerShape(18.dp)
            )
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Balance actual", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Text("$balance FrutCoins", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).padding(top = 12.dp).background(Color.White.copy(alpha = 0.25f)))
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ResumenCelda("Total ganado", "+$ganadoTotal")
            ResumenCelda("Total canjeado", "-$canjeadoTotal")
        }
    }
}

@Composable
private fun ResumenCelda(label: String, valor: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        Text(valor, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun FiltroChipHistorial(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand50, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (selected) Color.White else FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
