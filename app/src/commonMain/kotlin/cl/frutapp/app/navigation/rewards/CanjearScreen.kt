package cl.frutapp.app.navigation.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Confirmación de canje de recompensa por FrutCoins. Dos estados:
 *  - PreConfirmacion: muestra el costo + balance actual + botón "Confirmar canje"
 *  - Confirmado: muestra el código de cupón devuelto por el backend + balance
 *    actualizado + "Volver"
 *
 * El canje real va por RewardsStore.canjearRemoto → POST /v1/frutcoins/redeem.
 * Idempotencia: generamos un UUID v4 al entrar al composable y lo reusamos
 * en cada reintento (red mala etc).
 */
class CanjearScreen(
    private val recompensaTitulo: String,
    private val recompensaCosto: Int
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clipboard = LocalClipboardManager.current
        val scope = rememberCoroutineScope()
        var confirmado by remember { mutableStateOf(false) }
        var canjeando by remember { mutableStateOf(false) }
        var codigoCupon by remember { mutableStateOf("") }
        // Key estable para idempotencia: si el usuario toca "Confirmar" y el
        // POST se reintenta (red mala, app reabierta), el backend nos devuelve
        // el mismo cupon en vez de duplicar el debito.
        val idempotencyKey = remember(recompensaTitulo, recompensaCosto) { uuidV4Random() }
        val saldoTrasCanje = (RewardsStore.balance - recompensaCosto).coerceAtLeast(0)

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
                        if (confirmado) "¡Listo!" else "Canjear",
                        color = FrutAppColors.Brand800,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                if (!confirmado) {
                    PreConfirmacion(
                        titulo = recompensaTitulo,
                        costo = recompensaCosto,
                        balance = RewardsStore.balance,
                        saldoTrasCanje = saldoTrasCanje,
                        canjeando = canjeando,
                        onConfirmar = {
                            if (canjeando) return@PreConfirmacion
                            canjeando = true
                            scope.launch {
                                val cupon = RewardsStore.canjearRemoto(
                                    monto = recompensaCosto,
                                    recompensa = recompensaTitulo,
                                    idempotencyKey = idempotencyKey,
                                )
                                canjeando = false
                                if (cupon != null) {
                                    codigoCupon = cupon.codigo
                                    confirmado = true
                                } else {
                                    showToast("No pudimos canjear ahora. Vuelve a intentarlo.")
                                }
                            }
                        },
                        onCancelar = { navigator.pop() }
                    )
                } else {
                    ConfirmacionExitosa(
                        titulo = recompensaTitulo,
                        codigoCupon = codigoCupon,
                        saldoNuevo = RewardsStore.balance,
                        onCopiar = {
                            clipboard.setText(AnnotatedString(codigoCupon))
                            showToast("Código copiado")
                        },
                        onVolver = { navigator.pop() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PreConfirmacion(
    titulo: String,
    costo: Int,
    balance: Int,
    saldoTrasCanje: Int,
    canjeando: Boolean,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(96.dp).background(
                Brush.verticalGradient(listOf(FrutAppColors.AmberCoin, FrutAppColors.AmberCoin.copy(alpha = 0.7f))),
                CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(50.dp))
        }

        Text(titulo, color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        Text(
            "$costo FrutCoins",
            color = FrutAppColors.AmberCoin,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)).padding(16.dp)) {
            ResumenRow("Balance actual", "$balance FrutCoins")
            Spacer(Modifier.height(8.dp))
            ResumenRow("Costo del canje", "-$costo")
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(FrutAppColors.Brand100))
            Spacer(Modifier.height(8.dp))
            ResumenRow("Te quedarán", "$saldoTrasCanje FrutCoins", destacado = true)
        }

        Spacer(Modifier.weight(1f))
    }

    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp)) {
        FrutButtonPrimary(
            text = if (canjeando) "Canjeando…" else "Confirmar canje",
            enabled = !canjeando,
            onClick = onConfirmar,
        )
        Spacer(Modifier.height(8.dp))
        FrutButtonOutline(text = "Cancelar", enabled = !canjeando, onClick = onCancelar)
    }
}

/** Genera un UUID v4 como String (8-4-4-4-12 hex). Ligero, sin depender de
 *  java.util.UUID en commonMain. Usado como idempotency_key del canje. */
private fun uuidV4Random(): String {
    val bytes = ByteArray(16)
    Random.nextBytes(bytes)
    // version 4
    bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
    // variante 10xx
    bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
    fun seg(from: Int, to: Int): String =
        bytes.copyOfRange(from, to).joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    return "${seg(0,4)}-${seg(4,6)}-${seg(6,8)}-${seg(8,10)}-${seg(10,16)}"
}

@Composable
private fun ColumnScope.ConfirmacionExitosa(
    titulo: String,
    codigoCupon: String,
    saldoNuevo: Int,
    onCopiar: () -> Unit,
    onVolver: () -> Unit
) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(96.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
        }

        Text("¡Canje exitoso!", color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text(titulo, color = FrutAppColors.InkMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(20.dp))

        // Tarjeta del cupón
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(FrutAppColors.AmberSoft, RoundedCornerShape(16.dp))
                .border(2.dp, FrutAppColors.AmberCoin.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Tu código de cupón", color = FrutAppColors.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(codigoCupon, color = FrutAppColors.Brand800, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp)).clickable(onClick = onCopiar).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
                Text("Copiar código", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            }
            Text(
                "Aplica el código al pagar tu próximo pedido.",
                color = FrutAppColors.InkMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text("Balance actualizado: $saldoNuevo FrutCoins", color = FrutAppColors.AmberCoin, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.weight(1f))
    }

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp)) {
        FrutButtonPrimary(text = "Volver a FrutCoins", onClick = onVolver)
    }
}

@Composable
private fun ResumenRow(label: String, valor: String, destacado: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (destacado) FrutAppColors.Brand800 else FrutAppColors.InkMuted, fontSize = 13.sp, fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium)
        Text(valor, color = if (destacado) FrutAppColors.Brand800 else FrutAppColors.Ink, fontSize = 13.sp, fontWeight = if (destacado) FontWeight.Bold else FontWeight.SemiBold)
    }
}
