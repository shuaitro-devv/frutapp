package cl.frutapp.app.navigation.canastas

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.Canasta
import cl.frutapp.app.data.CanastaStore
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors

/** Mis canastas: lista de canastas creadas por el usuario + sugeridas FrutApp + crear nueva. */
class MisCanastasScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(onBack = { navigator.pop() })

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    if (CanastaStore.items.isEmpty()) {
                        EmptyState()
                    } else {
                        SectionTitle("Mis canastas", Modifier.padding(top = 8.dp, bottom = 8.dp))
                        CanastaStore.items.forEach { c ->
                            CanastaRow(c, onClick = { navigator.push(CanastaDetailScreen(c.id)) })
                        }
                    }

                    SectionTitle("Canastas FrutApp", Modifier.padding(top = 24.dp, bottom = 8.dp))
                    Text(
                        "Listas pre-armadas para que partas rápido. Tócalas para ver y comprarlas.",
                        color = FrutAppColors.InkMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    CanastaStore.templates.forEach { t ->
                        CanastaRow(t, onClick = { navigator.push(CanastaDetailScreen(t.id)) })
                    }

                    Spacer(Modifier.height(80.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 20.dp, vertical = 14.dp)) {
                    FrutButtonPrimary(
                        text = "Nueva canasta",
                        leadingIcon = Icons.Filled.Add,
                        onClick = { navigator.push(NuevaCanastaScreen()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
        }
        Text("Mis canastas", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun CanastaRow(c: Canasta, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp).background(Color.White, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(c.emoji, fontSize = 22.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.nombre, color = FrutAppColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (c.esTemplate) {
                    Box(modifier = Modifier.padding(start = 6.dp).background(FrutAppColors.Brand400, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text("FrutApp", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (c.recordatorioMensual) {
                    Text(" · 🔔", fontSize = 11.sp)
                }
            }
            Text(
                "${c.cantidadProductos} producto${if (c.cantidadProductos != 1) "s" else ""} · ~${formatClp(c.totalEstimado)}",
                color = FrutAppColors.InkSoft, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🧺", fontSize = 36.sp)
        }
        Text(
            "Aún no tienes canastas",
            color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            "Empieza con una canasta sugerida o créala tú mismo.",
            color = FrutAppColors.InkMuted, fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
