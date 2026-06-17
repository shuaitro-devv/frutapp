package cl.frutapp.app.data.remote

import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.shared.dto.ChatMensajeDto
import cl.frutapp.shared.dto.WsChatPush
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Conexion WebSocket al chat de un pedido. Recibe pushes de mensajes nuevos
 * y los emite como un Flow. Reconecta automaticamente con backoff exponencial
 * (max 30s) hasta que el caller llame a [detener].
 *
 *  - URL: wss?token=<JWT> (en debug usa ws si baseUrl es http).
 *  - El cliente NO envia frames; solo escucha.
 *  - Si pierde la conexion (red caida, server reinicio), reconecta. Cada vez
 *    que se reconecta, el caller deberia volver a llamar al historial con
 *    `desde = ultimoTimestamp` para no perder mensajes ocurridos durante el
 *    downtime.
 */
class ChatWsClient(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val _mensajes = MutableSharedFlow<ChatMensajeDto>(replay = 0, extraBufferCapacity = 64)

    /** Flow de mensajes nuevos empujados por el server. */
    val mensajes: SharedFlow<ChatMensajeDto> = _mensajes

    private var job: Job? = null

    /** Abre el WS para [orderId] y empieza a empujar mensajes al flow.
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
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            runCatching {
                                val push = json.decodeFromString(WsChatPush.serializer(), frame.readText())
                                _mensajes.emit(push.mensaje)
                            }
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

    /** Cierra la conexion si esta activa. */
    suspend fun detener() {
        job?.cancelAndJoin()
        job = null
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
