package cl.frutapp.app.navigation.recycle

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.ReciclaStore
import cl.frutapp.app.ui.theme.FrutAppColors

private data class FaqItem(val pregunta: String, val respuesta: String)

private val FAQ = listOf(
    FaqItem(
        "¿Qué pasa con cada material?",
        "• Plástico → empresas recicladoras (PET, HDPE) que vuelven a fabricar envases.\n" +
            "• Papel/cartón → fábricas de papel reciclado (la misma caja que recibiste vuelve al ciclo).\n" +
            "• Vidrio → fundición y nuevo envasado.\n" +
            "• Orgánico → compost industrial / huertas urbanas.\n" +
            "• Pilas → centros especializados (Recicla Tus Pilas u otros). NO van al tarro común."
    ),
    FaqItem(
        "¿Por qué las pilas son distintas?",
        "Las pilas tienen metales pesados (mercurio, cadmio, litio) que contaminan napas si terminan en basura común. Por eso requieren centros autorizados y por eso pagan más FrutCoins en el momento del retiro."
    ),
    FaqItem(
        "¿Cómo se acreditan las FrutCoins?",
        "Cuando nuestro centro recibe y valida el material, se acreditan automáticamente a tu cuenta (típicamente en 24–48 horas). Si la cantidad real difiere mucho de lo declarado, te avisamos antes de acreditar."
    ),
    FaqItem(
        "¿Cuánto cuesta el retiro a puerta?",
        "Depende del peso/volumen y la comuna. Está pensado para ser canjeable con FrutCoins (no tienes que pagar plata si las acumulaste reciclando). Los otros dos modos (punto de acopio y con tu pedido) son gratis."
    ),
    FaqItem(
        "¿Y la caja en que me llega mi pedido?",
        "Es nuestro \"loop cerrado\": devuélvela con tu próximo pedido. Cuando entreguemos lo nuevo, retiramos la caja vacía. Suma +30 coins y la caja vuelve al ciclo, no termina en la basura."
    ),
    FaqItem(
        "¿Qué pasa si me equivoco de tipo de material?",
        "No pasa nada — en nuestro centro separamos. Si declaraste \"plástico\" y trajiste cartón, lo procesamos igual con los coins correspondientes a lo que efectivamente trajiste."
    ),
    FaqItem(
        "¿Es seguro retirar pilas?",
        "Sí. Nuestro personal está capacitado y usa contenedores especiales sellados. Las pilas viajan separadas del resto de materiales y van directo a un centro certificado (referencia: Recicla Tus Pilas)."
    ),
    FaqItem(
        "¿En qué comunas opera hoy?",
        "Piloto en ${ReciclaStore.comunasPiloto.joinToString(", ")}. Si no estás en estas comunas, déjanos tu correo en la pantalla anterior y te avisamos en cuanto lleguemos."
    )
)

/** Detalle del proceso de reciclaje + FAQ + aliados + disclaimer del piloto. */
class SobreReciclajeScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

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
                    Text("Sobre el reciclaje", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    IntroCard()

                    Spacer(Modifier.height(20.dp))
                    Text("Preguntas frecuentes", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    FAQ.forEach { item -> FaqRow(item) }

                    Spacer(Modifier.height(22.dp))
                    AliadosSection()

                    Spacer(Modifier.height(22.dp))
                    DisclaimerCard()
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("Reciclamos juntos", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "Tú juntas, nosotros retiramos, los recicladores certificados cierran el ciclo. Y de paso ganas FrutCoins.",
            color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(12.dp))
        val pasos = listOf(
            Triple("1", "Eliges qué reciclar", "5 tipos: plástico, papel, vidrio, orgánico, pilas."),
            Triple("2", "Eliges cómo lo retiramos", "Puerta · Punto de acopio · Con tu próximo pedido."),
            Triple("3", "Validamos y separamos", "En nuestro centro se separa y va a recicladores certificados."),
            Triple("4", "Ganas FrutCoins", "Se acreditan en 24-48h. Pilas pagan más por su riesgo.")
        )
        pasos.forEach { (n, t, d) ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(26.dp).background(FrutAppColors.Brand400, CircleShape), contentAlignment = Alignment.Center) {
                    Text(n, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(t, color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(d, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun FaqRow(item: FaqItem) {
    var expandido by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(12.dp))
            .clickable { expandido = !expandido }
            .padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.pregunta, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(
                if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = FrutAppColors.Brand600,
                modifier = Modifier.size(20.dp)
            )
        }
        AnimatedVisibility(visible = expandido) {
            Text(
                item.respuesta,
                color = FrutAppColors.InkMuted, fontSize = 12.sp, lineHeight = 18.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AliadosSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Aliados (referencia)", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            "Empresas con las que conversamos para el piloto. El acuerdo comercial está en curso.",
            color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
        )
        // Chips con los aliados
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ReciclaStore.aliadosReferencia.chunked(2).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fila.forEach { aliado ->
                        Box(
                            modifier = Modifier
                                .background(FrutAppColors.Brand100, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(aliado, color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerCard() {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(FrutAppColors.Cream, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🛈", fontSize = 18.sp)
            Text(
                "Este módulo está en construcción",
                color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Text(
            "Estamos cerrando alianzas con recicladores certificados y empresas de transporte para operar a escala. " +
                "Por ahora funciona como piloto en algunas comunas. Si quieres ser parte del piloto cuando llegue a la tuya, " +
                "déjanos tu correo en la pantalla anterior.",
            color = FrutAppColors.InkMuted, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
