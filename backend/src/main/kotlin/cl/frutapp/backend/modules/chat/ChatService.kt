package cl.frutapp.backend.modules.chat

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.backend.modules.notifications.NotificationDispatcher
import cl.frutapp.shared.dto.ChatMensajeDto
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Logica del chat in-app:
 *
 *  - **Ownership**: cliente del pedido o staff asignado (picker/repartidor).
 *    Cualquier otro user → 404. Un picker que no es el asignado a este pedido
 *    NO puede ver ni enviar mensajes (anti-snooping).
 *
 *  - **Direccion del mensaje**:
 *      - cliente -> destinatario_rol (picker o repartidor segun a quien le habla)
 *      - picker  -> cliente (siempre)
 *      - repartidor -> cliente (siempre)
 *    Si el cliente envia a un destinatario no asignado todavia (ej. picker
 *    cuando el pedido aun no fue tomado), rechazamos con ValidationException.
 *
 *  - **Realtime**: tras persistir el mensaje, broadcast a las conexiones WS
 *    de ese pedido. Si el destinatario NO esta conectado (no hay conexiones
 *    suyas en el Hub), mandamos FCM data-only — la app lo recibe y muestra
 *    push o badge. NO mandamos push al autor.
 */
class ChatService(
    private val repo: ChatRepository,
    private val hub: ChatHub,
    private val notifications: NotificationDispatcher,
) {

    /** Envia un mensaje y lo persiste + broadcast + push al destinatario. */
    suspend fun enviar(
        autorUserId: UUID,
        autorRol: String,
        orderId: UUID,
        destinatarioRolPedido: String,
        cuerpo: String,
    ): ChatMensajeDto {
        val texto = cuerpo.trim()
        if (texto.isEmpty()) throw ValidationException("El mensaje no puede estar vacio.")
        if (texto.length > 1000) throw ValidationException("El mensaje supera el limite de 1000 caracteres.")
        if (autorRol !in ChatRol.VALIDOS) throw ValidationException("Rol invalido.")

        // Resolver el destinatario: si el autor es staff, siempre es cliente;
        // si el autor es cliente, depende del destinatario_rol que pidio.
        val destinatarioReal = when (autorRol) {
            ChatRol.PICKER, ChatRol.REPARTIDOR -> ChatRol.CLIENTE
            ChatRol.CLIENTE -> {
                if (destinatarioRolPedido !in setOf(ChatRol.PICKER, ChatRol.REPARTIDOR)) {
                    throw ValidationException("Destinatario invalido: solo picker o repartidor.")
                }
                destinatarioRolPedido
            }
            else -> throw ValidationException("Rol invalido.")
        }

        // Ownership + validez del destinatario asignado (no podes hablarle al
        // picker si el pedido todavia no fue tomado).
        val tipo = resolverRolUsuarioEnPedido(orderId, autorUserId)
            ?: throw NotFoundException("Pedido no encontrado.")
        if (tipo != autorRol) {
            // El cliente del pedido NO puede mandar como picker, etc.
            throw ValidationException("No tenes ese rol en este pedido.")
        }
        if (autorRol == ChatRol.CLIENTE) {
            val asignado = quienEsta(orderId, destinatarioReal)
            if (asignado == null) {
                throw ValidationException(
                    when (destinatarioReal) {
                        ChatRol.PICKER -> "Tu pedido todavia no fue tomado por un picker."
                        ChatRol.REPARTIDOR -> "Tu pedido todavia no tiene repartidor asignado."
                        else -> "Destinatario no asignado."
                    }
                )
            }
        }

        val row = repo.insert(orderId, autorUserId, autorRol, destinatarioReal, texto)
        val dto = row.toDto()

        // Broadcast realtime a las conexiones del pedido.
        hub.broadcast(orderId, dto)

        // Push FCM al destinatario si no esta conectado. Si esta conectado,
        // ya recibio el frame por el WS — un push duplicado da ruido.
        val destinatarioUserId = quienEsta(orderId, destinatarioReal)
        if (destinatarioUserId != null && hub.conexionesDe(orderId) == 0) {
            notifications.onChatMensaje(
                orderId = orderId,
                destinatarioUserId = destinatarioUserId,
                autorRol = autorRol,
                cuerpoBreve = texto.take(80),
            )
        }
        return dto
    }

    /** Historial del pedido para el usuario. Valida ownership; devuelve
     *  cronologico ASC. */
    suspend fun historial(
        userId: UUID,
        orderId: UUID,
        desde: Instant?,
    ): List<ChatMensajeDto> {
        resolverRolUsuarioEnPedido(orderId, userId)
            ?: throw NotFoundException("Pedido no encontrado.")
        return repo.historial(orderId, desde).map { it.toDto() }
    }

    /** El destinatario marca todos sus mensajes como leidos. Devuelve la
     *  cantidad afectada. */
    suspend fun marcarLeidos(userId: UUID, orderId: UUID): Int {
        val rol = resolverRolUsuarioEnPedido(orderId, userId)
            ?: throw NotFoundException("Pedido no encontrado.")
        // El cliente marca los mensajes destinados a "cliente"; staff marca
        // los destinados a su rol.
        return repo.marcarLeidos(orderId, rol)
    }

    /** True si el [userId] participa en el chat de [orderId]. Tambien usado
     *  por el endpoint WS para autorizar la conexion. */
    suspend fun rolEnPedido(orderId: UUID, userId: UUID): String? =
        resolverRolUsuarioEnPedido(orderId, userId)

    /** Devuelve el rol del [userId] en este pedido, o null si no participa.
     *  Usa los campos de OrdersTable (user_id, assigned_picker_id,
     *  assigned_repartidor_id). */
    private suspend fun resolverRolUsuarioEnPedido(orderId: UUID, userId: UUID): String? = dbQuery {
        val row = OrdersTable.selectAll().where { OrdersTable.id eq orderId }.singleOrNull()
            ?: return@dbQuery null
        when (userId) {
            row[OrdersTable.userId] -> ChatRol.CLIENTE
            row[OrdersTable.assignedPickerId] -> ChatRol.PICKER
            row[OrdersTable.assignedRepartidorId] -> ChatRol.REPARTIDOR
            else -> null
        }
    }

    /** UUID del usuario asignado al rol en este pedido, o null si no hay. */
    private suspend fun quienEsta(orderId: UUID, rol: String): UUID? = dbQuery {
        val row = OrdersTable.selectAll().where { OrdersTable.id eq orderId }.singleOrNull()
            ?: return@dbQuery null
        when (rol) {
            ChatRol.CLIENTE -> row[OrdersTable.userId]
            ChatRol.PICKER -> row[OrdersTable.assignedPickerId]
            ChatRol.REPARTIDOR -> row[OrdersTable.assignedRepartidorId]
            else -> null
        }
    }

    private fun ChatRepository.MensajeRow.toDto() = ChatMensajeDto(
        id = id.toString(),
        orderId = orderId.toString(),
        autorUserId = autorUserId.toString(),
        autorRol = autorRol,
        destinatarioRol = destinatarioRol,
        cuerpo = cuerpo,
        leidoEn = leidoEn?.toString(),
        createdAt = createdAt.toString(),
    )
}
