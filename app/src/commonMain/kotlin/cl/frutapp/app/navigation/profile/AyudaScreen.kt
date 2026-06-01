package cl.frutapp.app.navigation.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Phone
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
import cl.frutapp.app.ui.openUrl
import cl.frutapp.app.ui.theme.FrutAppColors

/** Centro de ayuda con FAQ + canales de contacto (WhatsApp + email). */
class AyudaScreen : Screen {
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
                    Text(
                        "Ayuda",
                        color = FrutAppColors.Brand800,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
                    item { HeroAyuda() }

                    SECCIONES.forEach { seccion ->
                        item {
                            Text(
                                seccion.titulo,
                                color = FrutAppColors.Brand800,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 6.dp)
                            )
                        }
                        items(seccion.preguntas.size) { idx ->
                            FaqRow(pregunta = seccion.preguntas[idx])
                        }
                    }

                    item {
                        Text(
                            "¿No encontraste lo que buscabas?",
                            color = FrutAppColors.Brand800,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }
                    item {
                        ContactoCard(
                            icon = Icons.Filled.Phone,
                            titulo = "Hablar por WhatsApp",
                            detalle = "Lunes a sábado · 9:00 a 20:00",
                            onClick = {
                                // Número placeholder p/ demo; cambiar al real cuando exista la línea.
                                openUrl("https://wa.me/56912345678?text=Hola%20FrutApp%2C%20necesito%20ayuda%20con%20mi%20pedido.")
                            }
                        )
                    }
                    item {
                        ContactoCard(
                            icon = Icons.Filled.Email,
                            titulo = "Escribirnos un correo",
                            detalle = "hola@frutapp.cl · respondemos en 24h",
                            onClick = {
                                openUrl("mailto:hola@frutapp.cl?subject=Consulta%20desde%20la%20app")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroAyuda() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("¿En qué te ayudamos?", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                "Revisa nuestras preguntas frecuentes o escríbenos por WhatsApp o correo.",
                color = FrutAppColors.InkMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun FaqRow(pregunta: Pregunta) {
    var abierta by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .clickable { abierta = !abierta }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                pregunta.q,
                color = FrutAppColors.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = FrutAppColors.Brand600,
                modifier = Modifier.size(22.dp)
            )
        }
        AnimatedVisibility(visible = abierta) {
            Text(
                pregunta.a,
                color = FrutAppColors.InkMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ContactoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(FrutAppColors.Cream, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(titulo, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(detalle, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private data class Pregunta(val q: String, val a: String)
private data class Seccion(val titulo: String, val preguntas: List<Pregunta>)

private val SECCIONES = listOf(
    Seccion(
        "Pedidos y despacho",
        listOf(
            Pregunta(
                "¿En qué zonas hacen despacho?",
                "Hoy cubrimos la Región Metropolitana de Santiago. Estamos sumando comunas semana a semana — si vives fuera, escríbenos y te avisamos cuando lleguemos a tu zona."
            ),
            Pregunta(
                "¿Cuánto tarda en llegar mi pedido?",
                "Si pides antes de las 15:00, te llega el mismo día entre las 17:00 y las 21:00. Pedidos posteriores llegan al día siguiente en la franja que elijas."
            ),
            Pregunta(
                "¿Puedo elegir el horario de entrega?",
                "Sí. Al pagar, eliges entre 'Lo antes posible' o una franja específica (mañana, tarde o noche)."
            ),
            Pregunta(
                "¿Cómo hago seguimiento de mi pedido?",
                "En 'Pedidos' verás el estado en vivo: confirmado → preparando → en ruta → entregado. También te llega una notificación cuando el repartidor está cerca."
            )
        )
    ),
    Seccion(
        "Productos y calidad",
        listOf(
            Pregunta(
                "¿Qué pasa si un producto no llega fresco?",
                "Avísanos en menos de 24 horas tomando una foto desde la app: reponemos el producto sin costo o te devolvemos FrutCoins por el monto correspondiente."
            ),
            Pregunta(
                "¿De dónde vienen los productos?",
                "Trabajamos directo con feriantes y productores chilenos. Compramos cada mañana lo que sale ese día — no hay stock en bodega, todo es del día."
            ),
            Pregunta(
                "¿Cómo eligen los orgánicos?",
                "Solo marcamos como orgánicos productos de productores con certificación vigente o de huertos familiares que conocemos directamente."
            )
        )
    ),
    Seccion(
        "Pagos",
        listOf(
            Pregunta(
                "¿Qué medios de pago aceptan?",
                "Khipu (transferencia bancaria), Webpay (débito/crédito), y transferencia directa. Próximamente sumamos pago contra entrega."
            ),
            Pregunta(
                "¿Es seguro pagar en la app?",
                "Sí. Nunca guardamos los datos de tu tarjeta — el pago lo procesan Khipu y Transbank, dos plataformas reguladas por la CMF chilena."
            ),
            Pregunta(
                "¿Reciben boleta o factura?",
                "Por ahora emitimos boleta electrónica para cada compra. Factura para empresas la habilitamos en el próximo mes."
            )
        )
    ),
    Seccion(
        "FrutCoins y reciclaje",
        listOf(
            Pregunta(
                "¿Cómo gano FrutCoins?",
                "Sumas FrutCoins por cada compra (1 FrutCoin = $1), reciclando envases retornables (+30 por bolsa), dejando reseñas (+20) y refiriendo amigos (+100)."
            ),
            Pregunta(
                "¿Cómo uso mis FrutCoins?",
                "Al pagar, puedes elegir 'Usar mis FrutCoins' para cubrir el despacho o parte del total. 1 FrutCoin = $1 en descuento."
            ),
            Pregunta(
                "¿Cómo funciona el reciclaje?",
                "Devuelves bolsas, cajas o frascos limpios cuando llega tu próximo pedido. Por cada envase reciclado sumamos FrutCoins automáticamente a tu cuenta."
            )
        )
    ),
    Seccion(
        "Mi cuenta",
        listOf(
            Pregunta(
                "Olvidé mi contraseña, ¿qué hago?",
                "En la pantalla de inicio de sesión toca '¿Olvidaste tu contraseña?'. Te enviamos un código de 6 dígitos al correo para que la cambies."
            ),
            Pregunta(
                "¿Cómo cambio mi dirección de despacho?",
                "En 'Perfil' → 'Mis direcciones' puedes agregar, editar o eliminar las direcciones guardadas."
            ),
            Pregunta(
                "¿Cómo cierro mi sesión o mi cuenta?",
                "Cerrar sesión: 'Perfil' → 'Cerrar sesión'. Si quieres eliminar tu cuenta por completo, escríbenos por WhatsApp y lo gestionamos en 48 horas."
            )
        )
    )
)
