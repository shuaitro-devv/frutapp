@file:OptIn(ExperimentalResourceApi::class, ExperimentalFoundationApi::class)

package cl.frutapp.app.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import cl.frutapp.app.navigation.auth.LoginScreen
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.LocalBrand
import cl.frutapp.app.ui.theme.brandLogoMain
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.mascota_cajita
import frutapp.app.generated.resources.mascota_coin
import frutapp.app.generated.resources.mascota_palta
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private data class OnboardChip(val icon: ImageVector, val label: String)
private data class OnboardPage(val imagen: DrawableResource, val titulo: String, val detalle: String, val chips: List<OnboardChip>)

private val PAGINAS = listOf(
    OnboardPage(
        Res.drawable.mascota_palta,
        "Soy Palta",
        "Te traigo lo más fresco de la feria, directo a tu mesa.",
        listOf(
            OnboardChip(Icons.Filled.Spa, "Fresco"),
            OnboardChip(Icons.Filled.Storefront, "De feria"),
            OnboardChip(Icons.Filled.Schedule, "Rápido")
        )
    ),
    OnboardPage(
        Res.drawable.mascota_coin,
        "Yo soy Coin",
        "Acumulas conmigo en cada compra y pagas con lo que ganaste.",
        listOf(
            OnboardChip(Icons.Filled.MonetizationOn, "Acumulas"),
            OnboardChip(Icons.Filled.Savings, "Ahorras"),
            OnboardChip(Icons.Filled.CardGiftcard, "Premios")
        )
    ),
    OnboardPage(
        Res.drawable.mascota_cajita,
        "Soy Cajita",
        "Llevo tu pedido a tu puerta y te aviso cada paso del camino.",
        listOf(
            OnboardChip(Icons.Filled.LocalShipping, "Entrega ágil"),
            OnboardChip(Icons.Filled.Place, "En vivo"),
            OnboardChip(Icons.Filled.Spa, "Sin estrés")
        )
    )
)

/**
 * Onboarding (intro de 3 slides). Desde el Splash (desdeSplash=true) al terminar va a Login;
 * abierto manualmente desde el perfil (desdeSplash=false) solo vuelve atrás.
 */
class OnboardingScreen(private val desdeSplash: Boolean = true) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { PAGINAS.size })
        val esUltima = pagerState.currentPage == PAGINAS.lastIndex

        val terminar = {
            if (desdeSplash) navigator.replace(LoginScreen()) else navigator.pop()
        }

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            // Barra superior limpia: logo + Saltar sobre blanco (sin ola, sin foto).
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(brandLogoMain()),
                    contentDescription = LocalBrand.current.displayName,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.height(28.dp)
                )
                Text("Saltar", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { terminar() })
            }

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                val p = PAGINAS[page]
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Burbuja verde claro con la mascota centrada. Al reemplazar la imagen por
                    // un PNG transparente, el verde del fondo se ve completo.
                    Box(
                        modifier = Modifier.size(280.dp)
                            .background(FrutAppColors.Brand50, RoundedCornerShape(36.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(p.imagen),
                            contentDescription = p.titulo,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(220.dp)
                        )
                    }
                    Text(
                        p.titulo,
                        color = FrutAppColors.Brand800,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 28.dp)
                    )
                    Text(
                        p.detalle,
                        color = FrutAppColors.InkMuted,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        p.chips.forEach { ChipBeneficio(it) }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(PAGINAS.size) { i ->
                    val seleccionado = pagerState.currentPage == i
                    val ancho by animateDpAsState(if (seleccionado) 22.dp else 8.dp)
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp).height(8.dp).width(ancho)
                            .background(if (seleccionado) FrutAppColors.Brand400 else FrutAppColors.Brand100, CircleShape)
                    )
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp)) {
                FrutButtonPrimary(
                    text = if (esUltima) (if (desdeSplash) "Comenzar" else "Entendido") else "Siguiente",
                    onClick = {
                        if (esUltima) terminar()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChipBeneficio(c: OnboardChip) {
    Row(
        modifier = Modifier
            .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(c.icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(14.dp))
        Text(c.label, color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
    }
}
