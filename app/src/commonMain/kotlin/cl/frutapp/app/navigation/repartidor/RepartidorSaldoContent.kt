package cl.frutapp.app.navigation.repartidor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.AvatarApi
import cl.frutapp.app.platform.rememberSelectorImagenes
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.AvatarImage
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * repartidor-06 — Saldo y ganancias. Hero verde con saldo disponible, stats de en-transito
 * y proximo pago, resumen semanal, lista de ultimas transacciones y card legal de garantia.
 *
 * No es un Screen propio: se renderiza dentro de RepartidorHomeScreen cuando el tab activo
 * es 'ganancias'. (Aun no agregamos ese tab; ver siguiente edit en RepartidorHomeScreen.)
 */
@Composable
fun RepartidorSaldoContent(modifier: Modifier = Modifier, onAyuda: () -> Unit = {}) {
    val resumen = remember { saldoResumenMock() }
    val nombreRepartidor = remember { TokenStore.user?.name?.substringBefore(' ') ?: "Repartidor" }
    val emailRepartidor = remember { TokenStore.user?.email.orEmpty() }
    // Subida de foto: mismo patron unificado del cliente + picker. AvatarApi reusa
    // el endpoint /v1/me/avatar (multipart), funciona para cualquier rol logueado.
    var subiendoFoto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val avatarApi = remember { AvatarApi() }
    val selectorImg = rememberSelectorImagenes { bytes ->
        subiendoFoto = true
        scope.launch {
            runCatching { avatarApi.upload(bytes) }
                .onSuccess { newUrl ->
                    TokenStore.user?.let { u -> TokenStore.updateUser(u.copy(avatarUrl = newUrl)) }
                    showToast("Foto actualizada ✓")
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    ErrorReporter.report(screen = "RepartidorSaldo", action = "upload_avatar", error = e)
                    showToast(mensajeAmigable(e, "subir la foto"))
                }
            subiendoFoto = false
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mi saldo", color = FrutAppColors.Brand800, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Repartidor", color = FrutAppColors.InkMuted, fontSize = 13.sp)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onAyuda)
                    .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        // Card mini de identidad del repartidor con avatar editable. Va antes del hero
        // de saldo para que el rol tenga foto en su propia pantalla (paralelo al cliente
        // y al picker). El hero verde de saldo sigue siendo el foco principal.
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AvatarImage(
                    url = TokenStore.user?.avatarUrl,
                    initial = nombreRepartidor.take(1).uppercase(),
                    size = 48.dp
                )
                Box(
                    modifier = Modifier.size(20.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, FrutAppColors.Brand600, CircleShape)
                        .clickable(enabled = !subiendoFoto) { selectorImg.galeria() },
                    contentAlignment = Alignment.Center
                ) {
                    if (subiendoFoto) {
                        CircularProgressIndicator(color = FrutAppColors.Brand600, modifier = Modifier.size(10.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "Cambiar foto", tint = FrutAppColors.Brand600, modifier = Modifier.size(10.dp))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(nombreRepartidor, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                if (emailRepartidor.isNotBlank()) {
                    Text(emailRepartidor, color = FrutAppColors.InkMuted, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Hero saldo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Saldo disponible", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(formatoClp(resumen.disponible), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Última actualización: ${resumen.actualizacion}", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MiniBox(label = "En tránsito", valor = formatoClp(resumen.enTransito), sub = "${resumen.transferenciasEnTransito} transferencias", modifier = Modifier.weight(1f))
                MiniBox(label = "Próximo pago", valor = resumen.proximoPagoFecha, sub = "Estimado: ${formatoClp(resumen.proximoPagoEstimado)}", modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Resumen esta semana", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatBox(icon = Icons.Filled.Inventory, valor = "${resumen.entregasSemana}", label = "Entregas", modifier = Modifier.weight(1f))
            StatBox(icon = Icons.Filled.TrendingUp, valor = formatoClp(resumen.gananciasBrutas), label = "Bruto", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatBox(icon = Icons.Filled.MoneyOff, valor = formatoClp(resumen.deducciones), label = "Comisiones", modifier = Modifier.weight(1f))
            StatBox(icon = Icons.Filled.LocalAtm, valor = formatoClp(resumen.totalNeto), label = "Neto", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Últimas transacciones", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("Ver todas", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            resumen.transacciones.forEach { TransaccionRow(it) }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand100, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Shield, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Tus ganancias están protegidas", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Trabajamos para que recibas tus pagos de forma segura y puntual.", color = FrutAppColors.InkSoft, fontSize = 11.sp)
            }
            Text("Más info", color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MiniBox(label: String, valor: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)).padding(10.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        Text(valor, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun StatBox(icon: ImageVector, valor: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(valor, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(label, color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TransaccionRow(t: TransaccionSaldo) {
    val (icon, bg) = when (t.tipo) {
        TipoTransaccion.ENTREGA -> Icons.Filled.Storefront to FrutAppColors.Brand50
        TipoTransaccion.COMISION -> Icons.Filled.Schedule to Color(0xFFFEF3C7)
        TipoTransaccion.TRANSFERENCIA -> Icons.Filled.AccountBalance to FrutAppColors.Brand50
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(bg, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(t.descripcion, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(t.cuando, color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = (if (t.monto >= 0) "+" else "") + formatoClp(t.monto),
                color = if (t.monto >= 0) FrutAppColors.Brand600 else Color(0xFFB91C1C),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(t.estado, color = FrutAppColors.InkMuted, fontSize = 10.sp)
        }
    }
}
