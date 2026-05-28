package cl.frutapp.backend.db

import cl.frutapp.backend.config.DbConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import javax.sql.DataSource

/**
 * Inicializa el pool HikariCP, corre las migraciones Flyway y conecta Exposed.
 * El orden importa: Flyway crea/actualiza el esquema ANTES de que Exposed lo use.
 */
object DatabaseFactory {

    fun init(config: DbConfig) {
        val dataSource = hikari(config)
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        Database.connect(dataSource)
    }

    private fun hikari(config: DbConfig): DataSource {
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = config.jdbcUrl
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(hikariConfig)
    }
}

/** Ejecuta un bloque dentro de una transacción suspendida sobre el dispatcher IO. */
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
