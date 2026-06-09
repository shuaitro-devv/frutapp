package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/**
 * Notificacion del inbox. Source of truth en `notification` (BD); el cliente
 * la consume via `GET /v1/notifications`. `data` queda como JSON serializado
 * (string) que el cliente puede deserializar segun [type] — evita versionado
 * de columnas rigidas y permite agregar tipos sin migration nueva.
 *
 * Tipos validos hoy (catalogo en codigo): PEDIDO, COINS, RECICLA, RACHA, PROMO.
 */
@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val data: String? = null,
    val createdAt: String,
    val readAt: String? = null
) {
    val leida: Boolean get() = readAt != null
}

@Serializable
data class NotificationsResponse(
    val items: List<NotificationDto>,
    val unreadCount: Int
)
