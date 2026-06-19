package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Mensaje del chat tal como lo ve la app (REST y WS). */
@Serializable
data class ChatMensajeDto(
    val id: String,
    val orderId: String,
    val autorUserId: String,
    val autorRol: String,        // cliente / picker / repartidor
    val destinatarioRol: String, // picker / repartidor
    val cuerpo: String,
    /** URL presignada de la imagen adjunta, o null si el mensaje es solo texto.
     *  El backend la firma cada vez que devuelve el mensaje (no se persiste). */
    val imagenUrl: String? = null,
    val leidoEn: String?,        // ISO o null si no fue leido
    val createdAt: String,       // ISO
)

/** Body (legacy JSON) para POST /v1/orders/{id}/chat. El endpoint ahora prefiere
 *  multipart para soportar imagen adjunta; este DTO queda para mantener tests
 *  o clientes que aun mandan JSON. */
@Serializable
data class EnviarMensajeRequest(
    /** picker o repartidor — a quien le habla el cliente. Cuando el autor es
     *  staff (picker o repartidor), el destinatario siempre es "cliente" y
     *  este campo se ignora (el backend lo setea). */
    val destinatarioRol: String,
    val cuerpo: String,
)

/**
 * Frame que se intercambia por el WebSocket de chat. Es un sobre con `type`
 * + payload opcional por tipo:
 *
 *  - **mensaje**: nuevo mensaje en el chat (push del server a todas las
 *    sesiones del pedido). [mensaje] tiene el DTO.
 *  - **typing**: alguien esta escribiendo. El cliente lo envia mientras
 *    tipea (throttled ~1.5s); el server lo rebroadcastea a las OTRAS
 *    sesiones del pedido (no al autor). [typingRol] y [typingUserId]
 *    identifican quien escribe.
 *  - **leido**: alguien marco como leidos los mensajes destinados a su
 *    rol. El server lo rebroadcastea para que el autor vea su tick azul
 *    en tiempo real sin recargar. [leidoEnRol] es el rol del que leyo
 *    (= destinatario_rol de los mensajes afectados); [leidoEn] es el
 *    timestamp ISO del marcado.
 *
 * Diseno con default en [type]="mensaje" para que APKs viejas que solo
 * miran `mensaje` no se rompan al recibir frames typing/leido (los
 * ignoran porque `mensaje` viene null).
 */
@Serializable
data class WsChatPush(
    val type: String = TYPE_MENSAJE,
    val mensaje: ChatMensajeDto? = null,
    val typingRol: String? = null,
    val typingUserId: String? = null,
    val leidoEnRol: String? = null,
    val leidoEn: String? = null,
) {
    companion object {
        const val TYPE_MENSAJE = "mensaje"
        const val TYPE_TYPING = "typing"
        const val TYPE_LEIDO = "leido"
    }
}
