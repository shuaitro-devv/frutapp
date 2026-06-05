package cl.frutapp.app.navigation.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.AyudaContacto
import cl.frutapp.app.ui.components.AyudaPregunta
import cl.frutapp.app.ui.components.AyudaScaffold
import cl.frutapp.app.ui.components.AyudaSeccion
import cl.frutapp.app.ui.openUrl

/** Centro de ayuda del cliente: FAQ de pedidos/productos/pagos/FrutCoins + WhatsApp + email. */
class AyudaScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AyudaScaffold(
            heroTitulo = "¿En qué te ayudamos?",
            heroSubtitulo = "Revisa nuestras preguntas frecuentes o escríbenos por WhatsApp o correo.",
            secciones = SECCIONES_CLIENTE,
            contactos = listOf(
                AyudaContacto(
                    icon = Icons.Filled.Phone,
                    titulo = "Hablar por WhatsApp",
                    detalle = "Lunes a sábado · 9:00 a 20:00",
                    onClick = {
                        openUrl("https://wa.me/56912345678?text=Hola%20FrutApp%2C%20necesito%20ayuda%20con%20mi%20pedido.")
                    }
                ),
                AyudaContacto(
                    icon = Icons.Filled.Email,
                    titulo = "Escribirnos un correo",
                    detalle = "hola@frutapp.cl · respondemos en 24h",
                    onClick = { openUrl("mailto:hola@frutapp.cl?subject=Consulta%20desde%20la%20app") }
                )
            ),
            onBack = { navigator.pop() }
        )
    }
}

private val SECCIONES_CLIENTE = listOf(
    AyudaSeccion(
        "Pedidos y despacho",
        listOf(
            AyudaPregunta(
                "¿En qué zonas hacen despacho?",
                "Hoy cubrimos la Región Metropolitana de Santiago. Estamos sumando comunas semana a semana — si vives fuera, escríbenos y te avisamos cuando lleguemos a tu zona."
            ),
            AyudaPregunta(
                "¿Cuánto tarda en llegar mi pedido?",
                "Si pides antes de las 15:00, te llega el mismo día entre las 17:00 y las 21:00. Pedidos posteriores llegan al día siguiente en la franja que elijas."
            ),
            AyudaPregunta(
                "¿Puedo elegir el horario de entrega?",
                "Sí. Al pagar, eliges entre 'Lo antes posible' o una franja específica (mañana, tarde o noche)."
            ),
            AyudaPregunta(
                "¿Cómo hago seguimiento de mi pedido?",
                "En 'Pedidos' verás el estado en vivo: confirmado → preparando → en ruta → entregado. También te llega una notificación cuando el repartidor está cerca."
            )
        )
    ),
    AyudaSeccion(
        "Productos y calidad",
        listOf(
            AyudaPregunta(
                "¿Qué pasa si un producto no llega fresco?",
                "Avísanos en menos de 24 horas tomando una foto desde la app: reponemos el producto sin costo o te devolvemos FrutCoins por el monto correspondiente."
            ),
            AyudaPregunta(
                "¿De dónde vienen los productos?",
                "Trabajamos directo con feriantes y productores chilenos. Compramos cada mañana lo que sale ese día — no hay stock en bodega, todo es del día."
            ),
            AyudaPregunta(
                "¿Cómo eligen los orgánicos?",
                "Solo marcamos como orgánicos productos de productores con certificación vigente o de huertos familiares que conocemos directamente."
            )
        )
    ),
    AyudaSeccion(
        "Pagos",
        listOf(
            AyudaPregunta(
                "¿Qué medios de pago aceptan?",
                "Khipu (transferencia bancaria), Webpay (débito/crédito), y transferencia directa. Próximamente sumamos pago contra entrega."
            ),
            AyudaPregunta(
                "¿Es seguro pagar en la app?",
                "Sí. Nunca guardamos los datos de tu tarjeta — el pago lo procesan Khipu y Transbank, dos plataformas reguladas por la CMF chilena."
            ),
            AyudaPregunta(
                "¿Reciben boleta o factura?",
                "Por ahora emitimos boleta electrónica para cada compra. Factura para empresas la habilitamos en el próximo mes."
            )
        )
    ),
    AyudaSeccion(
        "FrutCoins y reciclaje",
        listOf(
            AyudaPregunta(
                "¿Cómo gano FrutCoins?",
                "Sumas FrutCoins por cada compra (1 FrutCoin = $1), reciclando envases retornables (+30 por bolsa), dejando reseñas (+20) y refiriendo amigos (+100)."
            ),
            AyudaPregunta(
                "¿Cómo uso mis FrutCoins?",
                "Al pagar, puedes elegir 'Usar mis FrutCoins' para cubrir el despacho o parte del total. 1 FrutCoin = $1 en descuento."
            ),
            AyudaPregunta(
                "¿Cómo funciona el reciclaje?",
                "Devuelves bolsas, cajas o frascos limpios cuando llega tu próximo pedido. Por cada envase reciclado sumamos FrutCoins automáticamente a tu cuenta."
            )
        )
    ),
    AyudaSeccion(
        "Mi cuenta",
        listOf(
            AyudaPregunta(
                "Olvidé mi contraseña, ¿qué hago?",
                "En la pantalla de inicio de sesión toca '¿Olvidaste tu contraseña?'. Te enviamos un código de 6 dígitos al correo para que la cambies."
            ),
            AyudaPregunta(
                "¿Cómo cambio mi dirección de despacho?",
                "En 'Perfil' → 'Mis direcciones' puedes agregar, editar o eliminar las direcciones guardadas."
            ),
            AyudaPregunta(
                "¿Cómo cierro mi sesión o mi cuenta?",
                "Cerrar sesión: 'Perfil' → 'Cerrar sesión'. Si quieres eliminar tu cuenta por completo, escríbenos por WhatsApp y lo gestionamos en 48 horas."
            )
        )
    )
)
