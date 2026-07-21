@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.rewards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.data.remote.AuthApi
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.navigation.recycle.ReciclaScreen
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.comingSoon
import cl.frutapp.app.ui.shareText
import cl.frutapp.shared.domain.ReferralConfig
import cl.frutapp.shared.dto.FrutCoinsEntryDto
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.frutcoin
import frutapp.app.generated.resources.mascota_coin
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private data class FormaGanar(val icon: ImageVector, val titulo: String, val puntos: String, val onClick: () -> Unit = {})
private data class Recompensa(val titulo: String, val costo: Int)
private data class Desafio(val titulo: String, val actual: Int, val meta: Int)

/**
 * FrutCoins (mockup 14): balance, formas de ganar, canje de recompensas y desafíos.
 * Balance desde [RewardsStore] (refleja lo ganado al comprar). Acentos dorados.
 */
class FrutCoinsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var movimientos by remember { mutableStateOf<List<FrutCoinsEntryDto>>(emptyList()) }
        // Codigo de invitacion propio. Se lee del /auth/me (backend lo genera
        // lazy si el user es pre-V42). Null hasta que carga; el card lo esconde.
        var codigoInvitacion by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            runCatching { OrderApi().frutCoins() }
                .onSuccess {
                    RewardsStore.set(it.balance)
                    movimientos = it.movimientos
                }
                .onFailure { e ->
                    ErrorReporter.report(screen = "FrutCoins", action = "load_balance", error = e)
                }
            runCatching { AuthApi().me() }
                .onSuccess { codigoInvitacion = it.codigoInvitacion }
        }
        val ganar = listOf(
            FormaGanar(Icons.Filled.ShoppingCart, "Por cada compra", "+50"),
            FormaGanar(Icons.Filled.Recycling, "Reciclar envases", "+30", onClick = { navigator.push(ReciclaScreen()) }),
            FormaGanar(Icons.Filled.RateReview, "Dejar una reseña", "+20"),
            FormaGanar(Icons.Filled.PersonAdd, "Referir un amigo", "+100")
        )
        val recompensas = listOf(
            Recompensa("Envío gratis", 200),
            Recompensa("$1.000 de descuento", 500),
            Recompensa("Caja sorpresa de frutas", 800)
        )
        val desafios = listOf(
            Desafio("Compra 5 veces este mes", 3, 5),
            Desafio("Recicla 3 veces", 1, 3)
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(onBack = { navigator.pop() }, onHistorial = { navigator.push(HistorialCoinsScreen()) })

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Balance(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                    codigoInvitacion?.let { codigo ->
                        SectionTitle("Invita a un amigo", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                        CodigoInvitacionCard(
                            codigo = codigo,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }

                    SectionTitle("Cómo ganar FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ganar.forEach { GanarRow(it) }
                    }

                    SectionTitle("Canjea tus FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recompensas.forEach { r ->
                            RecompensaCard(
                                item = r,
                                balance = RewardsStore.balance,
                                onCanjear = { navigator.push(CanjearScreen(r.titulo, r.costo)) }
                            )
                        }
                    }

                    SectionTitle("Desafíos FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        desafios.forEach { DesafioRow(it) }
                    }

                    SectionTitle("Historial", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    if (movimientos.isEmpty()) {
                        Text(
                            "Aún no tienes movimientos.",
                            color = FrutAppColors.InkMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            movimientos.forEach { MovimientoRow(it) }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                FrutBottomNav(
                    selected = FrutTab.PEDIDOS,
                    onSelect = { tab -> if (tab != FrutTab.PEDIDOS) navigator.popUntilRoot() }
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onHistorial: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
        }
        Text("FrutCoins", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp).weight(1f))
        Text("Historial", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onHistorial))
    }
}

@Composable
private fun Balance(modifier: Modifier = Modifier) {
    val balance = RewardsStore.balance
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(
                Brush.horizontalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand400)),
                RoundedCornerShape(22.dp)
            )
            .clip(RoundedCornerShape(22.dp))
    ) {
        // Monedas decorativas semi-transparentes: textura de fondo para que no se vea vacío.
        Box(modifier = Modifier.size(140.dp).offset(x = (-40).dp, y = (-40).dp).background(Color.White.copy(alpha = 0.08f), CircleShape).align(Alignment.TopStart))
        Box(modifier = Modifier.size(70.dp).offset(x = (-12).dp, y = 30.dp).background(Color.White.copy(alpha = 0.10f), CircleShape).align(Alignment.BottomStart))
        Box(modifier = Modifier.size(180.dp).offset(x = 60.dp, y = (-60).dp).background(Color.White.copy(alpha = 0.07f), CircleShape).align(Alignment.TopEnd))
        Box(modifier = Modifier.size(40.dp).offset(x = (-20).dp, y = (-10).dp).background(Color.White.copy(alpha = 0.10f), CircleShape).align(Alignment.BottomEnd))

        Row(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tu saldo", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text("$balance", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
                    Text("FC", color = Color.White.copy(alpha = 0.7f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp, bottom = 12.dp))
                }
                Text(
                    "Cubre hasta el 20% de tus próximos pedidos",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Image(
                painter = painterResource(Res.drawable.mascota_coin),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(140.dp).rotate(-8f)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun GanarRow(item: FormaGanar) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)).clickable(onClick = item.onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
        }
        Text(item.titulo, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Box(modifier = Modifier.background(FrutAppColors.AmberSoft, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text(item.puntos, color = FrutAppColors.AmberCoin, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecompensaCard(item: Recompensa, balance: Int, onCanjear: () -> Unit) {
    val alcanza = balance >= item.costo
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(FrutAppColors.AmberSoft, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.titulo, color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("${item.costo} FrutCoins", color = FrutAppColors.AmberCoin, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
            }
            Box(
                modifier = Modifier
                    .background(if (alcanza) FrutAppColors.Brand400 else FrutAppColors.Brand100, RoundedCornerShape(12.dp))
                    .clickable(enabled = alcanza, onClick = onCanjear)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    if (alcanza) "Canjear" else "Te faltan",
                    color = if (alcanza) Color.White else FrutAppColors.InkSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

internal fun motivoLabel(motivo: String): String = when (motivo) {
    "COMPRA" -> "Compra"
    "CANJE" -> "Canje"
    "REEMBOLSO" -> "Reembolso"
    "AJUSTE" -> "Ajuste"
    else -> motivo
}

@Composable
internal fun MovimientoRow(item: FrutCoinsEntryDto) {
    val positivo = item.delta >= 0
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(motivoLabel(item.motivo), color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(item.fecha.take(10), color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
        Text(
            (if (positivo) "+" else "") + "${item.delta}",
            color = if (positivo) FrutAppColors.Brand600 else FrutAppColors.Error,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CodigoInvitacionCard(codigo: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(
            "Comparte este código con tus amigos:",
            color = FrutAppColors.InkSoft,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                codigo,
                color = FrutAppColors.Brand800,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .background(FrutAppColors.Brand600, RoundedCornerShape(10.dp))
                    .clickable {
                        // URL a la landing /invita/{codigo}: WhatsApp/Facebook
                        // genera preview con og:image + og:title dinamicos
                        // (nombre del referidor). Sin URL, el share seria
                        // texto plano sin preview. La landing captura el
                        // codigo y ademas activa el Google Play install
                        // referrer para autocompletarlo al instalar la app.
                        val texto = "¡Descarga FrutApp con mi código $codigo y ganamos FrutCoins los dos! De la cosecha a tu mesa 🥑🍅\n\nhttps://frutapp.cl/invita/$codigo"
                        shareText(texto)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("Compartir", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Cuando tu amigo se registre con tu código y complete su primer pedido, tú recibes ${ReferralConfig.BONO_REFERIDOR} FrutCoins y él/ella recibe ${ReferralConfig.BONO_REFERIDO}.",
            color = FrutAppColors.InkSoft,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun DesafioRow(item: Desafio) {
    val progreso = (item.actual.toFloat() / item.meta).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.titulo, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${item.actual}/${item.meta}", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(7.dp).background(FrutAppColors.Brand100, RoundedCornerShape(4.dp))) {
            Box(modifier = Modifier.fillMaxWidth(progreso).height(7.dp).background(FrutAppColors.Brand400, RoundedCornerShape(4.dp)))
        }
    }
}
