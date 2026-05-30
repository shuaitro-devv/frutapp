package cl.frutapp.app.ui

/**
 * Traduce excepciones técnicas a mensajes que el cliente puede entender.
 * Regla: el usuario final NUNCA debe ver stack traces, códigos HTTP crudos ni nombres
 * de clases internas. Si el error no calza con ningún patrón, devolvemos un mensaje
 * genérico — el detalle técnico se reporta vía [ErrorReporter] al monitor.
 */
fun mensajeAmigable(error: Throwable, accion: String = "completar la acción"): String {
    val msg = error.message.orEmpty().lowercase()
    return when {
        // Auth expirada — el usuario debería ir a Login.
        msg.contains("401") || msg.contains("unauthorized") ->
            "Tu sesión expiró. Por favor, vuelve a iniciar sesión."

        // Permisos insuficientes.
        msg.contains("403") || msg.contains("forbidden") ->
            "No tienes permiso para esta acción."

        // Validación de negocio (backend devuelve mensaje útil cuando es 422).
        msg.contains("422") || msg.contains("validation") ->
            "Hay un dato que no podemos procesar. Revisa tu pedido e intenta de nuevo."

        // Recurso no encontrado.
        msg.contains("404") || msg.contains("not found") ->
            "No encontramos lo que buscabas."

        // Caída de servidor.
        msg.contains("500") || msg.contains("internal") || msg.contains("server error") ->
            "Estamos teniendo problemas en nuestros servidores. Intenta en unos minutos."

        // Sin conexión.
        msg.contains("network") || msg.contains("connect") || msg.contains("unable to resolve") || msg.contains("timeout") ->
            "Sin conexión. Revisa tu internet e intenta de nuevo."

        // Genérico para todo lo demás.
        else -> "No pudimos $accion. Intenta de nuevo."
    }
}

/** True si el error parece deberse a sesión expirada (caller puede mandar a Login). */
fun esSesionExpirada(error: Throwable): Boolean {
    val msg = error.message.orEmpty().lowercase()
    return msg.contains("401") || msg.contains("unauthorized")
}
