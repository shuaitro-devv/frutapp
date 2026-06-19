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
 * Frame que el server empuja por el WebSocket cuando hay un mensaje nuevo
 * del pedido. El cliente NO envia frames — el WS es solo push (lectura).
 * Para enviar mensajes o marcar leidos se usa REST (idempotente, auditable).
 */
@Serializable
data class WsChatPush(val mensaje: ChatMensajeDto)
