@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.recycle

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.WineBar
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
import cl.frutapp.app.navigation.rewards.FrutCoinsScreen
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.contenedores_reciclaje
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private data class TipoReciclaje(val icon: ImageVector, val label: String)
private data class Punto(val nombre: String, val direccion: String, val horario: String)

/**
 * Recicla (mockup 13): hero con contenedores, tipos de reciclaje, puntos cercanos,
 * consejos y CTA para ganar FrutCoins reciclando.
 */
class ReciclaScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val tipos = listOf(
            TipoReciclaje(Icons.Filled.LocalDrink, "Plástico"),
            TipoReciclaje(Icons.Filled.Description, "Papel"),
            TipoReciclaje(Icons.Filled.WineBar, "Vidrio"),
            TipoReciclaje(Icons.Filled.Compost, "Orgánico"),
            TipoReciclaje(Icons.Filled.BatteryFull, "Pilas")
        )
        val puntos = listOf(
            Punto("Punto Verde Plaza Ñuñoa", "Av. Irarrázaval 4200, Ñuñoa", "Lun a Sáb · 09:00 - 19:00"),
            Punto("EcoCentro La Reina", "Av. Larraín 9750, La Reina", "Todos los días · 10:00 - 18:00")
        )
        val consejos = listOf(
            "Enjuaga los envases antes de reciclarlos.",
            "Separa por material: plástico, papel, vidrio.",
            "Compacta las botellas para ahorrar espacio."
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
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
                    }
                    Text("Recicla", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                LazyColumn(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                    item { Hero(modifier = Modifier.padding(horizontal = 20.dp)) }

                    item { SectionTitle("¿Qué deseas reciclar?", Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp)) }
                    item {
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tipos.size) { i -> TipoItem(tipos[i]) }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Puntos de reciclaje cerca de ti", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Ver mapa", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { })
                        }
                    }
                    items(puntos.size) { i -> PuntoCard(puntos[i]) }

                    item { SectionTitle("Consejos para reciclar mejor", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)) }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            consejos.forEach { ConsejoRow(it) }
                        }
                    }

                    item {
                        Box(modifier = Modifier.padding(20.dp)) {
                            FrutButtonPrimary(text = "Gana FrutCoins reciclando", onClick = { navigator.push(FrutCoinsScreen()) })
                        }
                    }
                }

                FrutBottomNav(selected = FrutTab.INICIO, onSelect = { navigator.popUntilRoot() })
            }
        }
    }
}

@Composable
private fun Hero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand400)), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Text("Recicla", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Pequeñas acciones, gran impacto", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
        Image(
            painter = painterResource(Res.drawable.contenedores_reciclaje),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 12.dp)
        )
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun TipoItem(tipo: TipoReciclaje) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(64.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(tipo.icon, contentDescription = tipo.label, tint = FrutAppColors.Brand600, modifier = Modifier.size(28.dp))
        }
        Text(tipo.label, color = FrutAppColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun PuntoCard(punto: Punto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(punto.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                Box(modifier = Modifier.padding(start = 8.dp).background(FrutAppColors.Brand200, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Abierto", color = FrutAppColors.Brand800, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(punto.direccion, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Text(punto.horario, color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun ConsejoRow(texto: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FrutAppColors.Cream, RoundedCornerShape(12.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(20.dp))
        Text(texto, color = FrutAppColors.Ink, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
    }
}
