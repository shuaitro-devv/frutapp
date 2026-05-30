@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.canastas

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
import cl.frutapp.app.data.Canasta
import cl.frutapp.app.data.CanastaStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.rememberCaptureLayer
import cl.frutapp.app.ui.shareImage
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.logo_white
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Preview + share de una canasta como imagen — mismo patrón que CompartirHuellaScreen.
 * El receptor del mensaje ve la imagen con el listado de productos + total + branding.
 */
class CompartirCanastaScreen(private val canastaId: Int) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val canasta = CanastaStore.get(canastaId)
        if (canasta == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                Text("Canasta no encontrada.", color = FrutAppColors.InkMuted, fontSize = 14.sp)
            }
            return
        }

        val scope = rememberCoroutineScope()
        val capture = rememberCaptureLayer()
        var compartiendo by remember { mutableStateOf(false) }

        val items = canasta.items.joinToString("\n") { "• ${it.producto.nombre}" }
        val caption = "🧺 ${canasta.emoji} ${canasta.nombre}:\n$items\n\n" +
            "Total estimado: ~${formatClp(canasta.totalEstimado)}\n" +
            "Compra esta canasta en FrutApp · De la cosecha a tu mesa."

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
                    Text("Compartir canasta", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Text(
                    "Así se verá la tarjeta:",
                    color = FrutAppColors.InkMuted, fontSize = 13.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 12.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .then(capture.modifier)
                ) {
                    ShareCardCanasta(canasta)
                }

                Spacer(Modifier.weight(1f))

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                    FrutButtonPrimary(
                        text = if (compartiendo) "Preparando…" else "Compartir canasta",
                        enabled = !compartiendo,
                        onClick = {
                            compartiendo = true
                            scope.launch {
                                runCatching {
                                    val bitmap = capture.toImageBitmap()
                                    shareImage(bitmap, caption)
                                }.onFailure {
                                    showToast("No pudimos preparar la imagen.")
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

@Composable
private fun ShareCardCanasta(canasta: Canasta) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)))
    ) {
        Box(modifier = Modifier.size(180.dp).offset(x = (-60).dp, y = (-50).dp).background(Color.White.copy(alpha = 0.07f), CircleShape).align(Alignment.TopStart))
        Box(modifier = Modifier.size(160.dp).offset(x = 50.dp, y = 50.dp).background(Color.White.copy(alpha = 0.06f), CircleShape).align(Alignment.BottomEnd))

        Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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

            Spacer(Modifier.height(20.dp))
            // Emoji enorme + nombre canasta
            Text(canasta.emoji, fontSize = 70.sp)
            Text(canasta.nombre, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Text("${canasta.cantidadProductos} productos · ~${formatClp(canasta.totalEstimado)}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(14.dp))
            // Mini-grid de productos: primeros 6 con imagen
            val muestra = canasta.items.take(6)
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                muestra.chunked(3).forEach { fila ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        fila.forEach { item ->
                            ProductoMini(item.producto.imagen, item.producto.nombre, Modifier.weight(1f))
                        }
                        // Padding si menos de 3 en la fila
                        if (fila.size < 3) repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "De la cosecha a tu mesa · cómprala en FrutApp",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProductoMini(imagen: org.jetbrains.compose.resources.DrawableResource, nombre: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(imagen),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        )
        Text(
            nombre,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
