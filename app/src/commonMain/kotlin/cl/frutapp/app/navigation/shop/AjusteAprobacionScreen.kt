package cl.frutapp.app.navigation.shop

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.SkeletonBox
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.AjusteResumenDto
import cl.frutapp.shared.dto.ItemAjusteDto
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * El cliente revisa items con cambio de peso vs lo pedido y decide aprobar
 * el total ajustado o rechazar los items que cambiaron mucho (se descartan,
 * el resto sigue al despacho). Solo aparece cuando el pedido esta en
 * ESPERANDO_AJUSTE_CLIENTE (el picker pesó items por kg y algun delta supero
 * la tolerancia configurada). Sin acción del cliente, el pedido se queda
 * acá hasta que decida.
 */
class AjusteAprobacionScreen(private val orderId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var resumen by remember { mutableStateOf<AjusteResumenDto?>(null) }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(orderId) {
            runCatching { OrderApi().getAjuste(orderId) }
                .onSuccess { resumen = it }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "AjusteAprobacion", action = "get_ajuste", error = e)
                    error = mensajeAmigable(e, "cargar el ajuste")
                }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(onBack = { navigator.pop() })
                val r = resumen
                when {
                    error != null -> Centered(error!!)
                    r == null -> Loading()
                    else -> Contenido(
                        resumen = r,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (r != null && error == null) BotonesInferior(
                    enabled = !loading,
                    onAprobar = {
                        if (loading) return@BotonesInferior
                        loading = true
                        scope.launch {
                            runCatching { OrderApi().aprobarAjuste(orderId) }
                                .onSuccess {
                                    showToast("¡Listo! Pedido confirmado.")
                                    navigator.pop()
                                }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "AjusteAprobacion", action = "aprobar", error = e)
                                    showToast(mensajeAmigable(e, "aprobar el ajuste"))
                                    // Refresca el resumen: si el estado cambio server-side
                                    // (otra sesion, cron), el GET vuelve a fallar y se ve
                                    // el error real; si no, el cliente puede reintentar
                                    // con datos frescos en lugar de hacer loop sobre el
                                    // mismo snapshot que el backend ya rechazo.
                                    runCatching { OrderApi().getAjuste(orderId) }
                                        .onSuccess { resumen = it; error = null }
                                        .onFailure { ee -> error = mensajeAmigable(ee, "cargar el ajuste") }
                                    loading = false
                                }
                        }
                    },
                    onRechazar = {
                        if (loading) return@BotonesInferior
                        loading = true
                        scope.launch {
                            runCatching { OrderApi().rechazarAjuste(orderId) }
                                .onSuccess {
                                    showToast("Items con cambio descartados. El resto sigue.")
                                    navigator.pop()
                                }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "AjusteAprobacion", action = "rechazar", error = e)
                                    showToast(mensajeAmigable(e, "rechazar los items"))
                                    runCatching { OrderApi().getAjuste(orderId) }
                                        .onSuccess { resumen = it; error = null }
                                        .onFailure { ee -> error = mensajeAmigable(ee, "cargar el ajuste") }
                                    loading = false
                                }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, "Atrás", tint = FrutAppColors.Brand800)
        }
        Spacer(Modifier.size(4.dp))
        Text(
            "Ajuste de peso",
            color = FrutAppColors.Brand800,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun Loading() {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        SkeletonBox(Modifier.fillMaxWidth(0.6f).height(20.dp))
        Spacer(Modifier.height(16.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(120.dp), RoundedCornerShape(20.dp))
        Spacer(Modifier.height(20.dp))
        repeat(2) {
            SkeletonBox(Modifier.fillMaxWidth().height(90.dp), RoundedCornerShape(16.dp))
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Centered(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(mensaje, color = FrutAppColors.InkSoft, fontSize = 14.sp)
    }
}

@Composable
private fun Contenido(resumen: AjusteResumenDto, modifier: Modifier = Modifier) {
    val delta = resumen.totalAjustado - resumen.totalEstimadoOriginal
    val signo = if (delta >= 0) "+" else ""
    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(
            "Pedido ${resumen.numero}",
            color = FrutAppColors.InkMuted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tu Seleccionador pesó algunos items y el peso real difiere de lo que pediste. " +
                "Revisa el nuevo total y decide.",
            color = FrutAppColors.Brand800,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(20.dp))

        // Resumen de totales: original tachado, ajustado destacado + delta.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total original", color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(
                    formatClp(resumen.totalEstimadoOriginal),
                    color = FrutAppColors.InkMuted,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.LineThrough
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total ajustado", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(
                    formatClp(resumen.totalAjustado),
                    color = FrutAppColors.Brand800,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "$signo${formatClp(delta)} respecto al pedido original",
                color = if (delta >= 0) FrutAppColors.Brand600 else FrutAppColors.InkSoft,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(24.dp))
        if (resumen.itemsAjustados.isNotEmpty()) {
            SectionTitle("Items con cambio significativo (${resumen.itemsAjustados.size})")
            Spacer(Modifier.height(8.dp))
            resumen.itemsAjustados.forEach { item ->
                ItemCard(item, destacar = true)
                Spacer(Modifier.height(10.dp))
            }
        }
        if (resumen.itemsDentroTolerancia.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SectionTitle("Cambios menores (${resumen.itemsDentroTolerancia.size})")
            Spacer(Modifier.height(8.dp))
            resumen.itemsDentroTolerancia.forEach { item ->
                ItemCard(item, destacar = false)
                Spacer(Modifier.height(10.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ItemCard(item: ItemAjusteDto, destacar: Boolean) {
    val deltaPct = (item.deltaPorc * 100).toInt()
    val signo = if (deltaPct >= 0) "+" else ""
    val borderColor = if (destacar) FrutAppColors.Brand600 else FrutAppColors.Brand50
    val deltaColor = when {
        abs(deltaPct) >= 15 -> FrutAppColors.Brand600
        else -> FrutAppColors.InkMuted
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(FrutAppColors.Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Scale, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.nombre, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Pediste ${formatGramos(item.gramosPedidos)} · llegó ${formatGramos(item.gramosReales)}",
                    color = FrutAppColors.InkMuted,
                    fontSize = 12.sp
                )
            }
            Text(
                "$signo$deltaPct%",
                color = deltaColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatClp(item.montoEstimado),
                color = FrutAppColors.InkMuted,
                fontSize = 13.sp,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatClp(item.montoFinal),
                color = FrutAppColors.Brand800,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatGramos(g: Int): String =
    if (g >= 1000) "${g / 1000.0} kg" else "$g g"

@Composable
private fun BotonesInferior(
    enabled: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    // Safe area abajo: en celus con 3-button nav la system bar tapa el boton si no
    // se aplica navigationBarsPadding (regla aprendida con CTAs al fondo).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FrutButtonPrimary(
            text = if (enabled) "Aprobar el ajuste" else "Procesando…",
            enabled = enabled,
            onClick = onAprobar
        )
        FrutButtonOutline(
            text = "Rechazar items con cambio",
            enabled = enabled,
            onClick = onRechazar
        )
    }
}
