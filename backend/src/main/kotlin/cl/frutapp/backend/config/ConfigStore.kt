package cl.frutapp.backend.config

import cl.frutapp.backend.db.dbQuery
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.selectAll

object ConfigTable : Table("app_config") {
    val key = text("key")
    val value = text("value")
    val type = text("type")
    val description = text("description").nullable()
    val clientVisible = bool("client_visible")
    val updatedAt = timestamp("updated_at")
    override val primaryKey = PrimaryKey(key)
}

data class ConfigEntry(val key: String, val value: String, val clientVisible: Boolean)

class ConfigRepository {
    suspend fun loadAll(): List<ConfigEntry> = dbQuery {
        ConfigTable.selectAll().map {
            ConfigEntry(it[ConfigTable.key], it[ConfigTable.value], it[ConfigTable.clientVisible])
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

    /** Subconjunto seguro para el cliente (GET /v1/config). */
    fun clientVisible(): Map<String, String> =
        entries.values.filter { it.clientVisible }.associate { it.key to it.value }
}
