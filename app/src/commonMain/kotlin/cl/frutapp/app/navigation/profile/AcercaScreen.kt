@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.profile

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.brandLogoWhite
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private data class IntegranteEquipo(val nombre: String, val rol: String, val inicial: String)

/**
 * Acerca de FrutApp: hero con marca + historia + equipo + aliados + valores + versión.
 * Pensada para el inversor que entra a "Acerca de" para validar.
 */
class AcercaScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val equipo = listOf(
            IntegranteEquipo("Sebastián Huaitro", "Co-Founder", "S"),
            IntegranteEquipo("Mauricio González", "Co-Founder", "M")
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
                    Text("Acerca de FrutApp", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    HeroBrand()

                    Spacer(Modifier.height(22.dp))
                    SectionTitle("Nuestra historia")
                    Text(
                        "FrutApp nace para resolver una contradicción: la gente quiere lo fresco y barato de la feria, " +
                            "pero sin la lata de ir, estacionar, cargar y hacer fila. El super es caro y menos fresco; " +
                            "los delivery tradicionales traen del super con markup. Nosotros traemos de la feria.",
                        color = FrutAppColors.InkMuted, fontSize = 13.sp, lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(Modifier.height(22.dp))
                    SectionTitle("Misión")
                    Text(
                        "Frutas y verduras de la cosecha a tu mesa, frescas y a buen precio. Con la conveniencia de una " +
                            "app y el ahorro de comprar al por mayor. Y un ecosistema verde que devuelve al ciclo lo que viene.",
                        color = FrutAppColors.InkMuted, fontSize = 13.sp, lineHeight = 19.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(Modifier.height(22.dp))
                    SectionTitle("Equipo")
                    Spacer(Modifier.height(8.dp))
                    equipo.forEach { IntegranteRow(it) }

                    Spacer(Modifier.height(22.dp))
                    ValoresCard()

                    Spacer(Modifier.height(22.dp))
                    SectionTitle("Aliados")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Lo Valledor (proveedor ancla mayorista) · Feriantes seleccionados · " +
                            "TriCiclos · Recicla Tus Pilas · EcoCentros municipales",
                        color = FrutAppColors.InkMuted, fontSize = 12.sp, lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(28.dp))
                    Footer()
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroBrand() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(180.dp)
            .background(Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800)), RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(brandLogoWhite()),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.height(34.dp)
        )
        Text(
            "De la cosecha a tu mesa",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun IntegranteRow(integrante: IntegranteEquipo) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(integrante.inicial, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(integrante.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(integrante.rol, color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun ValoresCard() {
    Column(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Cream, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Text("Nuestros valores", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        listOf(
            "🌿" to "Frescura real, no marketing",
            "💚" to "Precio justo, sin markup escondido",
            "♻️" to "Loop cerrado: lo que llega vuelve al ciclo",
            "🤝" to "Inclusión: la app es para todos, no solo cuicos"
        ).forEach { (emoji, texto) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 16.sp)
                Text(texto, color = FrutAppColors.Ink, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun Footer() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hecho en Chile 🇨🇱 con cariño", color = FrutAppColors.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text("FrutApp v0.1.0 · piloto", color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
    }
}
