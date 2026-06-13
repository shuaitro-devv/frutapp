package cl.frutapp.backend.modules.notifications

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

/**
 * Cliente para Firebase Cloud Messaging HTTP v1 API. Sin Firebase Admin SDK (pesa
 * ~30MB y arrastra Guava/Netty), implementamos el flujo OAuth2 directo:
 *
 *  1. Tomamos la service account JSON (env FIREBASE_SERVICE_ACCOUNT_JSON).
 *  2. Firmamos un JWT RS256 con la private_key como aud=token endpoint, scope=fcm.
 *  3. Intercambiamos el JWT por un access_token de corta vida (cacheamos 50 min).
 *  4. POST a fcm.googleapis.com/v1/projects/{project_id}/messages:send con Bearer.
 *
 *  Errores que importan:
 *  - 404 UNREGISTERED → el token de FCM murió (uninstall, reset, mucho tiempo
 *    sin abrir la app). Devolvemos [SendResult.UnregisteredToken] para que el
 *    dispatcher borre la fila de device_token.
 *  - 400 INVALID_ARGUMENT → mal formato del token, mismo cleanup.
 *  - 5xx → reintento? Por ahora logueamos y seguimos (el push es best-effort,
 *    el cliente tiene el polling 5s como fallback hasta migrar completamente).
 */
