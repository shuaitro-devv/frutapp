package cl.frutapp.app.navigation.recycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.HuellaVerdeStore
import cl.frutapp.app.data.ModoRetiro
import cl.frutapp.app.data.ReciclaStore
import cl.frutapp.app.data.RetiroHistorial
import cl.frutapp.app.data.StreakStore
import cl.frutapp.app.data.TipoReciclaje
import cl.frutapp.app.navigation.rewards.HuellaVerdeScreen
import cl.frutapp.app.ui.components.FrutBottomNav
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTab
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Recicla ampliado: hero + caja FrutApp loop + cómo funciona + 5 tipos + 3 modos + historial
 * + lead capture + link a "Saber más". Todo dummy: el flujo real depende del piloto comercial.
 */
class ReciclaScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var tiposSel by remember { mutableStateOf(setOf<String>()) }
        var modoSel by remember { mutableStateOf<String?>(null) }

        val coinsEstimadas = tiposSel.size * 30 // estimación dummy

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopBar(
                    onBack = { navigator.pop() },
                    onSaberMas = { navigator.push(SobreReciclajeScreen()) }
                )

                LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 90.dp)) {
                    item { MiniHero(onHuella = { navigator.push(HuellaVerdeScreen()) }, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }

                    if (ReciclaStore.cajasFrutAppPendientes > 0) {
                        item { CajaLoopCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) }
                    }

                    item {
                        ComoFuncionaSection(
                            onSaberMas = { navigator.push(SobreReciclajeScreen()) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                        )
                    }

                    item { SectionTitle("¿Qué reciclas hoy?", Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)) }
                    item {
                        TiposGrid(
                            tipos = ReciclaStore.tipos,
                            seleccionados = tiposSel,
                            onToggle = { codigo ->
                                tiposSel = if (codigo in tiposSel) tiposSel - codigo else tiposSel + codigo
                            },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }

                    item { SectionTitle("¿Cómo te lo retiramos?", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp)) }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ReciclaStore.modos.forEach { modo ->
                                ModoRow(modo = modo, seleccionado = modoSel == modo.codigo, onClick = { modoSel = modo.codigo })
                            }
                        }
                    }

                    item { SectionTitle("Tus retiros", Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp)) }
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ReciclaStore.historial.take(4).forEach { HistorialRow(it) }
                        }
                    }

                    item { LeadCaptureCard(modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) }
                }

                // CTA fijo abajo + badge piloto
                Box(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        if (tiposSel.isNotEmpty()) {
                            Text(
                                "Estimado: +$coinsEstimadas FrutCoins",
                                color = FrutAppColors.Brand600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        FrutButtonPrimary(
                            text = "Agendar reciclaje",
                            enabled = tiposSel.isNotEmpty() && modoSel != null,
                            onClick = {
                                val nombreTipos = ReciclaStore.tipos.filter { it.codigo in tiposSel }.joinToString(", ") { it.nombre.lowercase() }
                                ReciclaStore.historial.add(0, RetiroHistorial(
                                    tipo = nombreTipos.replaceFirstChar { it.uppercase() },
                                    cantidad = "${tiposSel.size} tipo${if (tiposSel.size != 1) "s" else ""}",
                                    coinsGanadas = coinsEstimadas,
                                    fechaRelativa = "ahora"
                                ))
                                HuellaVerdeStore.sumarReciclaje(gramos = 200, coins = coinsEstimadas)
                                StreakStore.sumarDia()
                                showToast("¡Agendado! Te avisaremos cuando pasemos a buscarlo")
                                tiposSel = emptySet()
                                modoSel = null
                            }
                        )
                    }
                    // Badge PILOTO arriba a la derecha del CTA — honestidad sin frenar
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 20.dp)
                            .background(FrutAppColors.AmberSoft, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PILOTO", color = FrutAppColors.AmberCoin, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                FrutBottomNav(selected = FrutTab.INICIO, onSelect = { navigator.popUntilRoot() })
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit, onSaberMas: () -> Unit) {
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
        Text("Recicla", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp))
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onSaberMas),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Info, contentDescription = "Saber más", tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MiniHero(onHuella: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand400)), RoundedCornerShape(18.dp))
            .clickable(onClick = onHuella)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Park, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("Mi huella verde", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "${HuellaVerdeStore.reciclajes} reciclajes · 🔥 ${StreakStore.dias} días",
                color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CajaLoopCard(modifier: Modifier = Modifier) {
    // El highlight más fuerte: loop cerrado FrutApp (caja que vino con un pedido vuelve).
    val cajas = ReciclaStore.cajasFrutAppPendientes
    Row(
        modifier = modifier.fillMaxWidth()
            .background(FrutAppColors.AmberSoft, RoundedCornerShape(16.dp))
            .border(1.5.dp, FrutAppColors.AmberCoin.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { showToast("Lo agendamos con tu próximo pedido · +30 coins") }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📦", fontSize = 30.sp)
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                "Tienes $cajas caja${if (cajas != 1) "s" else ""} FrutApp por devolver",
                color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Text(
                "La retiramos con tu próximo pedido · +30 coins",
                color = FrutAppColors.AmberCoin, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FrutAppColors.AmberCoin, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ComoFuncionaSection(onSaberMas: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Cómo funciona", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Saber más →",
                color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onSaberMas)
            )
        }
        Spacer(Modifier.height(10.dp))
        val pasos = listOf(
            "1" to "Eliges qué reciclar",
            "2" to "Eliges cómo lo retiramos",
            "3" to "Validamos y enviamos a recicladores certificados",
            "4" to "Ganas FrutCoins"
        )
        pasos.forEach { (n, txt) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(22.dp).background(FrutAppColors.Brand400, CircleShape), contentAlignment = Alignment.Center) {
                    Text(n, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(txt, color = FrutAppColors.Ink, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun TiposGrid(tipos: List<TipoReciclaje>, seleccionados: Set<String>, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    // Grid 2 columnas (2 filas de 2 + 1 fila de 1 quedará desbalanceado para 5).
    // Usamos 3 columnas: 5 = 3 + 2 → segunda fila tiene gap natural.
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tipos.chunked(3).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { tipo ->
                    TipoCard(tipo = tipo, seleccionado = tipo.codigo in seleccionados, onClick = { onToggle(tipo.codigo) }, modifier = Modifier.weight(1f))
                }
                if (fila.size < 3) repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TipoCard(tipo: TipoReciclaje, seleccionado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(98.dp)
            .background(if (seleccionado) FrutAppColors.Brand400 else FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .border(if (seleccionado) 0.dp else 1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(tipo.emoji, fontSize = 26.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            tipo.nombre,
            color = if (seleccionado) Color.White else FrutAppColors.Brand800,
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            tipo.coinsLabel,
            color = if (seleccionado) Color.White.copy(alpha = 0.85f) else FrutAppColors.Brand600,
            fontSize = 10.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun ModoRow(modo: ModoRetiro, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(if (seleccionado) 2.dp else 1.dp, if (seleccionado) FrutAppColors.Brand400 else FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(FrutAppColors.Brand50, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(modo.emoji, fontSize = 22.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(modo.nombre, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(modo.detalle, color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 1.dp))
            Text(modo.costo, color = FrutAppColors.Brand600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
        }
        // Radio visual
        Box(
            modifier = Modifier.size(22.dp).background(Color.White, CircleShape)
                .border(2.dp, if (seleccionado) FrutAppColors.Brand400 else FrutAppColors.Brand100, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (seleccionado) Box(modifier = Modifier.size(10.dp).background(FrutAppColors.Brand400, CircleShape))
        }
    }
}

@Composable
private fun HistorialRow(item: RetiroHistorial) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.tipo} · ${item.cantidad}", color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(item.fechaRelativa, color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
        Text("+${item.coinsGanadas} coins", color = FrutAppColors.AmberCoin, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LeadCaptureCard(modifier: Modifier = Modifier) {
    var email by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }
    var enviado by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth()
            .background(FrutAppColors.Cream, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("🌱 Próximamente en tu comuna", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
            "Andamos en piloto en ${ReciclaStore.comunasPiloto.joinToString(", ")}. ¿No estás en estas comunas? Déjanos saber.",
            color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
        )

        if (enviado) {
            Spacer(Modifier.height(10.dp))
            Text(
                "¡Listo! Te avisaremos cuando llegue a tu comuna 🌿",
                color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
        } else {
            Spacer(Modifier.height(12.dp))
            MiniField(value = email, onValueChange = { email = it }, placeholder = "Tu correo")
            Spacer(Modifier.height(8.dp))
            MiniField(value = comuna, onValueChange = { comuna = it }, placeholder = "Tu comuna")
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(
                        if (email.isNotBlank() && comuna.isNotBlank()) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = email.isNotBlank() && comuna.isNotBlank()) {
                        enviado = true
                        showToast("¡Gracias! Te avisaremos")
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Avísame cuando llegue",
                    color = if (email.isNotBlank() && comuna.isNotBlank()) Color.White else FrutAppColors.InkSoft,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MiniField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        modifier = Modifier.fillMaxWidth().height(44.dp)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) Text(placeholder, color = FrutAppColors.InkSoft, fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 13.sp),
            cursorBrush = SolidColor(FrutAppColors.Brand400),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
