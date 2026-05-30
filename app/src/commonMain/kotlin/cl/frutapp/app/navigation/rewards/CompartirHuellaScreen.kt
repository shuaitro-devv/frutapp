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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.rememberCaptureLayer
import cl.frutapp.app.ui.shareImage
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.huella_verde
import frutapp.app.generated.resources.logo_white
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Pantalla de "Compartir mi huella verde": preview de la tarjeta exacta que se va a compartir
 * + botón que la captura a imagen y abre el menú nativo. Patrón Instagram/Strava.
 */
class CompartirHuellaScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val capture = rememberCaptureLayer()
        var compartiendo by remember { mutableStateOf(false) }

        val recic = HuellaVerdeStore.reciclajes
        val gramos = HuellaVerdeStore.gramosAlCiclo
        val coins = HuellaVerdeStore.coinsGanados
        val ahorrado = HuellaVerdeStore.ahorradoClp
        val dias = StreakStore.dias
        val nivel = StreakStore.nivel

        val caption = "🌿 Mi huella verde con FrutApp:\n" +
            "$recic reciclajes · ${gramos}g al ciclo · $coins FrutCoins · ${formatClp(ahorrado)} ahorrado.\n" +
            "🔥 $dias días en racha verde · ${nivel.emoji} nivel ${nivel.titulo}\n" +
            "De la Vega a tu mesa · y de vuelta al ciclo."

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
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
                    Text("Compartir mi huella", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Text(
                    "Así se verá tu tarjeta:",
                    color = FrutAppColors.InkMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 12.dp)
                )

                // Preview de la tarjeta — wrapped en el CaptureLayer para que se pueda capturar.
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .then(capture.modifier)
                ) {
                    ShareCardHuella(
                        recic = recic,
                        gramos = gramos,
                        coins = coins,
                        ahorrado = ahorrado,
                        dias = dias,
                        nivel = nivel
                    )
                }

                Spacer(Modifier.weight(1f))

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    FrutButtonPrimary(
                        text = if (compartiendo) "Preparando…" else "Compartir mi huella",
                        enabled = !compartiendo,
                        onClick = {
                            compartiendo = true
                            scope.launch {
                                runCatching {
                                    val bitmap = capture.toImageBitmap()
                                    shareImage(bitmap, caption)
                                }.onFailure {
                                    showToast("No pudimos preparar la imagen. Intenta de nuevo.")
                                }
                                compartiendo = false
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    FrutButtonOutline(text = "Cancelar", onClick = { navigator.pop() })
                }
            }
        }
    }
}

/**
 * Tarjeta cuadrada (1:1) con el resumen visual de la huella verde — pensada para que se
 * capture y comparta como imagen en WhatsApp / IG / etc.
 */
@Composable
fun ShareCardHuella(
    recic: Int,
    gramos: Int,
    coins: Int,
    ahorrado: Int,
    dias: Int,
    nivel: NivelRacha,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)))
    ) {
        // Hojas decorativas semi-transparentes para textura.
        Box(modifier = Modifier.size(180.dp).offset(x = (-60).dp, y = (-50).dp).background(Color.White.copy(alpha = 0.07f), CircleShape).align(Alignment.TopStart))
        Box(modifier = Modifier.size(160.dp).offset(x = 50.dp, y = 50.dp).background(Color.White.copy(alpha = 0.06f), CircleShape).align(Alignment.BottomEnd))
        Box(modifier = Modifier.size(80.dp).offset(x = (-20).dp, y = (-20).dp).background(Color.White.copy(alpha = 0.10f), CircleShape).align(Alignment.BottomStart))

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: logo + @handle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(Res.drawable.logo_white),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.height(22.dp)
                )
                Text("@frutapp.cl", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            // Hero: huella + mascota racha overlap
            Box(
                modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.huella_verde),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(150.dp).offset(x = (-32).dp)
                )
                Image(
                    painter = painterResource(nivel.mascota),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(110.dp).offset(x = 50.dp, y = 10.dp).rotate(8f)
                )
            }

            // Título grande: racha
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Text("🔥", fontSize = 28.sp)
                Text(
                    "$dias días",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Text(
                "en racha verde · ${nivel.emoji} ${nivel.titulo}",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            // Grid 2x2 de stats abajo
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChipShare("$recic", "reciclajes", Modifier.weight(1f))
                StatChipShare("${gramos}g", "al ciclo", Modifier.weight(1f))
                StatChipShare("$coins", "coins", Modifier.weight(1f))
                StatChipShare(formatClp(ahorrado), "ahorrado", Modifier.weight(1f))
            }

            Spacer(Modifier.weight(1f))
            Text(
                "De la Vega a tu mesa · y de vuelta al ciclo",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatChipShare(valor: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.13f), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
