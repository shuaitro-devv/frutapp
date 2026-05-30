package cl.frutapp.app.navigation.profile

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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CartStore
import cl.frutapp.app.data.DireccionesStore
import cl.frutapp.app.data.FavoritesStore
import cl.frutapp.app.data.MetodosPagoStore
import cl.frutapp.app.data.RewardsStore
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.OrderApi
import cl.frutapp.app.legal.LegalDocKind
import cl.frutapp.app.navigation.OnboardingScreen
import cl.frutapp.app.navigation.auth.LoginScreen
import cl.frutapp.app.navigation.legal.LegalDocScreen
import cl.frutapp.app.navigation.rewards.FrutCoinsScreen
import cl.frutapp.app.navigation.rewards.HuellaVerdeScreen
import cl.frutapp.app.ui.comingSoon
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.theme.FrutAppColors

private data class MenuItem(val icon: ImageVector, val label: String, val onClick: () -> Unit = {})

/**
 * Perfil (mockup 16): datos del usuario (reales, desde [TokenStore]), badge de FrutCoins,
 * secciones de cuenta/preferencias/opciones y cerrar sesión.
 */
class ProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val user = TokenStore.user
        LaunchedEffect(Unit) {
            runCatching { OrderApi().frutCoins() }.onSuccess { RewardsStore.set(it.balance) }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Perfil", color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Ayuda", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { comingSoon() })
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    UserCard(
                        nombre = user?.name ?: "Invitado",
                        email = user?.email ?: "",
                        telefono = user?.phone,
                        onFrutCoins = { navigator.push(FrutCoinsScreen()) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    MenuSection(
                        "Mis cuentas y direcciones",
                        listOf(
                            MenuItem(Icons.Filled.Park, "Mi huella verde", onClick = { navigator.push(HuellaVerdeScreen()) }),
                            MenuItem(Icons.Filled.Place, "Direcciones", onClick = { navigator.push(DireccionesScreen()) }),
                            MenuItem(Icons.Filled.CreditCard, "Métodos de pago", onClick = { navigator.push(MetodosPagoScreen()) }),
                            MenuItem(Icons.Filled.Link, "Cuentas vinculadas", onClick = { comingSoon() })
                        )
                    )
                    MenuSection(
                        "Preferencias",
                        listOf(
                            MenuItem(Icons.Filled.Notifications, "Notificaciones", onClick = { comingSoon() }),
                            MenuItem(Icons.Filled.Language, "Idioma", onClick = { comingSoon() })
                        )
                    )
                    MenuSection(
                        "Más opciones",
                        listOf(
                            MenuItem(Icons.Filled.SupportAgent, "Ayuda", onClick = { comingSoon() }),
                            MenuItem(Icons.Filled.Slideshow, "Ver intro", onClick = { navigator.push(OnboardingScreen(desdeSplash = false)) }),
                            MenuItem(Icons.Filled.Description, "Términos y condiciones", onClick = { navigator.push(LegalDocScreen(LegalDocKind.TERMS)) }),
                            MenuItem(Icons.Filled.PrivacyTip, "Privacidad", onClick = { navigator.push(LegalDocScreen(LegalDocKind.PRIVACY)) }),
                            MenuItem(Icons.Filled.Info, "Acerca de FrutApp", onClick = { comingSoon() })
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                            .clickable {
                                // Limpia TODA la sesión y los stores en memoria para que el
                                // próximo usuario no herede carrito/FrutCoins/pedidos.
                                CartStore.clear()
                                RewardsStore.reset()
                                DireccionesStore.reset()
                                MetodosPagoStore.reset()
                                FavoritesStore.clear()
                                TokenStore.clear()
                                navigator.replaceAll(LoginScreen())
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = FrutAppColors.Error, modifier = Modifier.size(22.dp))
                        Text("Cerrar sesión", color = FrutAppColors.Error, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 12.dp))
                    }

                    Box(modifier = Modifier.padding(20.dp)) {
                        FrutButtonPrimary(text = "Invita a un amigo", onClick = { navigator.push(InvitarAmigoScreen()) })
                    }
                    Spacer(Modifier.height(8.dp))
                }

                FrutBottomNav(
                    selected = FrutTab.PERFIL,
                    onSelect = { tab -> if (tab != FrutTab.PERFIL) navigator.popUntilRoot() }
                )
            }
        }
    }
}

@Composable
private fun UserCard(nombre: String, email: String, telefono: String?, onFrutCoins: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(18.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(FrutAppColors.Brand400, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(nombre.take(1).uppercase(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                Text(nombre, color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(email, color = FrutAppColors.InkMuted, fontSize = 13.sp, maxLines = 1)
                if (!telefono.isNullOrBlank()) {
                    Text(telefono, color = FrutAppColors.InkMuted, fontSize = 13.sp)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Editar perfil", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { comingSoon() })
            Box(
                modifier = Modifier.background(FrutAppColors.AmberSoft, RoundedCornerShape(12.dp)).clickable(onClick = onFrutCoins).padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("${RewardsStore.balance} FrutCoins", color = FrutAppColors.AmberCoin, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MenuSection(titulo: String, items: List<MenuItem>) {
    Text(titulo, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp))
    Column(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
    ) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
                }
                Text(item.label, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 12.dp))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(20.dp))
            }
        }
    }
}
