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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.frutcoin
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private data class FormaGanar(val icon: ImageVector, val titulo: String, val puntos: String)
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
        val ganar = listOf(
            FormaGanar(Icons.Filled.ShoppingCart, "Por cada compra", "+50"),
            FormaGanar(Icons.Filled.Recycling, "Reciclar envases", "+30"),
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
                TopBar(onBack = { navigator.pop() })

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Balance(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                    SectionTitle("Cómo ganar FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ganar.forEach { GanarRow(it) }
                    }

                    SectionTitle("Canjea tus FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        recompensas.forEach { RecompensaCard(it, RewardsStore.balance) }
                    }

                    SectionTitle("Desafíos FrutCoins", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        desafios.forEach { DesafioRow(it) }
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
private fun TopBar(onBack: () -> Unit) {
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
        Text("Historial", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { })
    }
}

@Composable
private fun Balance(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(Brush.horizontalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand400)), RoundedCornerShape(22.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(Res.drawable.frutcoin), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.size(54.dp))
        Text("${RewardsStore.balance}", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Text("FrutCoins disponibles", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun GanarRow(item: FormaGanar) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(14.dp)).padding(14.dp),
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
private fun RecompensaCard(item: Recompensa, balance: Int) {
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Canjear", color = if (alcanza) Color.White else FrutAppColors.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
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
