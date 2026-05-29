package cl.frutapp.app.data

/**
 * Autenticación biométrica (huella) — expect/actual. En Android usa BiometricPrompt.
 * Si no hay huella disponible/enrolada, [isAvailable] devuelve false y el flujo no
 * bloquea al usuario.
 */
expect object BiometricAuth {
    fun isAvailable(): Boolean
    fun authenticate(onSuccess: () -> Unit, onError: () -> Unit)
}
