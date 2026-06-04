package cl.frutapp.app.navigation.repartidor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReportProblem
import cl.frutapp.app.ui.components.StaffAction

/**
 * Catalogo de acciones del menu de 3 puntos del repartidor. Distinto al del picker:
 * el repartidor esta en la calle, sus acciones contextuales son sobre la entrega
 * (llamar al cliente, cambiar direccion, reportar) no sobre el pedido en bodega
 * (asignar, pausar el armado, etc).
 *
 * Cada `onAction` es un lambda que la pantalla padre configura — asi una sola
 * funcion (`accionesRepartidor`) sirve para Detalle, EnCamino y Entrega; cada
 * pantalla decide que hacer con cada accion (navegar, mostrar dialogo, toast).
 */
fun accionesRepartidor(
    onPausar: () -> Unit,
    onReportar: () -> Unit,
    onCambiarDireccion: () -> Unit,
    onLlamarCliente: () -> Unit,
    onChatCliente: () -> Unit,
    onHistorial: () -> Unit,
    onCancelar: () -> Unit
): List<StaffAction> = listOf(
    StaffAction(
        titulo = "Llamar al cliente",
        detalle = "Contacto directo",
        icono = Icons.Filled.Phone,
        onClick = onLlamarCliente
    ),
    StaffAction(
        titulo = "Chatear con el cliente",
        detalle = "Mensajes via plataforma",
        icono = Icons.AutoMirrored.Filled.Chat,
        onClick = onChatCliente
    ),
    StaffAction(
        titulo = "Cambiar dirección",
        detalle = "Solicitar correccion al cliente",
        icono = Icons.Filled.EditLocationAlt,
        onClick = onCambiarDireccion
    ),
    StaffAction(
        titulo = "Pausar entrega",
        detalle = "Suspender temporalmente (5 min)",
        icono = Icons.Filled.PauseCircle,
        onClick = onPausar
    ),
    StaffAction(
        titulo = "Reportar problema",
        detalle = "Escalar a soporte",
        icono = Icons.Filled.ReportProblem,
        onClick = onReportar
    ),
    StaffAction(
        titulo = "Ver historial",
        detalle = "Cambios e intentos previos",
        icono = Icons.Filled.History,
        onClick = onHistorial
    ),
    StaffAction(
        titulo = "Cancelar entrega",
        detalle = "Marcar como no entregada (pide motivo)",
        icono = Icons.Filled.Cancel,
        destructiva = true,
        onClick = onCancelar
    )
)
