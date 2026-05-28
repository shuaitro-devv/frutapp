package cl.frutapp.backend.plugins

import cl.frutapp.backend.config.DbConfig
import cl.frutapp.backend.db.DatabaseFactory
import io.ktor.server.application.Application

/** Arranca el pool de conexiones, corre migraciones y conecta Exposed. */
fun Application.configureDatabases() {
    DatabaseFactory.init(DbConfig.from(environment.config))
}
