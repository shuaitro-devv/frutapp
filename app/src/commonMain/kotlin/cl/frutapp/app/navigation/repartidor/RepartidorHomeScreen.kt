package cl.frutapp.app.navigation.repartidor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.navigation.auth.LoginScreen
import cl.frutapp.app.navigation.staff.PerfilStaff
import cl.frutapp.app.navigation.staff.StaffAyudaScreen
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.StaffBottomNav
import cl.frutapp.app.ui.components.StaffCenterButton
import cl.frutapp.app.ui.components.StaffQuickAction
import cl.frutapp.app.ui.components.StaffTab
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Home del perfil Repartidor. Placeholder: 4 tabs (Despachos/En ruta/Entregados/Perfil) con
 * cuerpo "Próximamente". Mismo shell que PickerHomeScreen, distintas tabs e iconografia.
 *
 * Bottom nav del mockup -06 (saldo) tenia 5 tabs, lo dejamos en 4 para alinear con el resto
 * del repartidor — el saldo va dentro de Perfil cuando se construya esa pantalla.
 */
class RepartidorHomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedTab by rememberSaveable { mutableStateOf("despachos") }

        val tabs = remember {
            listOf(
                StaffTab("despachos", "Despachos", Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
                StaffTab("en_ruta", "En ruta", Icons.Filled.Route, Icons.Outlined.Route),
                StaffTab("entregados", "Entregados", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
                StaffTab("perfil", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
            )
        }

        // Scanner del voucher del pedido: el repartidor escanea el QR/barcode
        // pegado a la caja al retirar en bodega para confirmar el pedido
        // fisico. Al leer muestra el codigo — integrar con matching de
        // pedido en cola es follow-up.
        val scanner = cl.frutapp.app.platform.rememberScanner(
            prompt = "Apunta al QR del pedido",
        ) { codigo ->
            showToast("Código leído: $codigo")
        }

        // Centro destacado del repartidor: las acciones recurrentes en ruta son navegar al
        // destino, marcar llegada y reportar problemas. Por ahora todo no-op con toast.
        val center = StaffCenterButton(
            icon = Icons.Filled.NearMe,
            contentDescription = "Acción rápida",
            selected = false,
            onClick = { showToast("Acción rápida - Próximamente") },
            quickActions = listOf(
                StaffQuickAction(Icons.Filled.Map, "Navegar a destino") {
                    showToast("Navegar - Próximamente")
                },
                StaffQuickAction(Icons.Filled.QrCodeScanner, "Escanear entrega") {
                    scanner.escanear()
                },
                StaffQuickAction(Icons.Filled.ReportProblem, "Reportar incidencia") {
                    showToast("Reportar - Próximamente")
                }
            )
        )

        Scaffold(
            bottomBar = {
                StaffBottomNav(
                    tabs = tabs,
                    selectedId = selectedTab,
                    onSelect = { selectedTab = it },
                    center = center
                )
            },
            containerColor = FrutAppColors.Background
        ) { innerPadding ->
            val tituloTab = tabs.firstOrNull { it.id == selectedTab }?.label ?: ""
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (selectedTab) {
                    "despachos" -> RepartidorColaContent()
                    "en_ruta" -> RepartidorEnRutaContent()
                    "entregados" -> RepartidorEntregadosContent()
                    "perfil" -> Column(modifier = Modifier.fillMaxSize()) {
                        // Hasta tener una pantalla 'Perfil' propia, mostramos el saldo del
                        // repartidor en esta tab y agregamos el boton de logout al final.
                        Box(modifier = Modifier.weight(1f)) {
                            RepartidorSaldoContent(
                                onAyuda = { navigator.push(StaffAyudaScreen(PerfilStaff.REPARTIDOR)) }
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp)) {
                            FrutButtonOutline(
                                text = "Cerrar sesión",
                                onClick = {
                                    TokenStore.clear()
                                    navigator.replaceAll(LoginScreen())
                                }
                            )
                        }
                    }
                    else -> ProximamentePlaceholder(modifier = Modifier.fillMaxSize(), titulo = tituloTab)
                }
            }
        }
    }
}

@Composable
private fun ProximamentePlaceholder(
    modifier: Modifier = Modifier,
    titulo: String,
    onLogout: (() -> Unit)? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).background(FrutAppColors.Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = FrutAppColors.Brand400,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = titulo,
                color = FrutAppColors.Brand800,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Próximamente",
                color = FrutAppColors.InkMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Estamos preparando la herramienta del repartidor.\nVuelve en unos días.",
                color = FrutAppColors.InkSoft,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            if (onLogout != null) {
                Spacer(Modifier.height(28.dp))
                FrutButtonOutline(
                    text = "Cerrar sesión",
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )
            }
        }
    }
}
