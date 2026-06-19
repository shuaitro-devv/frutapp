package cl.frutapp.app.data.remote

import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.shared.dto.WsChatPush
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Conexion WebSocket al chat de un pedido. Bidireccional:
 *
 *  - **Recibe** frames del server (mensaje nuevo, "esta escribiendo", "leido")
 *    y los emite por [frames] para que la pantalla reaccione.
 *  - **Envia** frames efimeros del lado cliente: por ahora solo "typing"
 *    via [enviarTyping]. Mensajes persistentes y "marcar leido" siguen yendo
 *    por REST (auditable, idempotente).
 *
 * Reconecta automaticamente con backoff exponencial (max 30s) hasta que el
 * caller llame a [detener]. Si pierde la conexion, despues de reconectar la
 * pantalla deberia volver a llamar al historial con `desde = ultimoTimestamp`
 * para no perder mensajes ocurridos durante el downtime.
 */
class ChatWsClient(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val _frames = MutableSharedFlow<WsChatPush>(replay = 0, extraBufferCapacity = 64)

    /** Flow unificado de frames recibidos por el WS. La pantalla filtra por
     *  [WsChatPush.type] para distinguir mensaje / typing / leido. */
    val frames: SharedFlow<WsChatPush> = _frames

    /** Compat alias: emite solo los frames de tipo "mensaje" como ChatMensajeDto.
     *  Util si algun caller solo quiere mensajes y no eventos efimeros. */
    val mensajes: SharedFlow<cl.frutapp.shared.dto.ChatMensajeDto>
        get() = throw UnsupportedOperationException("Usa frames y filtra por type. Esta API quedo deprecada al agregar typing/leido.")

    private var job: Job? = null
    @Volatile private var sesionActiva: DefaultClientWebSocketSession? = null

    /** Abre el WS para [orderId] y empieza a empujar frames al flow.
     *  Idempotente: si ya hay una conexion, primero la cierra. */
    suspend fun conectar(scope: CoroutineScope, orderId: String) {
        detener()
        job = scope.launch {
            var backoffMs = 1_000L
            while (true) {
                val token = TokenStore.accessToken
                if (token.isNullOrBlank()) {
                    // Sin sesion → no tiene sentido reintentar; el caller se
                    // dara cuenta cuando vuelva a abrir el chat.
                    return@launch
                }
                runCatching {
                    val wsUrl = buildWsUrl(orderId, token)
                    client.webSocket({ url(wsUrl) }) {
                        backoffMs = 1_000L  // conexion exitosa → reset backoff
                        sesionActiva = this
                        try {
                            for (frame in incoming) {
                                if (frame !is Frame.Text) continue
                                runCatching {
                                    val push = json.decodeFromString(WsChatPush.serializer(), frame.readText())
                                    _frames.emit(push)
                                }
                            }
                        } finally {
                            sesionActiva = null
                        }
                    }
                }.onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    // No reportar cada reintento (ruido); solo si el ws cae
                    // con un error inesperado.
                    ErrorReporter.report(screen = "ChatWs", action = "conectar", error = e)
                }
                // Backoff exponencial hasta max 30s. Reset si reconecta exitoso.
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(30_000L)
            }
        }
    }

    /** Envia un frame "typing" si el WS esta conectado. Best-effort: si la
     *  conexion no esta lista (reconectando), se descarta sin error. */
    suspend fun enviarTyping() {
        val sesion = sesionActiva ?: return
        runCatching {
            val payload = json.encodeToString(
                WsChatPush.serializer(),
                WsChatPush(type = WsChatPush.TYPE_TYPING)
            )
            sesion.send(Frame.Text(payload))
        }
    }

    /** Cierra la conexion si esta activa. */
    suspend fun detener() {
        job?.cancelAndJoin()
        job = null
        sesionActiva = null
    }

    private fun buildWsUrl(orderId: String, token: String): String {
        // http -> ws ; https -> wss. El backend escucha en el mismo dominio.
        val proto = when {
            baseUrl.startsWith("https://") -> "wss://"
            baseUrl.startsWith("http://") -> "ws://"
            else -> "wss://"
        }
        val host = baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')
        return "$proto$host/v1/orders/$orderId/chat/ws?token=$token"
    }
}
