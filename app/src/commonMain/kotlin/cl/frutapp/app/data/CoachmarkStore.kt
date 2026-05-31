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
     * Defensa secundaria: si el persist a SessionStorage fallara (caso raro: Keystore
     * corrupto, EncryptedSharedPreferences no disponible), igual evitamos mostrar el
     * tour más de una vez dentro del mismo proceso. Se setea a true al
     * arrancar/saltar/completar — el siguiente cold start lo resetea, pero ese cold
     * start volvería a evaluar el flag persistido.
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

    /** Si nunca se mostró en este dispositivo (o en este proceso), arrancarlo. */
    fun maybeAutoStart() {
        if (shownEsteProceso || shown) return
        shownEsteProceso = true
        start()
    }

    fun registerTarget(key: String, rect: Rect) {
        targets[key] = rect
    }

    /** Reset completo — al pedir "Ver tutorial" de nuevo desde Perfil. */
    fun reset() {
        currentStep = -1
        shownEsteProceso = false
        targets.clear()
        SessionStorage.remove(KEY_SHOWN)
    }
}
