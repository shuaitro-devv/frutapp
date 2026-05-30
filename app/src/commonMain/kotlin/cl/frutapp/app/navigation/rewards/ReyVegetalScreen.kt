package cl.frutapp.app.navigation.rewards

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.theme.FrutAppColors

private data class Rey(val nombre: String, val comuna: String, val reciclajes: Int, val racha: Int, val puesto: Int)

/**
 * Rey Vegetal del Mes (mockup gamificación). Mauricio lo lanzó como idea para premiar
 * al mejor reciclador del mes con compra gratis o coins extra. Aquí mockeamos el hero
 * + ranking + disclaimer "en desarrollo".
 */
class ReyVegetalScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val reyActual = Rey("María Pérez", "Ñuñoa", reciclajes = 47, racha = 92, puesto = 1)
        val top = listOf(
            Rey("María Pérez", "Ñuñoa", 47, 92, 1),
            Rey("Carlos R.", "Providencia", 41, 68, 2),
            Rey("Sebastián H.", "Las Condes", 38, 54, 3),
            Rey("Daniela V.", "La Reina", 32, 47, 4),
            Rey("Felipe M.", "Ñuñoa", 28, 41, 5)
        )

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
                    Text("Rey Vegetal del mes", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    ReyHero(reyActual)

                    Spacer(Modifier.height(22.dp))
                    Text("Top 5 del mes", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    top.forEach { RankingRow(it) }

                    Spacer(Modifier.height(22.dp))
                    ComoGanarCard()

                    Spacer(Modifier.height(18.dp))
                    DisclaimerCard()
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun ReyHero(rey: Rey) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(220.dp)
            .background(Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)), RoundedCornerShape(22.dp))
    ) {
        // Decoración
        Box(modifier = Modifier.size(180.dp).offset(x = (-60).dp, y = (-60).dp).background(Color.White.copy(alpha = 0.07f), CircleShape).align(Alignment.TopStart))
        Box(modifier = Modifier.size(120.dp).offset(x = 40.dp, y = 30.dp).background(Color.White.copy(alpha = 0.05f), CircleShape).align(Alignment.BottomEnd))

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("👑", fontSize = 44.sp)
            Text("Rey Vegetal del mes", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            Text(rey.nombre, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            Text("${rey.comuna} · ${rey.reciclajes} reciclajes · 🔥 ${rey.racha} días", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .background(FrutAppColors.AmberCoin, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("🎁 Le regalamos su próxima compra", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RankingRow(rey: Rey) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(if (rey.puesto == 1) FrutAppColors.AmberSoft else FrutAppColors.Brand50, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp)
                .background(
                    when (rey.puesto) { 1 -> FrutAppColors.AmberCoin; 2 -> FrutAppColors.Brand400; 3 -> FrutAppColors.Brand200; else -> FrutAppColors.Brand100 },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("${rey.puesto}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(rey.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(rey.comuna, color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${rey.reciclajes}", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("reciclajes", color = FrutAppColors.InkSoft, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ComoGanarCard() {
    Column(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Text("¿Cómo me convierto en Rey?", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        listOf(
            "Acumula reciclajes durante el mes",
            "Mantén tu racha verde activa",
            "Comparte tu huella verde con amigos"
        ).forEach { Tip(it) }
    }
}

@Composable
private fun Tip(texto: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🌱", fontSize = 14.sp)
        Text(texto, color = FrutAppColors.Ink, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun DisclaimerCard() {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(FrutAppColors.Cream, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🛈", fontSize = 16.sp)
        Text(
            "Programa en desarrollo. El primer ganador real se anuncia cuando se cierren los criterios de elegibilidad y el presupuesto del premio.",
            color = FrutAppColors.InkMuted, fontSize = 11.sp, lineHeight = 15.sp, textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
