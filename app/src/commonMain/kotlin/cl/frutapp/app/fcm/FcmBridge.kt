package cl.frutapp.app.fcm

/**
 * Hook desde [TokenStore] hacia la integracion FCM nativa de cada plataforma.
 *
 * El TokenStore vive en commonMain y no puede importar el SDK de Firebase (Android
 * only); este bridge lo permite via expect/actual. En androidMain dispara
 * fire-and-forget el [FcmTokenSync]; en otras plataformas (jvm, iOS futuro)
 * queda como no-op.
 *
 * onLoginSuccess: tras login efectivo (POST /auth/login OK o verify-email OK).
 *                 Manda el token de FCM al backend.
 * onLogoutSuccess: tras [TokenStore.clear]. Borra el token del backend para que
 *                  pushes del user anterior no lleguen al sucesor.
 *
 * Ambas son sincronas (no suspend) y disparan trabajo en background sin bloquear.
 */
expect object FcmBridge {
    fun onLoginSuccess()
    /**
     * Llamado al hacer logout. Recibe el `jwt` snapshot del access token de la
     * sesion que se va, capturado ANTES de que [TokenStore.clear] nulee el
     * estado — sin esto, el DELETE /v1/device/token saldria sin Bearer (la
     * coroutine corre en background y el interceptor de ApiClient ya leeria
     * null), el backend devolveria 401 y el token quedaria colgando en el
     * server, recibiendo pushes viejos hasta que FCM lo marque UNREGISTERED.
     */
    fun onLogoutSuccess(jwt: String?)
}
