package cl.frutapp.backend.modules.chat

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.media.StorageService
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.backend.modules.notifications.NotificationDispatcher
import cl.frutapp.shared.dto.ChatMensajeDto
import kotlinx.datetime.Instant
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
 *  - **Adjuntos**: solo imagenes (JPEG/PNG). Mismo patron de validacion que
 *    EvidenceService (MIME whitelist + magic-number + 5 MB cap). Mensaje
 *    puede ser solo-texto, texto+imagen, o solo-imagen.
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
    private val storage: StorageService?,
) {

    companion object {
        private const val MAX_IMAGE_BYTES = 5L * 1024 * 1024  // 5 MB
        private val MIME_WHITELIST = setOf("image/jpeg", "image/png")
    }

    /** Envia un mensaje y lo persiste + broadcast + push al destinatario. */
    suspend fun enviar(
        autorUserId: UUID,
        autorRol: String,
        orderId: UUID,
        destinatarioRolPedido: String,
        cuerpo: String,
        imagenBytes: ByteArray? = null,
        imagenContentType: String? = null,
    ): ChatMensajeDto {
        val texto = cuerpo.trim()
        val hayImagen = imagenBytes != null && imagenBytes.isNotEmpty()
        if (texto.isEmpty() && !hayImagen) {
            throw ValidationException("El mensaje no puede estar vacío.")
        }
        if (texto.length > 1000) throw ValidationException("El mensaje supera el límite de 1000 caracteres.")
        if (autorRol !in ChatRol.VALIDOS) throw ValidationException("Rol inválido.")

        // Resolver el destinatario: si el autor es staff, siempre es cliente;
        // si el autor es cliente, depende del destinatario_rol que pidio.
        val destinatarioReal = when (autorRol) {
            ChatRol.PICKER, ChatRol.REPARTIDOR -> ChatRol.CLIENTE
            ChatRol.CLIENTE -> {
                if (destinatarioRolPedido !in setOf(ChatRol.PICKER, ChatRol.REPARTIDOR)) {
                    throw ValidationException("Destinatario inválido: solo picker o repartidor.")
                }
                destinatarioRolPedido
            }
            else -> throw ValidationException("Rol inválido.")
        }

        // Ownership + validez del destinatario asignado (no puedes hablarle al
        // picker si el pedido todavia no fue tomado).
        val tipo = resolverRolUsuarioEnPedido(orderId, autorUserId)
            ?: throw NotFoundException("Pedido no encontrado.")
        if (tipo != autorRol) {
            // El cliente del pedido NO puede mandar como picker, etc.
            throw ValidationException("No tienes ese rol en este pedido.")
        }
        if (autorRol == ChatRol.CLIENTE) {
            val asignado = quienEsta(orderId, destinatarioReal)
            if (asignado == null) {
                throw ValidationException(
                    when (destinatarioReal) {
                        ChatRol.PICKER -> "Tu pedido todavía no fue tomado por un picker."
                        ChatRol.REPARTIDOR -> "Tu pedido todavía no tiene repartidor asignado."
                        else -> "Destinatario no asignado."
                    }
                )
            }
        }

        // Si hay imagen: validar + subir a MinIO antes de persistir. El key usa
        // el mensajeId que ya reservamos asi mensaje<->objeto matchean siempre.
        // (Mismo patron que EvidenceService: sube PRIMERO, despues inserta;
        // si el insert falla queda un huerfano benigno en el bucket.)
        val mensajeId = UUID.randomUUID()
        val imageKey = if (hayImagen) {
            val store = storage
                ?: throw ValidationException("Imágenes deshabilitadas en este ambiente.")
            val ct = imagenContentType ?: "application/octet-stream"
            validarImagen(imagenBytes!!, ct)
            val ext = if (ct == "image/png") "png" else "jpg"
            val key = "chat/$orderId/$mensajeId.$ext"
            store.subir(key, imagenBytes, ct)
            key
        } else null

        val row = repo.insert(orderId, autorUserId, autorRol, destinatarioReal, texto, imageKey, forcedId = mensajeId)
        val dto = row.toDto()

        // Broadcast realtime a las conexiones del pedido.
        hub.broadcast(orderId, dto)

        // Notificacion al destinatario: SIEMPRE persistimos en su inbox (para
        // que aparezca el badge en la campanita y la pantalla de notificaciones
        // sin importar si estaba conectado al chat o no). Push FCM solo si el
        // DESTINATARIO especifico NO esta conectado al chat de este pedido
        // (si lo esta, ya vio el mensaje por WS — push duplicaria).
        //
        // Antes el chequeo era hub.conexionesDe(orderId) == 0, que cuenta
        // CUALQUIER sesion (incluyendo la del autor). Si el picker tenia el
        // chat abierto y mandaba mensaje al cliente, conexionesDe = 1 → no se
        // creaba inbox NI push → el cliente no veia nada.
        val destinatarioUserId = quienEsta(orderId, destinatarioReal)
        if (destinatarioUserId != null) {
            val cuerpoBreve = if (texto.isNotEmpty()) texto.take(80) else "Te envió una foto"
            val destinatarioConectado = hub.usuarioConectado(orderId, destinatarioUserId)
            notifications.onChatMensaje(
                orderId = orderId,
                destinatarioUserId = destinatarioUserId,
                autorRol = autorRol,
                cuerpoBreve = cuerpoBreve,
                destinatarioConectado = destinatarioConectado,
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
     *  cantidad afectada. Si marcamos al menos 1, broadcast por WS para que
     *  el autor vea el tick azul en tiempo real (sin recargar). */
    suspend fun marcarLeidos(userId: UUID, orderId: UUID): Int {
        val rol = resolverRolUsuarioEnPedido(orderId, userId)
            ?: throw NotFoundException("Pedido no encontrado.")
        // El cliente marca los mensajes destinados a "cliente"; staff marca
        // los destinados a su rol.
        val n = repo.marcarLeidos(orderId, rol)
        if (n > 0) {
            hub.broadcastLeido(
                orderId = orderId,
                leidoEnRol = rol,
                leidoEn = kotlinx.datetime.Clock.System.now().toString(),
            )
        }
        return n
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

    private fun validarImagen(bytes: ByteArray, contentType: String) {
        if (contentType !in MIME_WHITELIST) {
            throw ValidationException("Tipo de imagen no permitido. Solo JPEG o PNG.")
        }
        if (bytes.size.toLong() > MAX_IMAGE_BYTES) {
            throw ValidationException("La imagen pesa más de 5 MB. Sácala con menos resolución o comprímela.")
        }
        if (!verificarMagicNumber(bytes, contentType)) {
            throw ValidationException("La imagen está corrupta o no coincide con el tipo declarado.")
        }
    }

    private fun verificarMagicNumber(bytes: ByteArray, contentType: String): Boolean {
        if (bytes.size < 8) return false
        return when (contentType) {
            "image/jpeg" -> bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            "image/png" -> bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            else -> false
        }
    }

    private fun ChatRepository.MensajeRow.toDto() = ChatMensajeDto(
        id = id.toString(),
        orderId = orderId.toString(),
        autorUserId = autorUserId.toString(),
        autorRol = autorRol,
        destinatarioRol = destinatarioRol,
        cuerpo = cuerpo,
        imagenUrl = imageKey?.let { storage?.urlFirmada(it) },
        leidoEn = leidoEn?.toString(),
        createdAt = createdAt.toString(),
    )
}
