package cl.frutapp.backend.modules.chat

import cl.frutapp.shared.dto.ChatMensajeDto
import cl.frutapp.shared.dto.WsChatPush
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.send
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Hub en memoria de conexiones WebSocket por pedido.
 *
 * Cada vez que un cliente abre el chat de un pedido, su WS se registra aca
 * (por `orderId` Y `userId`). Cuando alguien envia un mensaje por REST, el
 * ChatService llama a [broadcastMensaje] que empuja el mensaje a TODAS las
 * conexiones de ese pedido (cliente + picker/repartidor que esten
 * conectados).
 *
 * Eventos efimeros (typing/leido) se rebroadcastean EXCLUYENDO al autor —
 * no tiene sentido que el que tipea vea "te estas escribiendo".
 *
 * El tracking por userId permite preguntar "esta el DESTINATARIO conectado
 * a este pedido?" para decidir si mandamos push FCM (evita push duplicado
 * cuando ya esta viendo el mensaje por WS, y al reves: si el autor esta
 * conectado pero el destinatario no, igual mandamos push al destinatario).
 *
 * Si una conexion esta caida, falla el send y la limpiamos del set para no
 * acumular zombies.
 *
 * Diseno single-instance: si en el futuro hay varios backend nodes detras
 * de un load balancer, hay que migrar a Redis pub/sub para sincronizar
 * broadcasts entre instancias. Por ahora un solo nodo en Contabo basta.
 */
class ChatHub {
    private val sesiones = mutableMapOf<UUID, MutableSet<Conexion>>()
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }

    /** Una conexion activa: WS + el user que la abrio. */
    private data class Conexion(val ws: WebSocketSession, val userId: UUID)

    suspend fun registrar(orderId: UUID, userId: UUID, ws: WebSocketSession) {
        mutex.withLock {
            sesiones.getOrPut(orderId) { mutableSetOf() }.add(Conexion(ws, userId))
        }
    }

    suspend fun desregistrar(orderId: UUID, ws: WebSocketSession) {
        mutex.withLock {
            sesiones[orderId]?.removeAll { it.ws === ws }
            if (sesiones[orderId]?.isEmpty() == true) sesiones.remove(orderId)
        }
    }

    /** Cuantas conexiones activas tiene el pedido (TODAS, incluyendo autor).
     *  Util para diagnostico / debug. */
    suspend fun conexionesDe(orderId: UUID): Int = mutex.withLock {
        sesiones[orderId]?.size ?: 0
    }

    /** True si [userId] tiene al menos una sesion activa en el chat de
     *  [orderId]. Usado para decidir si mandar push FCM al destinatario. */
    suspend fun usuarioConectado(orderId: UUID, userId: UUID): Boolean = mutex.withLock {
        sesiones[orderId]?.any { it.userId == userId } == true
    }

    /** Push de mensaje nuevo a TODAS las conexiones del pedido. */
    suspend fun broadcastMensaje(orderId: UUID, mensaje: ChatMensajeDto) {
        enviar(orderId, WsChatPush(type = WsChatPush.TYPE_MENSAJE, mensaje = mensaje), excluir = null)
    }

    /** Backcompat: alias del nuevo broadcastMensaje. */
    suspend fun broadcast(orderId: UUID, mensaje: ChatMensajeDto) = broadcastMensaje(orderId, mensaje)

    /** Rebroadcast de "esta escribiendo" a las OTRAS sesiones del pedido.
     *  [excluir] es la sesion del autor — no tiene sentido reenviarselo. */
    suspend fun broadcastTyping(
        orderId: UUID,
        autorRol: String,
        autorUserId: UUID,
        excluir: WebSocketSession?,
    ) {
        enviar(
            orderId,
            WsChatPush(
                type = WsChatPush.TYPE_TYPING,
                typingRol = autorRol,
                typingUserId = autorUserId.toString(),
            ),
            excluir = excluir,
        )
    }

    /** Broadcast de "leido": el rol [leidoEnRol] (= destinatario_rol de los
     *  mensajes marcados) acaba de marcarlos como leidos. Las otras sesiones
     *  del pedido lo usan para actualizar el tick a azul en tiempo real. */
    suspend fun broadcastLeido(
        orderId: UUID,
        leidoEnRol: String,
        leidoEn: String,
    ) {
        enviar(
            orderId,
            WsChatPush(
                type = WsChatPush.TYPE_LEIDO,
                leidoEnRol = leidoEnRol,
                leidoEn = leidoEn,
            ),
            excluir = null,
        )
    }

    /** Envia [push] a las conexiones de [orderId]. Si [excluir] != null, salta
     *  esa sesion (util para no reenviar el typing al propio autor). Limpia
     *  conexiones zombies como efecto. */
    private suspend fun enviar(orderId: UUID, push: WsChatPush, excluir: WebSocketSession?) {
        val snapshot = mutex.withLock { sesiones[orderId]?.toList().orEmpty() }
        if (snapshot.isEmpty()) return
        val payload = json.encodeToString(WsChatPush.serializer(), push)
        val muertas = mutableListOf<Conexion>()
        for (con in snapshot) {
            if (con.ws === excluir) continue
            try {
                con.ws.send(Frame.Text(payload))
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                muertas.add(con)
            }
        }
        if (muertas.isNotEmpty()) {
            mutex.withLock {
                sesiones[orderId]?.removeAll(muertas.toSet())
                if (sesiones[orderId]?.isEmpty() == true) sesiones.remove(orderId)
            }
        }
    }
}
