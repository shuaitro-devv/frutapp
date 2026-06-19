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
 * (por `orderId`). Cuando alguien envia un mensaje por REST, el ChatService
 * llama a [broadcastMensaje] que empuja el mensaje a TODAS las conexiones
 * de ese pedido (cliente + picker/repartidor que esten conectados).
 *
 * Eventos efimeros (typing/leido) se rebroadcastean EXCLUYENDO al autor —
 * no tiene sentido que el que tipea vea "te estas escribiendo".
 *
 * Si una conexion esta caida, falla el send y la limpiamos del set para no
 * acumular zombies.
 *
 * Diseno single-instance: si en el futuro hay varios backend nodes detras
 * de un load balancer, hay que migrar a Redis pub/sub para sincronizar
 * broadcasts entre instancias. Por ahora un solo nodo en Contabo basta.
 */
class ChatHub {
    private val sesiones = mutableMapOf<UUID, MutableSet<WebSocketSession>>()
    private val mutex = Mutex()
    private val json = Json { encodeDefaults = true }

    suspend fun registrar(orderId: UUID, ws: WebSocketSession) {
        mutex.withLock {
            sesiones.getOrPut(orderId) { mutableSetOf() }.add(ws)
        }
    }

    suspend fun desregistrar(orderId: UUID, ws: WebSocketSession) {
        mutex.withLock {
            sesiones[orderId]?.remove(ws)
            if (sesiones[orderId]?.isEmpty() == true) sesiones.remove(orderId)
        }
    }

    /** Cuantas conexiones activas tiene el pedido. Util para decidir si
     *  mandar push FCM al destinatario (lo mandamos si no esta conectado). */
    suspend fun conexionesDe(orderId: UUID): Int = mutex.withLock {
        sesiones[orderId]?.size ?: 0
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
        val muertos = mutableListOf<WebSocketSession>()
        for (ws in snapshot) {
            if (ws === excluir) continue
            try {
                ws.send(Frame.Text(payload))
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                muertos.add(ws)
            }
        }
        if (muertos.isNotEmpty()) {
            mutex.withLock {
                sesiones[orderId]?.removeAll(muertos.toSet())
                if (sesiones[orderId]?.isEmpty() == true) sesiones.remove(orderId)
            }
        }
    }
}
