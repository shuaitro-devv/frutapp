package cl.frutapp.backend.modules.pagos

import cl.frutapp.backend.config.WebpayConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Cliente HTTP minimal para Webpay Plus (Transbank). Sin SDK: son 2 llamadas
 * REST. Mismo enfoque que el WebpayClient de polizapp (probado end-to-end
 * sandbox 2026-06-14, ver playbook
 * H:\Mi unidad\shuaitro-brain\general\integraciones\webpay-plus-playbook.md).
 *
 * Endpoints:
 *  - POST {base}/rswebpaytransaction/api/webpay/v1.2/transactions
 *      -> crear transaccion; devuelve token + url para la WebView.
 *  - PUT  {base}/rswebpaytransaction/api/webpay/v1.2/transactions/{token}
 *      -> confirmar (commit); devuelve status + response_code + amount.
 *
 * Aprobado = status == "AUTHORIZED" AND response_code == 0.
 *
 * Networking: java.net.http.HttpClient (JDK built-in, sin libs). Llamadas en
 * Dispatchers.IO porque son sincronas y bloquean el thread.
 */
class WebpayClient(private val cfg: WebpayConfig) {

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val endpoint = "${cfg.apiBaseUrl}/rswebpaytransaction/api/webpay/v1.2/transactions"

    @Serializable
    private data class CrearRequest(
        val buy_order: String,
        val session_id: String,
        val amount: Int,
        val return_url: String,
    )

    @Serializable
    private data class CrearResponse(val token: String, val url: String)

    /** Crea una transaccion. Devuelve (token, urlFormPost) que la app debe
     *  abrir en una WebView con form-POST de `token_ws = token`. */
    suspend fun crear(
        buyOrder: String,
        sessionId: String,
        montoClp: Int,
        returnUrl: String,
    ): CrearTxResult = withContext(Dispatchers.IO) {
        val body = json.encodeToString(
            CrearRequest.serializer(),
            CrearRequest(buyOrder, sessionId, montoClp, returnUrl)
        )
        val req = HttpRequest.newBuilder(URI.create(endpoint))
            .header("Tbk-Api-Key-Id", cfg.commerceCode)
            .header("Tbk-Api-Key-Secret", cfg.apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(20))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw WebpayException("crear: HTTP ${resp.statusCode()} ${resp.body()}")
        }
        val parsed = json.decodeFromString(CrearResponse.serializer(), resp.body())
        CrearTxResult(token = parsed.token, urlFormPost = parsed.url)
    }

    @Serializable
    private data class ConfirmarResponse(
        val status: String? = null,
        val response_code: Int? = null,
        val amount: Int? = null,
        val buy_order: String? = null,
        val authorization_code: String? = null,
        val card_detail: CardDetail? = null,
        val transaction_date: String? = null,
    )

    @Serializable
    private data class CardDetail(val card_number: String? = null)

    /** Confirma (commit) la transaccion previamente creada. Llamar UNA sola
     *  vez por token: Transbank rechaza la segunda con 422. */
    suspend fun confirmar(token: String): ConfirmarResult = withContext(Dispatchers.IO) {
        val req = HttpRequest.newBuilder(URI.create("$endpoint/$token"))
            .header("Tbk-Api-Key-Id", cfg.commerceCode)
            .header("Tbk-Api-Key-Secret", cfg.apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(20))
            .PUT(HttpRequest.BodyPublishers.ofString(""))
            .build()
        val resp = http.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            throw WebpayException("confirmar: HTTP ${resp.statusCode()} ${resp.body()}")
        }
        val parsed = json.decodeFromString(ConfirmarResponse.serializer(), resp.body())
        val aprobada = parsed.status == "AUTHORIZED" && parsed.response_code == 0
        ConfirmarResult(
            aprobada = aprobada,
            status = parsed.status,
            responseCode = parsed.response_code,
            monto = parsed.amount,
            buyOrder = parsed.buy_order,
            authorizationCode = parsed.authorization_code,
            ultimosDigitosTarjeta = parsed.card_detail?.card_number,
            fechaTx = parsed.transaction_date,
        )
    }
}

data class CrearTxResult(val token: String, val urlFormPost: String)

data class ConfirmarResult(
    val aprobada: Boolean,
    val status: String?,
    val responseCode: Int?,
    val monto: Int?,
    val buyOrder: String?,
    val authorizationCode: String?,
    val ultimosDigitosTarjeta: String?,
    val fechaTx: String?,
)

class WebpayException(message: String) : RuntimeException(message)
