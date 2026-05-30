@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.mascota_palta_arbol
import frutapp.app.generated.resources.mascota_palta_bosque
import frutapp.app.generated.resources.mascota_palta_brote
import frutapp.app.generated.resources.mascota_palta_racha
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Racha estilo Duolingo: días consecutivos con actividad ecológica (compra o reciclaje).
 * Cuatro niveles que escalan emocionalmente — el siguiente nivel siempre se ve "alcanzable".
 */
enum class NivelRacha(
    val emoji: String,
    val titulo: String,
    /** Días en los que arranca este nivel. */
    val desdeDia: Int,
    /** Días en los que termina (null = nivel máximo, no hay siguiente). */
    val hastaDia: Int?
) {
    BROTE("🌱", "Brote", desdeDia = 1, hastaDia = 7),
    HOJA("🍃", "Hoja", desdeDia = 8, hastaDia = 30),
    ARBOL("🌳", "Árbol", desdeDia = 31, hastaDia = 100),
    BOSQUE("🌲", "Bosque", desdeDia = 101, hastaDia = null);

    /** El día en el que sube al siguiente nivel (null para BOSQUE). */
    val diaParaProximoNivel: Int? get() = hastaDia?.let { it + 1 }

    val proximoNivel: NivelRacha? get() = when (this) {
        BROTE -> HOJA
        HOJA -> ARBOL
        ARBOL -> BOSQUE
        BOSQUE -> null
    }

    /** Mascota Palta correspondiente al nivel. */
    val mascota: DrawableResource get() = when (this) {
        BROTE -> Res.drawable.mascota_palta_brote
        HOJA -> Res.drawable.mascota_palta_racha
        ARBOL -> Res.drawable.mascota_palta_arbol
        BOSQUE -> Res.drawable.mascota_palta_bosque
    }

    companion object {
        fun de(dias: Int): NivelRacha = entries.first { n ->
            dias >= n.desdeDia && (n.hastaDia == null || dias <= n.hastaDia)
        }
    }
}

/**
 * Estado de la racha del usuario (dummy en memoria por sesión). En producción se
 * calcularía desde el backend cruzando compras + reciclajes diarios.
 *
 * Arrancamos en 12 días para que el demo muestre el nivel **Hoja** (próximo: Árbol).
 */
object StreakStore {
    var dias by mutableStateOf(12)
        private set

    val nivel: NivelRacha get() = NivelRacha.de(dias)

    /** Días que faltan para subir al siguiente nivel (null si ya está en BOSQUE). */
    val diasParaProximoNivel: Int?
        get() = nivel.diaParaProximoNivel?.let { (it - dias).coerceAtLeast(0) }

    /** Suma un día (típicamente al confirmar compra o reciclaje). */
    fun sumarDia() {
        dias += 1
    }

    fun reset() {
        dias = 12
    }
}
