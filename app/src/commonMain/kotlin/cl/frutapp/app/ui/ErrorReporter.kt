package cl.frutapp.app.ui

/**
 * Reporta errores de forma centralizada. Hoy solo loggea a la consola de la plataforma
 * (logcat en Android) con un tag estable; mañana se enchufa a Sentry / backend /v1/errors
 * cambiando la implementación. Lo importante es que TODAS las llamadas a API críticas
 * pasen por aquí, así no perdemos errores en producción.
 */
object ErrorReporter {
    /**
     * Reporta un error con su contexto (pantalla + acción que falló).
     * - screen: identificador estable de la pantalla (ej. "Checkout", "HuellaVerde")
     * - action: la acción que falló (ej. "create_order", "load_balance")
     * - error: la excepción capturada
     */
    fun report(screen: String, action: String, error: Throwable) {
        log("[$screen] $action failed: ${error::class.simpleName}: ${error.message}", error)
    }
}

/** Implementación de logging plataforma-específica (Android: Log.e con tag "FrutApp"). */
internal expect fun log(message: String, throwable: Throwable?)
