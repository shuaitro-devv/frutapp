package cl.frutapp.backend.modules.chat

import cl.frutapp.backend.config.ConfigCache
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.plugins.JWT_AUTH
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.core.readBytes
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import cl.frutapp.shared.dto.WsChatPush
import java.util.UUID

/**
 * Rutas del chat in-app por pedido. Todas gated por la feature flag
 * `feature.chat`: si esta apagada, devuelven 403 sin tocar logica de
 * negocio. Asi se prende/apaga el chat desde central (`UPDATE app_config
 * SET value='true' WHERE key='feature.chat'`) sin redeploy.
 *
 *  - POST /v1/orders/{id}/chat            -> enviar mensaje
 *  - GET  /v1/orders/{id}/chat?desde=...  -> historial (cronologico ASC)
 *  - POST /v1/orders/{id}/chat/leer       -> marcar todos los del rol como leidos
 *  - WS   /v1/orders/{id}/chat/ws?token=  -> push realtime de nuevos mensajes
 *
 * Auth del WS: el navegador no manda Authorization en el handshake de un
 * WebSocket, asi que pasamos el JWT como query param `?token=...`. La
 * conexion se verifica manualmente con TokenService.buildVerifier().
 */
fun Route.chatRoutes(service: ChatService, hub: ChatHub, tokenService: TokenService) {

    fun gate(): Boolean = ConfigCache.bool("feature.chat", default = false)

    authenticate(JWT_AUTH) {

        // Multipart (mismo patron que EvidenceRoutes): campos
        //   - destinatarioRol: FormItem string (picker | repartidor; el server lo
        //     ignora si el autor es staff)
        //   - cuerpo: FormItem string opcional (puede ser vacio si hay archivo)
        //   - archivo: FileItem opcional (imagen JPEG/PNG)
        // Al menos uno de cuerpo/archivo debe venir; el service lo valida.
        post("/v1/orders/{orderId}/chat") {
            if (!gate()) { call.respond(HttpStatusCode.Forbidden); return@post }
            val autorUserId = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId invalido.")
            var destinatarioRol = ""
            var cuerpo = ""
            var bytes: ByteArray? = null
            var contentType: String? = null
            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        contentType = part.contentType?.toString() ?: "application/octet-stream"
                        bytes = part.provider().readBytes()
                    }
                    is PartData.FormItem -> when (part.name) {
                        "destinatarioRol" -> destinatarioRol = part.value
                        "cuerpo" -> cuerpo = part.value
                    }
                    else -> Unit
                }
                part.dispose()
            }
            // El rol del autor se deriva del usuario: cliente del pedido, o
            // picker/repartidor asignado. El service resuelve y valida.
            val rolAutor = service.rolEnPedido(orderId, autorUserId)
                ?: run { call.respond(HttpStatusCode.NotFound); return@post }
            val dto = service.enviar(
                autorUserId = autorUserId,
                autorRol = rolAutor,
                orderId = orderId,
                destinatarioRolPedido = destinatarioRol,
                cuerpo = cuerpo,
                imagenBytes = bytes,
                imagenContentType = contentType,
            )
            call.respond(HttpStatusCode.Created, dto)
        }

        get("/v1/orders/{orderId}/chat") {
            if (!gate()) { call.respond(HttpStatusCode.Forbidden); return@get }
            val uid = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId invalido.")
            val desde = call.request.queryParameters["desde"]
                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            call.respond(service.historial(uid, orderId, desde))
        }

        post("/v1/orders/{orderId}/chat/leer") {
            if (!gate()) { call.respond(HttpStatusCode.Forbidden); return@post }
            val uid = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId invalido.")
            val n = service.marcarLeidos(uid, orderId)
            call.respond(HttpStatusCode.OK, mapOf("marcados" to n))
        }
    }

    // WebSocket — auth manual via query token. NO va dentro de authenticate(JWT_AUTH)
    // porque ese plugin valida headers Authorization que el handshake WS no envia.
    webSocket("/v1/orders/{orderId}/chat/ws") {
        if (!gate()) {
            close(CloseReason(CloseReason.Codes.NORMAL, "Chat deshabilitado")); return@webSocket
        }
        val token = call.request.queryParameters["token"]
        if (token.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token requerido")); return@webSocket
        }
        val payload = runCatching { tokenService.buildVerifier().verify(token) }.getOrNull()
        if (payload == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token invalido")); return@webSocket
        }
        val uid = runCatching { UUID.fromString(payload.subject) }.getOrNull()
        val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (uid == null || orderId == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Parametros invalidos")); return@webSocket
        }
        // Ownership: el usuario debe participar en este pedido.
        val rol = service.rolEnPedido(orderId, uid)
        if (rol == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Sin acceso")); return@webSocket
        }
        hub.registrar(orderId, uid, this)
        val wsJson = Json { ignoreUnknownKeys = true; isLenient = true }
        try {
            // El cliente envia frames "typing" mientras tipea; el server los
            // rebroadcastea a las OTRAS sesiones del pedido. Mensajes
            // persistentes y "marcar leido" SIGUEN yendo por REST (auditable,
            // idempotente). Cualquier otro tipo de frame se ignora silenciosamente.
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val push = runCatching { wsJson.decodeFromString(WsChatPush.serializer(), frame.readText()) }.getOrNull()
                if (push?.type == WsChatPush.TYPE_TYPING) {
                    hub.broadcastTyping(
                        orderId = orderId,
                        autorRol = rol,
                        autorUserId = uid,
                        excluir = this,
                    )
                }
            }
        } finally {
            hub.desregistrar(orderId, this)
        }
    }
}