class FcmSender(
    private val serviceAccountJson: String
) {
    private val logger = LoggerFactory.getLogger("FcmSender")
    private val http = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val sa: ServiceAccount = json.decodeFromString(serviceAccountJson)
    private val sendEndpoint = "https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send"

    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedTokenExpiresAt: Instant = Instant.EPOCH

    /** Envia push a un solo token. */
    suspend fun send(message: FcmMessage): SendResult {
        val accessToken = withContext(Dispatchers.IO) { ensureAccessToken() }
        val body = buildBody(message)
        val request = HttpRequest.newBuilder(URI.create(sendEndpoint))
            .header("Authorization", "Bearer $accessToken")
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
            .build()
        val response = withContext(Dispatchers.IO) {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        }
        return when {
            response.statusCode() in 200..299 -> SendResult.Ok
            response.statusCode() == 404 || response.statusCode() == 400 -> {
                val errorCode = extractErrorCode(response.body())
                if (errorCode == "UNREGISTERED" || errorCode == "INVALID_ARGUMENT") {
                    logger.warn("Token FCM muerto ({}) — sera removido: ...{}", errorCode, message.token.takeLast(10))
                    SendResult.UnregisteredToken
                } else {
                    logger.error("FCM rechazó push a ...{}: {} {}", message.token.takeLast(10), response.statusCode(), response.body())
                    SendResult.Failed
                }
            }
            else -> {
                logger.error("FCM error {} para ...{}: {}", response.statusCode(), message.token.takeLast(10), response.body())
                SendResult.Failed
            }
        }
    }

    private fun buildBody(message: FcmMessage): String {
        // DATA-ONLY MESSAGE: NO incluimos `notification` payload, solo `data`.
        //
        // Why: cuando el push viene con `notification` payload Y la app esta en
        // background, Android maneja la notificacion automaticamente (system
        // tray), NO invoca a FrutAppMessagingService.onMessageReceived, y al
        // tocar abre la launcher activity SIN los extras que el deep link espera.
        //
        // Con data-only el SDK SIEMPRE llama onMessageReceived (foreground y
        // background), NUESTRO codigo construye la notificacion con el PendingIntent
        // que lleva orderId/type/status como extras, y el tap navega a la pantalla
        // correcta. title/body los meto adentro de data; el cliente ya los lee de
        // ahi como fallback (FrutAppMessagingService:45-46).
        val messageObj = buildJsonObject {
            put("token", JsonPrimitive(message.token))
            putJsonObject("data") {
                if (message.title != null) put("title", JsonPrimitive(message.title))
                if (message.body != null) put("body", JsonPrimitive(message.body))
                message.data.forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            }
            // Android-specific: HIGH priority para que data-only NO sea suprimido por
            // doze mode (data-only de NORMAL priority puede ser delayed indefinidamente
            // si el celu esta dormido). collapse_key agrupa pushes redundantes del
            // mismo pedido (ej. 3 cambios de estado rapidos → solo el ultimo se ve).
            putJsonObject("android") {
                put("priority", JsonPrimitive("HIGH"))
                if (message.androidCollapseKey != null) {
                    put("collapse_key", JsonPrimitive(message.androidCollapseKey))
                }
            }
        }
        val envelope = buildJsonObject { put("message", messageObj) }
        return json.encodeToString(JsonObject.serializer(), envelope)
    }

    /** Devuelve un access_token vigente. Cachea 50 min (el access_token vive 60). */
    private fun ensureAccessToken(): String {
        val now = Instant.now()
        val cached = cachedToken
        if (cached != null && now.isBefore(cachedTokenExpiresAt)) return cached
        return mintAccessToken().also { (token, expires) ->
            cachedToken = token
            cachedTokenExpiresAt = expires
        }.first
    }

    /** Firma JWT RS256 → intercambia por access_token. Devuelve token + when expire. */
    private fun mintAccessToken(): Pair<String, Instant> {
        val privateKey = parseRsaPrivateKey(sa.private_key)
        val now = Instant.now()
        val jwt = JWT.create()
            .withIssuer(sa.client_email)
            .withSubject(sa.client_email)
            .withAudience("https://oauth2.googleapis.com/token")
            .withClaim("scope", "https://www.googleapis.com/auth/firebase.messaging")
            .withIssuedAt(now)
            .withExpiresAt(now.plusSeconds(3600))
            .sign(Algorithm.RSA256(null, privateKey))
        val form = "grant_type=" + URLEncoder.encode("urn:ietf:params:oauth:grant-type:jwt-bearer", "UTF-8") +
            "&assertion=" + URLEncoder.encode(jwt, "UTF-8")
        val request = HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("Google OAuth2 rechazo el JWT: ${response.statusCode()} ${response.body()}")
        }
        val token = json.decodeFromString<TokenResponse>(response.body())
        // Cache 10 min antes de su expiracion para evitar races
        return token.access_token to now.plusSeconds(token.expires_in - 600L)
    }

    private fun parseRsaPrivateKey(pem: String): RSAPrivateKey {
        val cleaned = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = Base64.getDecoder().decode(cleaned)
        val keySpec = PKCS8EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey
    }

    private fun extractErrorCode(body: String): String? = runCatching {
        val obj = json.parseToJsonElement(body) as? JsonObject ?: return null
        val error = obj["error"] as? JsonObject ?: return null
        val details = error["details"] as? kotlinx.serialization.json.JsonArray
        details?.firstNotNullOfOrNull { d ->
            (d as? JsonObject)?.get("errorCode")?.let { (it as? JsonPrimitive)?.content }
        }
    }.getOrNull()

    @Serializable
    private data class ServiceAccount(
        val type: String,
        val project_id: String,
        val private_key: String,
        val client_email: String
    )

    @Serializable
    private data class TokenResponse(
        val access_token: String,
        val expires_in: Long
    )
}

/** Mensaje a enviar via FCM. Estructura intermedia para no acoplar el dispatcher al JSON. */
data class FcmMessage(
    val token: String,
    val title: String? = null,
    val body: String? = null,
    val data: Map<String, String> = emptyMap(),
    /** Agrupa pushes redundantes (ej. mismo pedido). El sistema solo muestra el último. */
    val androidCollapseKey: String? = null,
    /** Canal de notificacion creado por la app. */
    val androidChannelId: String? = null
)

sealed interface SendResult {
    /** Push entregado al bus de FCM (puede que el device aun no este). */
    data object Ok : SendResult
    /** El token de FCM murio — borrar de BD. */
    data object UnregisteredToken : SendResult
    /** Error transitorio (5xx, red). El push se perdio; el cliente tiene polling como fallback. */
    data object Failed : SendResult
}
