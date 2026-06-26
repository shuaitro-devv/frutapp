package cl.frutapp.app.data

/**
 * Flag de "ya vi el onboarding" persistido en [SessionStorage].
 *
 * Mismo patron que [CoachmarkStore] pero por **device**, no por usuario: el
 * onboarding son 3 slides genericas (que es FrutApp, FrutCoins, Cajita
 * mascota) — no varia por cuenta. Si el usuario A lo cerro, B en el mismo
 * celular no necesita verlo de nuevo.
 *
 * Diferencia con coachmark: este se persiste ANTES de cualquier login (no
 * hay user_id disponible), por eso es device-wide.
 */
object OnboardingStore {
    private const val KEY = "onboarding_v1_shown"

    /** True si ya se mostro al menos una vez en este device. Lectura directa
     *  (sin cache) para evitar problemas si SessionStorage no esta listo al
     *  inicializar el object. */
    val shown: Boolean
        get() = SessionStorage.getString(KEY) == "1"

    /** Marca el onboarding como visto. Llamado al "Saltar" o al "Comenzar"
     *  del ultimo slide. Una vez seteado nunca vuelve a salir solo. */
    fun markShown() {
        SessionStorage.putString(KEY, "1")
    }

    /** Reset — util si en el futuro queremos un "Ver onboarding otra vez" en
     *  Perfil, o para tests. */
    fun reset() {
        SessionStorage.remove(KEY)
    }
}
