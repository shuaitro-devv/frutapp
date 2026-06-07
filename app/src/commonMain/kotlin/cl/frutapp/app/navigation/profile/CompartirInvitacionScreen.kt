@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.rememberCaptureLayer
import cl.frutapp.app.ui.shareImage
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.brandLogoWhite
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Captura la tarjeta de invitación a imagen y abre el menú nativo de compartir.
 * Mismo patrón que [cl.frutapp.app.navigation.rewards.CompartirHuellaScreen] y
 * [cl.frutapp.app.navigation.canastas.CompartirCanastaScreen].
 */
class CompartirInvitacionScreen(private val code: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val capture = rememberCaptureLayer()
        var compartiendo by remember { mutableStateOf(false) }

        val caption = "🎁 ¡Te invito a FrutApp!\n" +
            "Frutas y verduras frescas de la feria, directo a tu casa.\n" +
            "Usa mi código $code en tu registro y ganamos FrutCoins los dos.\n" +
            "De la cosecha a tu mesa."

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
                    Text("Compartir invitación", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
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
                        .aspectRatio(0.8f) // 4:5 portrait, mismo ratio que Huella/Canasta
                        .clip(RoundedCornerShape(24.dp))
                        .then(capture.modifier)
                ) {
                    ShareCardInvitacion(code = code)
                }

                Spacer(Modifier.weight(1f))

                Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    FrutButtonPrimary(
                        text = if (compartiendo) "Preparando…" else "Compartir invitación",
                        enabled = !compartiendo,
                        onClick = {
                            compartiendo = true
                            scope.launch {
                                runCatching {
                                    val bitmap = capture.toImageBitmap()
                                    shareImage(bitmap, caption, chooserTitle = "Compartir invitación")
                                }.onFailure { e ->
                                    cl.frutapp.app.ui.ErrorReporter.report(screen = "CompartirInvitacion", action = "share_image", error = e)
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
 * Tarjeta vertical 4:5 con la invitación + código destacado. Pensada para capturar y
 * compartir como imagen.
 */
@Composable
private fun ShareCardInvitacion(code: String) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(FrutAppColors.Brand400, FrutAppColors.Brand800)))
    ) {
        // Círculos decorativos semi-transparentes para textura (mismo lenguaje que Huella).
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
                    painter = painterResource(brandLogoWhite()),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.height(22.dp)
                )
                Text("@frutapp.cl", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(18.dp))

            // Icono regalo
            Box(
                modifier = Modifier.size(74.dp).background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
            }

            Text(
                "¡Te invito a FrutApp!",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp)
            )
            Text(
                "Frutas y verduras frescas de la feria,\ndirecto a tu casa.",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(18.dp))

            // Bloque del código — el héroe visual de la tarjeta
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(18.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Usa mi código",
                    color = FrutAppColors.InkSoft,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    code,
                    color = FrutAppColors.Brand800,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    "y ganan FrutCoins los dos 🎉",
                    color = FrutAppColors.Brand600,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                "De la cosecha a tu mesa",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
