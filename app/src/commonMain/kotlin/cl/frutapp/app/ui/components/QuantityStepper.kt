package cl.frutapp.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Control unificado para "agregar producto al carrito" / "ajustar cantidad". Cuando
 * `quantity <= 0` muestra un círculo `+`; cuando `quantity > 0` muestra una pílula
 * `−  N  +`. Maneja sus propias micro-animaciones (escala al tocar + flash de check
 * al primer add).
 *
 * Componente reutilizable — usado desde [ProductCard] (Home/Catálogo) y desde
 * OfferCard (Ofertas). Cualquier card nueva debe enchufar este control en lugar de
 * duplicar el patrón.
 *
 * @param size diámetro del botón `+` en estado vacío. Default 36.dp; OfferCard pasa
 *   34.dp para mantener proporción con cards más chicas.
 * @param showAddedToast IGNORADO desde el fix del rectangulo negro: el Toast
 *   nativo de Android, spammeado rapido en spam de "+", se renderiza como
 *   un rectangulo negro parpadeante (bug conocido del sistema en Android 13+).
 *   El feedback visual ya esta cubierto por el flash de check + animation del
 *   boton. Parametro queda por compatibilidad de llamadas.
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    showAddedToast: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val addScale = remember { Animatable(1f) }
    var addJob by remember { mutableStateOf<Job?>(null) }
    var added by remember { mutableStateOf(false) }
    // Debounce: ignora taps mas seguidos que [DEBOUNCE_MS]. Evita sumar 20
    // unidades sin querer por spam y baja la presion sobre el sistema de
    // Toast/animaciones (mismo problema que causaba el rectangulo negro).
    var ultimoTapMs by remember { mutableStateOf(0L) }

    if (quantity <= 0) {
        Box(
            modifier = modifier
                .size(size)
                .scale(addScale.value)
                .background(if (added) FrutAppColors.Brand600 else FrutAppColors.Brand400, CircleShape)
                .clickable {
                    val ahora = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                    if (ahora - ultimoTapMs < DEBOUNCE_MS) return@clickable
                    ultimoTapMs = ahora
                    onAdd()
                    addJob?.cancel()
                    addJob = scope.launch {
                        added = true
                        addScale.animateTo(0.8f, tween(90))
                        addScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        delay(550)
                        added = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (added) Icons.Default.Check else Icons.Default.Add,
                contentDescription = "Agregar",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        Row(
            modifier = modifier
                .scale(addScale.value)
                .height(size)
                .background(FrutAppColors.Brand400, RoundedCornerShape(size / 2)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepBtn(Icons.Default.Remove, "Quitar uno", size) {
                val ahora = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                if (ahora - ultimoTapMs < DEBOUNCE_MS) return@StepBtn
                ultimoTapMs = ahora
                onDecrement()
            }
            Text(
                "$quantity",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
            StepBtn(Icons.Default.Add, "Agregar uno", size) {
                val ahora = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                if (ahora - ultimoTapMs < DEBOUNCE_MS) return@StepBtn
                ultimoTapMs = ahora
                onIncrement()
                addJob?.cancel()
                addJob = scope.launch {
                    addScale.animateTo(0.85f, tween(80))
                    addScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
            }
        }
    }
}

/** Tiempo minimo entre taps consecutivos del stepper, en ms. 80ms es
 *  imperceptible para humanos (taps deliberados van a ~200ms+) pero
 *  filtra el spam de doble-clic accidental y el spammeo "para probar". */
private const val DEBOUNCE_MS = 80L

@Composable
private fun StepBtn(icon: ImageVector, desc: String, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(size).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}
