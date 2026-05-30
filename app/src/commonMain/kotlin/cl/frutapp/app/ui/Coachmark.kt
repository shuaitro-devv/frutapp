package cl.frutapp.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.data.CoachmarkStore
import cl.frutapp.app.ui.theme.FrutAppColors

/** Un paso del tour: apunta a un elemento (key) y muestra título/texto. */
data class CoachmarkStep(
    val key: String,
    val titulo: String,
    val texto: String
)

/**
 * Marca este Composable como target del tour con la `key` dada — al posicionarse, sus
 * coordenadas se registran en [CoachmarkStore] para que el overlay las use.
 */
fun Modifier.coachmarkTarget(key: String): Modifier = this.onGloballyPositioned { coords ->
    CoachmarkStore.registerTarget(key, coords.boundsInWindow())
}

/**
 * Overlay del tour. Renderízalo como último hijo del Box raíz de la pantalla (Home) para
 * que quede por encima de TODO. Cuando [CoachmarkStore.isActive] es true, dibuja:
 *  - Capa oscura semi-transparente
 *  - "Agujero" alrededor del target del step actual (usando BlendMode.Clear)
 *  - Tooltip con título + texto + botones "Saltar" y "Siguiente"
 */
@Composable
fun CoachmarkOverlay(steps: List<CoachmarkStep>) {
    if (!CoachmarkStore.isActive) return
    val step = steps.getOrNull(CoachmarkStore.currentStep) ?: return
    val targetRect = CoachmarkStore.targets[step.key]

    Box(
        modifier = Modifier.fillMaxSize()
            // Bloquea taps en el resto de la app mientras el tour está activo (sin esto,
            // tocas "detrás" del overlay y pasan los eventos).
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (targetRect != null) {
            // Capa oscura con agujero alrededor del target.
            Canvas(
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                drawRect(Color.Black.copy(alpha = 0.7f))
                val pad = 8.dp.toPx()
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(targetRect.left - pad, targetRect.top - pad),
                    size = Size(targetRect.width + 2 * pad, targetRect.height + 2 * pad),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        } else {
            // Si todavía no llegó la registración del target, oscurece todo igual.
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)))
        }

        // Tooltip: arriba o abajo del target, según donde haya más espacio.
        // Si el target está en la mitad inferior de la pantalla, tooltip arriba (para no taparlo).
        val tooltipAtBottom = targetRect == null || targetRect.center.y < 1100
        Column(
            modifier = Modifier
                .align(if (tooltipAtBottom) Alignment.BottomCenter else Alignment.TopCenter)
                .fillMaxWidth()
                .let {
                    if (tooltipAtBottom) {
                        // Padding bottom amplio para dejar al carrito flotante visible debajo
                        // y respetar la barra de navegación del sistema.
                        it.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp)
                    } else {
                        it.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)
                    }
                }
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                "Paso ${CoachmarkStore.currentStep + 1} de ${steps.size}",
                color = FrutAppColors.InkSoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                step.titulo,
                color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                step.texto,
                color = FrutAppColors.InkMuted, fontSize = 14.sp, lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Saltar tutorial",
                    color = FrutAppColors.InkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { CoachmarkStore.skip() }
                )
                Spacer(Modifier.padding(start = 12.dp))
                Box(
                    modifier = Modifier
                        .background(FrutAppColors.Brand400, RoundedCornerShape(12.dp))
                        .clickable { CoachmarkStore.next(steps.size) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (CoachmarkStore.currentStep == steps.size - 1) "¡Listo!" else "Siguiente →",
                        color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
