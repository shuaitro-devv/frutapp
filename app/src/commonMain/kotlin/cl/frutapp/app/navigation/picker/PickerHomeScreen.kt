package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
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
 * Home del perfil Picker. Por ahora es un placeholder: 4 tabs (Cola/En curso/Listos/Perfil)
 * con un cuerpo "Próximamente" para cualquiera de las pestañas. Cuando se construyan las
 * pantallas reales segun los mockups, cada tab pasara a renderizar su pantalla propia.
 *
 * NO usa el FrutBottomNav del cliente porque ese tiene el boton de carrito al centro
 * (concepto cliente-only). Usa StaffBottomNav, mas plano y reusable por todos los roles staff.
 */
class PickerHomeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedTab by rememberSaveable { mutableStateOf("cola") }

        val tabs = remember {
            listOf(
                StaffTab("cola", "Cola", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
                StaffTab("en_curso", "En curso", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
                StaffTab("listos", "Listos", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
                StaffTab("perfil", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
            )
        }

        // Centro destacado: por ahora todo no-op con toast 'Próximamente'. Cuando esten
        // los flujos reales se cablean a: tomar siguiente pedido, escanear codigo y reportar
        // problema (estas son las acciones recurrentes del picker durante el turno).
        val center = StaffCenterButton(
            icon = Icons.Filled.Bolt,
            contentDescription = "Acción rápida",
            selected = false,
            onClick = { showToast("Acción rápida - Próximamente") },
            quickActions = listOf(
                StaffQuickAction(Icons.Filled.PlayCircle, "Tomar siguiente") {
                    showToast("Tomar siguiente - Próximamente")
                },
                StaffQuickAction(Icons.Filled.QrCodeScanner, "Escanear código") {
                    showToast("Escanear - Próximamente")
                },
                StaffQuickAction(Icons.Filled.ReportProblem, "Reportar problema") {
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
            // Cada tab renderiza su propia pantalla. Las que todavia no estan construidas
            // caen al ProximamentePlaceholder. Cuando se sumen mas pantallas reales (picker-02
            // detalle/picklist se accede via tap en un pedido — vive en su propio Screen), se
            // van agregando ramas al `when` y removiendo del placeholder.
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (selectedTab) {
                    "cola" -> PickerColaContent()
                    "en_curso" -> PickerEnCursoContent()
                    "listos" -> PickerListosContent()
                    "perfil" -> PickerPerfilContent(
                        modifier = Modifier.fillMaxSize(),
                        onLogout = {
                            TokenStore.clear()
                            navigator.replaceAll(LoginScreen())
                        },
                        onAyuda = { navigator.push(StaffAyudaScreen(PerfilStaff.PICKER)) }
                    )
                    else -> ProximamentePlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        titulo = tituloTab
                    )
                }
            }
        }
    }
}

/**
 * Placeholder visual mientras las pantallas reales del perfil staff no estan construidas.
 * Cuando cada tab tenga su pantalla, se reemplaza por la pantalla real — el placeholder
 * queda como tab fallback si alguna no esta lista todavia.
 */
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
                    Icons.Filled.Inventory2,
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
                text = "Estamos preparando la herramienta del picker.\nVuelve en unos días.",
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
