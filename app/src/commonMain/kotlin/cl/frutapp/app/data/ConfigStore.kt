package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cl.frutapp.app.data.remote.ConfigApi
import cl.frutapp.app.ui.ErrorReporter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Configuración de negocio (envío, FrutCoins, etc.) leída del backend con cache local.
 *
 * Estrategia: **stale-while-revalidate**. El usuario ve el cache local al instante;
 * en background se refresca contra `/v1/config` y, si cambió, las pantallas que
 * leen vía Compose state recomponen. Antes de pintar el checkout final se llama
 * a [refreshNow] como cinturón de seguridad (await con timeout corto).
 *
 * Cache TTL: 30 min. El backend ya tiene cache de 60s, así que un TTL más corto en
 * el cliente solo gastaría red sin reducir la ventana de inconsistencia real.
 *
 * Si el cliente nunca consiguió hablar con el backend (primer install offline), los
 * defaults locales arrancan la app — el riesgo de cobrar mal queda cerrado igual por
 * la validación de snapshot que hace el backend al crear la orden.
 */
object ConfigStore {
    private const val K_VALUES = "config_values_json"
    private const val K_FETCHED_AT = "config_fetched_at_ms"
    private const val TTL_MS = 30L * 60L * 1000L  // 30 min

    // Defaults de fallback si no hay cache local todavia (primer install offline).
    // En cuanto el backend responda con valores nuevos, estos se sobrescriben en
    // memoria + persistencia y dejan de usarse hasta el proximo borrado de datos.
    private val DEFAULT_VALUES = mapOf(
        "costo_envio" to "2990",
        "envio_gratis_desde" to "15000",
        "frutcoins_gana_cada_clp" to "100",
        "frutcoin_valor_clp" to "1",
        "frutcoins_max_porc_pago" to "0.20"
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())
    private val mutex = Mutex()
    private val api = ConfigApi()

    // Compose state: cualquier pantalla que lea via [intOrDefault] recompone al cambiar.
    private var values: Map<String, String> by mutableStateOf(DEFAULT_VALUES)
        private set
    private var fetchedAtMs: Long = 0L

    /** Carga el cache persistido a memoria (idempotente). Llamar al arrancar la app. */
    fun restore() {
        val raw = SessionStorage.getString(K_VALUES)
        if (raw != null) {
            runCatching { json.decodeFromString(mapSerializer, raw) }
                .getOrNull()
                ?.let { values = DEFAULT_VALUES + it }  // defaults para keys ausentes
        }
        fetchedAtMs = SessionStorage.getString(K_FETCHED_AT)?.toLongOrNull() ?: 0L
    }

    /** Refresca solo si el cache esta vencido (TTL). Llamar al entrar a pantallas
     *  no criticas: la app esta usable al toque con cache, esto solo dispara la red
     *  cuando hace falta. */
    suspend fun refreshIfStale() {
        if (Clock.System.now().toEpochMilliseconds() - fetchedAtMs < TTL_MS) return
        refreshNow()
    }

    /** Refresca AHORA, ignorando el TTL. Para el checkout final, donde un valor
     *  viejo se traduce en una experiencia rota (el total que se ve no es el que
     *  el backend cobrara). Silencioso ante fallos: si la red esta caida, seguimos
     *  con el cache + el cinturon de seguridad del snapshot en el create-order. */
    suspend fun refreshNow() = mutex.withLock {
        try {
            val fresh = api.fetch().values
            // Merge con defaults: si el backend deja de exponer una key client_visible,
            // el cliente sigue teniendo un default razonable en vez de un crash.
            val merged = DEFAULT_VALUES + fresh
            if (merged != values) values = merged
            val now = Clock.System.now().toEpochMilliseconds()
            fetchedAtMs = now
            SessionStorage.putString(K_VALUES, json.encodeToString(mapSerializer, merged))
            SessionStorage.putString(K_FETCHED_AT, now.toString())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Sin red / backend caido / 5xx: seguimos con el cache. NO reportamos como
            // error visible al usuario — esto corre en background. ErrorReporter para
            // que quede traza en el debug log si pasa seguido.
            ErrorReporter.report(screen = "ConfigStore", action = "refresh", error = e)
        }
    }

    fun intOrDefault(key: String, default: Int): Int =
        values[key]?.toIntOrNull() ?: default

    fun doubleOrDefault(key: String, default: Double): Double =
        values[key]?.toDoubleOrNull() ?: default

    /** Snapshot inmutable de los valores actuales para enviar al backend con la orden.
     *  El backend compara contra su cache y rechaza con 409 si difiere — asi nunca
     *  cobramos algo distinto a lo que el cliente vio. */
    fun snapshot(): Map<String, String> = values
}
