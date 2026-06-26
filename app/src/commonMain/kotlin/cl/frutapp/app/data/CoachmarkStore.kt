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
    private const val KEY_SHOWN = "coachmark_home_shown_v1"

    var currentStep by mutableStateOf(-1)
        private set

    /** Coordenadas (window bounds) de cada elemento registrado, indexado por su key. */
    val targets = mutableStateMapOf<String, Rect>()

    /**
     * Marca si ya se mostró alguna vez en este dispositivo. Lectura DIRECTA cada vez
     * a [SessionStorage] (no cacheamos): si la inicialización del object ocurriera antes
     * de que [SessionStorage.init] termine, un `mutableStateOf(getString())` cacheado
     * quedaría en `false` para siempre en ese proceso y el tour reaparecería en cada
     * cold start aunque ya estuviera marcado.
     */
    val shown: Boolean
        get() = SessionStorage.getString(KEY_SHOWN) == "1"

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
        SessionStorage.putString(KEY_SHOWN, "1")
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

    /** Reset completo — al pedir "Ver tutorial" de nuevo desde Perfil. */
    fun reset() {
        currentStep = -1
        shownEsteProceso = false
        targets.clear()
        SessionStorage.remove(KEY_SHOWN)
    }
}
