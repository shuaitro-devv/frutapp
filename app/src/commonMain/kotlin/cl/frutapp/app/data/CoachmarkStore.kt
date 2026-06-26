package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

/**
 * Tour de bienvenida (coachmark) — estado en memoria por sesión.
 * - currentStep = -1 → inactivo (no se muestra overlay)
 * - currentStep >= 0 → el overlay apunta al target del step actual
 * - shown → se marca true al completar/saltar para no repetirlo automáticamente
 *
 * Cada elemento de la UI que sea target del tour se registra con `Modifier.coachmarkTarget(key)`
 * que reporta su [Rect] (en coordenadas de ventana) acá.
 */
object CoachmarkStore {
    /** Prefijo de la key; el sufijo es el user_id del usuario logueado para
     *  que cada cuenta tenga su propia marca de "ya vi el tour". Antes era
     *  global por dispositivo y los usuarios secundarios nunca lo veían. */
    private const val KEY_SHOWN_PREFIX = "coachmark_home_shown_v1_"
    /** Key heredada (global por dispositivo). La leemos para back-compat: si
     *  el usuario ya cerró el tour antes de este cambio, no le aparece otra
     *  vez en su primera sesión nueva. */
    private const val KEY_LEGACY = "coachmark_home_shown_v1"

    var currentStep by mutableStateOf(-1)
        private set

    /** Coordenadas (window bounds) de cada elemento registrado, indexado por su key. */
    val targets = mutableStateMapOf<String, Rect>()

    /** Construye la key para el usuario actual. Si no hay user logueado (raro,
     *  el Home solo se renderiza con sesion activa), cae a la key legacy. */
    private fun shownKey(): String {
        val uid = TokenStore.user?.id
        return if (uid.isNullOrBlank()) KEY_LEGACY else KEY_SHOWN_PREFIX + uid
    }

    /** Limpia el flag in-memory `shownEsteProceso` al cambiar de usuario. La
     *  pantalla de Login (o el logout) lo llama; sin esto, si el usuario A
     *  cierra el tour y despues el usuario B loguea en el mismo proceso, el
     *  flag de A bloqueaba el tour de B. */
    fun resetMemoriaProceso() {
        shownEsteProceso = false
    }

    /**
     * Marca si ya se mostró alguna vez para el USUARIO actual. Lectura
     * directa a [SessionStorage] cada vez (sin caching). Si la key del usuario
     * no esta seteada, fallback a la key legacy global — asi devices con el
     * tour cerrado antes del cambio no lo ven repetirse al primer login.
     */
    val shown: Boolean
        get() = SessionStorage.getString(shownKey()) == "1" ||
            SessionStorage.getString(KEY_LEGACY) == "1"

    /**
     * Defensa secundaria contra fallo de persist: si SessionStorage falla por algún
     * motivo (Keystore corrupto, EncryptedSharedPreferences no disponible), evitamos
     * mostrar el tour más de una vez por proceso. SOLO se setea cuando el usuario
     * cierra el tour explícitamente (skip/complete) — si lo seteáramos en
     * `maybeAutoStart` antes de start(), una navegación lejos del Home pre-cierre
     * lo perdería sin persistir nada y el próximo cold start lo volvería a mostrar.
     */
    private var shownEsteProceso = false

    val isActive: Boolean get() = currentStep >= 0

    /** Arranca el tour desde el paso 0. */
    fun start() {
        currentStep = 0
    }

    /** Avanza al siguiente paso. Si era el último, termina. */
    fun next(totalSteps: Int) {
        if (currentStep < totalSteps - 1) currentStep++
        else complete()
    }

    /** Salir del tour (Saltar). */
    fun skip() {
        currentStep = -1
        markShown()
    }

    /** Terminar exitosamente. */
    fun complete() {
        currentStep = -1
        markShown()
    }

    private fun markShown() {
        shownEsteProceso = true
        SessionStorage.putString(shownKey(), "1")
    }

    /**
     * Si nunca se mostró en este dispositivo (o en este proceso), arrancarlo. Si el
     * tour YA está activo (currentStep >= 0) — ej. user navegó al Buscador y volvió
     * a Home — no lo reiniciamos: el LaunchedEffect del HomeScreen sigue cubriéndolo.
     */
    fun maybeAutoStart() {
        if (isActive || shownEsteProceso || shown) return
        start()
    }

    fun registerTarget(key: String, rect: Rect) {
        // Solo updateamos si el rect cambio "de verdad" (mas de medio pixel en
        // cualquier dimension). Sin esto, onGloballyPositioned dispara cada
        // frame con sub-pixeles distintos por densidad — mutableStateMapOf ve
        // un cambio, recompone todo lo que observa `targets`, y eso vuelve a
        // disparar onGloballyPositioned → loop de flicker.
        val anterior = targets[key]
        if (anterior == null || !aproximadamenteIgual(anterior, rect)) {
            targets[key] = rect
        }
    }

    private fun aproximadamenteIgual(a: Rect, b: Rect): Boolean {
        val eps = 0.5f
        return kotlin.math.abs(a.left - b.left) < eps &&
            kotlin.math.abs(a.top - b.top) < eps &&
            kotlin.math.abs(a.right - b.right) < eps &&
            kotlin.math.abs(a.bottom - b.bottom) < eps
    }

    /** Reset completo — al pedir "Ver tutorial" de nuevo desde Perfil.
     *  Solo borra la marca del usuario actual; otros usuarios del mismo
     *  device mantienen su estado. */
    fun reset() {
        currentStep = -1
        shownEsteProceso = false
        targets.clear()
        SessionStorage.remove(shownKey())
    }
}
