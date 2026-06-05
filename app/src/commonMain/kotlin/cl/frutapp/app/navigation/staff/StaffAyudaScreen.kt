package cl.frutapp.app.navigation.staff

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.AyudaContacto
import cl.frutapp.app.ui.components.AyudaPregunta
import cl.frutapp.app.ui.components.AyudaScaffold
import cl.frutapp.app.ui.components.AyudaSeccion
import cl.frutapp.app.ui.openUrl

/** Perfil staff que abre Ayuda — cambia las preguntas frecuentes y el numero de soporte. */
enum class PerfilStaff { PICKER, REPARTIDOR }

/**
 * Centro de ayuda para perfiles staff (picker / repartidor). Misma UI que la AyudaScreen
 * del cliente (reusa AyudaScaffold), distinto contenido: aqui las FAQ son operativas
 * (que hacer si el cliente no contesta, como reportar un producto malo, etc) y el
 * canal de WhatsApp apunta al soporte interno, no al de clientes.
 */
class StaffAyudaScreen(private val perfil: PerfilStaff) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val secciones = when (perfil) {
            PerfilStaff.PICKER -> SECCIONES_PICKER
            PerfilStaff.REPARTIDOR -> SECCIONES_REPARTIDOR
        }
        AyudaScaffold(
            heroTitulo = "Centro de ayuda",
            heroSubtitulo = "Consulta el protocolo o contacta a soporte interno cuando necesites apoyo en terreno.",
            secciones = secciones,
            contactos = listOf(
                AyudaContacto(
                    icon = Icons.Filled.SupportAgent,
                    titulo = "Soporte FrutApp (WhatsApp)",
                    detalle = "Operativa · 7:00 a 22:00 todos los días",
                    onClick = {
                        openUrl("https://wa.me/56987654321?text=Hola%20soporte%2C%20necesito%20ayuda%20con%20una%20entrega.")
                    }
                ),
                AyudaContacto(
                    icon = Icons.Filled.Phone,
                    titulo = "Llamar al supervisor",
                    detalle = "Solo para emergencias en ruta",
                    onClick = { openUrl("tel:+56987654322") }
                )
            ),
            contactosTitulo = "¿Necesitas escalar el caso?",
            onBack = { navigator.pop() }
        )
    }
}

private val SECCIONES_PICKER = listOf(
    AyudaSeccion(
        "Preparación de pedidos",
        listOf(
            AyudaPregunta(
                "¿Qué hago si no encuentro un producto?",
                "Pulsa el menú de 3 puntos del item y marca 'Faltante' o 'Sustituir'. Si dudas, sustituye con un producto similar de igual o mejor calidad — el sistema avisa al cliente y confirma el cambio."
            ),
            AyudaPregunta(
                "¿Cómo registro un producto con peso variable?",
                "Pesa en balanza, ingresa el peso real y confirma. La diferencia se ajusta automáticamente en el cobro final o se devuelve al cliente como FrutCoins."
            ),
            AyudaPregunta(
                "¿Qué pasa si llega menos cantidad de la pedida?",
                "Marca 'Reducido' e ingresa la cantidad disponible. El cliente recibe notificación y se le descuenta del total — nunca despaches menos sin marcarlo."
            ),
            AyudaPregunta(
                "¿Puedo modificar el pedido después de enviarlo a despacho?",
                "No directamente. Si detectas un error contacta a soporte por WhatsApp antes de que el repartidor inicie el retiro."
            )
        )
    ),
    AyudaSeccion(
        "Calidad y devoluciones",
        listOf(
            AyudaPregunta(
                "¿Cómo identifico un producto en mal estado?",
                "Revisa frescura, color, firmeza y olor antes de empacar. Cualquier producto que tú no llevarías a tu casa no debe ir en un pedido. Marca 'Faltante' y sustituye."
            ),
            AyudaPregunta(
                "¿Qué hago con productos rechazados en bodega?",
                "Sepáralos en la caja roja de devoluciones — el supervisor los retira al final del turno y deja registro para el proveedor."
            )
        )
    ),
    AyudaSeccion(
        "Mi turno",
        listOf(
            AyudaPregunta(
                "¿Cómo me pagan los turnos?",
                "El pago se procesa quincenalmente según turnos confirmados en la app. Si ves diferencias, contacta a soporte con captura del calendario."
            ),
            AyudaPregunta(
                "¿Cómo reporto una falla del sistema?",
                "Si la app se traba o no carga un pedido, contacta soporte por WhatsApp con el ID del pedido. No reinicies la app sin confirmar — puedes perder el progreso del picklist."
            )
        )
    )
)

private val SECCIONES_REPARTIDOR = listOf(
    AyudaSeccion(
        "Antes del retiro",
        listOf(
            AyudaPregunta(
                "¿Cómo confirmo que retiré el pedido correcto?",
                "En bodega revisa que el ID del pedido coincida con tu app antes de salir. El picker firma el handoff y queda registrado con foto."
            ),
            AyudaPregunta(
                "¿Qué hago si faltan items al momento del retiro?",
                "No salgas hasta que el picker resuelva o registre la incidencia. Tu responsabilidad inicia cuando aceptas el retiro."
            )
        )
    ),
    AyudaSeccion(
        "En ruta",
        listOf(
            AyudaPregunta(
                "El cliente no contesta el timbre o el teléfono, ¿qué hago?",
                "Espera 5 minutos. Llama 2 veces, envía mensaje por chat de la app y deja foto en la puerta solo si el cliente lo autorizó previamente. Si nada de eso resulta, reporta 'Cliente ausente' desde el menú de la entrega."
            ),
            AyudaPregunta(
                "La dirección está mal o no existe, ¿qué hago?",
                "Usa la opción 'Cambiar dirección' del menú para solicitar nueva ubicación al cliente. Si no responde en 10 minutos, marca 'Dirección incorrecta' y vuelve a bodega — no improvises destinos."
            ),
            AyudaPregunta(
                "¿Puedo pausar entregas si estoy con problemas técnicos del vehículo?",
                "Sí, usa 'Pausar entrega' del menú. El sistema notifica al cliente y reasigna si es necesario. Reporta luego por WhatsApp el motivo."
            )
        )
    ),
    AyudaSeccion(
        "Incidencias y devoluciones",
        listOf(
            AyudaPregunta(
                "El cliente reclama un producto en mal estado al entregar, ¿qué hago?",
                "Toma foto del producto, registra la incidencia desde 'Reportar' y deja la entrega como completada. El reembolso o reposición lo gestiona soporte después, no en terreno."
            ),
            AyudaPregunta(
                "¿Qué hago con un pedido rechazado por el cliente?",
                "Vuelve con el pedido a bodega y registra 'Devuelto' al cierre. Nunca dejes producto fuera sin confirmación del cliente."
            )
        )
    ),
    AyudaSeccion(
        "Mi turno",
        listOf(
            AyudaPregunta(
                "¿Cómo se calculan mis ganancias por entrega?",
                "Cada entrega completada acumula al pago de la quincena. Las propinas se abonan automáticamente al cierre del día."
            ),
            AyudaPregunta(
                "¿Qué hago si tengo accidente o problema en ruta?",
                "Primero tu seguridad: detén la entrega, llama al supervisor. Luego registra en la app y conserva fotos para el seguro."
            )
        )
    )
)
