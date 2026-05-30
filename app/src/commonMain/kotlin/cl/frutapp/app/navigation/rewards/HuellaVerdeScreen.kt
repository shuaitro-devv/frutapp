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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import cl.frutapp.app.data.HuellaVerdeStore
import cl.frutapp.app.data.NivelRacha
import cl.frutapp.app.data.StreakStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.huella_verde
import frutapp.app.generated.resources.logo_white
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Mi huella verde: dashboard de impacto + tarjeta shareable de marca. Cuando llega
 * `huella_verde.png` (PNG transparente), reemplazamos el placeholder en el hero card.
 */
class HuellaVerdeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val recic = HuellaVerdeStore.reciclajes
        val gramos = HuellaVerdeStore.gramosAlCiclo
        val coins = HuellaVerdeStore.coinsGanados
        val ahorrado = HuellaVerdeStore.ahorradoClp

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(onBack = { navigator.pop() })

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    HeroCard(recic = recic, gramos = gramos, coins = coins, ahorrado = ahorrado)

                    RachaCard(
                        dias = StreakStore.dias,
                        nivel = StreakStore.nivel,
                        faltaParaProximo = StreakStore.diasParaProximoNivel,
                        modifier = Modifier.padding(top = 14.dp)
                    )

                    SectionTitle("Cómo sumar más", Modifier.padding(top = 24.dp, bottom = 8.dp))
                    val tips = listOf(
                        Triple(Icons.Filled.Recycling, "Devuelve tus envases", "+30 coins por reciclaje"),
                        Triple(Icons.Filled.ShoppingCart, "Compra fresco", "+50 coins por compra"),
                        Triple(Icons.Filled.RateReview, "Deja una reseña", "+20 coins"),
                        Triple(Icons.Filled.PersonAdd, "Invita un amigo", "+100 coins")
                    )
                    tips.forEach { (icon, titulo, detalle) -> TipRow(icon, titulo, detalle) }

                    Spacer(Modifier.height(20.dp))
                    FrutButtonPrimary(
                        text = "Compartir mi huella",
                        onClick = { navigator.push(CompartirHuellaScreen()) }
                    )
                    Spacer(Modifier.height(28.dp))
                }
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
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
        }
        Text("Mi huella verde", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun HeroCard(recic: Int, gramos: Int, coins: Int, ahorrado: Int) {
    Box(
        modifier = Modifier.fillMaxWidth().height(420.dp).padding(top = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)))
    ) {
        // Hojas decorativas semi-transparentes para textura del fondo.
        Box(modifier = Modifier.size(220.dp).offset(x = (-80).dp, y = (-60).dp).background(Color.White.copy(alpha = 0.06f), CircleShape).align(Alignment.TopStart))
        Box(modifier = Modifier.size(160.dp).offset(x = 60.dp, y = 60.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).align(Alignment.BottomEnd))
        Box(modifier = Modifier.size(80.dp).offset(x = (-20).dp, y = 20.dp).background(Color.White.copy(alpha = 0.08f), CircleShape).align(Alignment.BottomStart))

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Marca top-left + label top-right
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.logo_white),
                    contentDescription = "FrutApp",
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.height(20.dp)
                )
                Text("MI HUELLA VERDE", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(Res.drawable.huella_verde),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text("$recic", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text("reciclajes", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(16.dp))
            // Grid 1x3 de stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill("${gramos}g", "al ciclo", Modifier.weight(1f))
                StatPill("$coins", "coins", Modifier.weight(1f))
                StatPill(formatClp(ahorrado), "ahorrado", Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))
            Text(
                "De la cosecha a tu mesa · y de vuelta al ciclo",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatPill(valor: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun TipRow(icon: ImageVector, titulo: String, detalle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(titulo, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(detalle, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun RachaCard(dias: Int, nivel: NivelRacha, faltaParaProximo: Int?, modifier: Modifier = Modifier) {
    // Card horizontal: mascota a la izquierda, datos de racha a la derecha.
    Row(
        modifier = modifier.fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(82.dp)
                .background(androidx.compose.ui.graphics.Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(nivel.mascota),
                contentDescription = "Mascota racha ${nivel.titulo}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(72.dp)
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 22.sp)
                Text(
                    "$dias días",
                    color = FrutAppColors.Brand800,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 2.dp)
                    .background(androidx.compose.ui.graphics.Color.White, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(nivel.emoji, fontSize = 12.sp)
                Text(
                    "Nivel ${nivel.titulo}",
                    color = FrutAppColors.Brand800,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            val proximo = nivel.proximoNivel
            if (faltaParaProximo != null && proximo != null) {
                Text(
                    "+$faltaParaProximo días para ${proximo.emoji} ${proximo.titulo}",
                    color = FrutAppColors.InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    "¡Máximo nivel alcanzado!",
                    color = FrutAppColors.Brand600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
