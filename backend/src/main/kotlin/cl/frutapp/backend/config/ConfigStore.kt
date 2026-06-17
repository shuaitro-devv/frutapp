package cl.frutapp.backend.config

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object ConfigTable : Table("app_config") {
    val key = text("key")
    val value = text("value")
    val type = text("type")
    val description = text("description").nullable()
    val clientVisible = bool("client_visible")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(key)
}

/** Vista completa de una fila de app_config. Antes solo exponíamos lo que el cliente
 *  necesitaba (key/value/clientVisible); el back office necesita además type y
 *  description para validar inputs y armar la UI. */
data class ConfigEntry(
    val key: String,
    val value: String,
    val type: String,
    val description: String?,
    val clientVisible: Boolean,
    val updatedAt: Instant
)

class ConfigRepository {
    suspend fun loadAll(): List<ConfigEntry> = dbQuery {
        ConfigTable.selectAll().map {
            ConfigEntry(
                key = it[ConfigTable.key],
                value = it[ConfigTable.value],
                type = it[ConfigTable.type],
                description = it[ConfigTable.description],
                clientVisible = it[ConfigTable.clientVisible],
                updatedAt = it[ConfigTable.updatedAt]
            )
        }
    }

    /** Edita una key existente. Devuelve filas afectadas: 0 = la key no existe (404).
     *  No creamos keys nuevas desde acá — esas vienen por migración. */
    suspend fun update(key: String, value: String): Int = dbQuery {
        ConfigTable.update({ ConfigTable.key eq key }) {
            it[ConfigTable.value] = value
            it[ConfigTable.updatedAt] = Clock.System.now()
        }
    }
}

/**
 * Caché en memoria de la configuración (fuente de verdad: tabla `app_config`).
 * Se refresca al arrancar y periódicamente, así se puede cambiar un parámetro **sin
 * redeploy**: editas la fila en BD y al próximo refresh surte efecto.
 *
 * Esta es la costura del futuro módulo de configuración (web admin escribirá la tabla).
 */
object ConfigCache {
    @Volatile
    private var entries: Map<String, ConfigEntry> = emptyMap()

    suspend fun refresh(repo: ConfigRepository) {
        entries = repo.loadAll().associateBy { it.key }
    }

    fun int(key: String, default: Int): Int = entries[key]?.value?.toIntOrNull() ?: default
    fun double(key: String, default: Double): Double = entries[key]?.value?.toDoubleOrNull() ?: default
    fun bool(key: String, default: Boolean): Boolean =
        entries[key]?.value?.equals("true", ignoreCase = true) ?: default

    /** Feature flags: convencion `feature.<nombre>` con value BOOL. Atajo para
     *  el patron mas comun del backend (gate en endpoints). Default = false:
     *  si la flag NO esta en la tabla, el feature esta apagado (fail-safe).
     *  Para activar hay que insertar la fila explicitamente (via migration o
     *  endpoint admin) → la app vieja sin la flag no rompe, solo no muestra
     *  el feature nuevo. */
    fun featureEnabled(key: String): Boolean = bool(key, default = false)

    /** Subconjunto seguro para el cliente (GET /v1/config). */
    fun clientVisible(): Map<String, String> =
        entries.values.filter { it.clientVisible }.associate { it.key to it.value }

    /** Vista completa para el back office (GET /v1/admin/config). Ordenado por key
     *  para que la UI muestre las entradas en orden estable. */
    fun all(): List<ConfigEntry> = entries.values.sortedBy { it.key }

    /** Una key específica con metadata; null si no existe. Útil para validar antes de
     *  hacer PUT (necesitamos saber el type esperado). */
    fun entry(key: String): ConfigEntry? = entries[key]
}
