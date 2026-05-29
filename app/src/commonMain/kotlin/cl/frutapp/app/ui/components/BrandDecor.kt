package cl.frutapp.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.frutapp.app.ui.theme.FrutAppColors

/**
 * Cabecera de marca: bloque verde con borde inferior asimétrico (curva tipo "ola"). Da
 * identidad visual y sirve de fondo para que el contenido claro resalte. Reutilizable en
 * onboarding, splash, auth y headers.
 */
@Composable
fun BrandWaveHeader(modifier: Modifier = Modifier, height: Dp = 240.dp) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h * 0.74f)
            // Curva asimétrica: baja a la derecha y sube a la izquierda.
            cubicTo(w * 0.72f, h * 1.06f, w * 0.30f, h * 0.62f, 0f, h * 0.90f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand800))
        )
    }
}

/**
 * Loader de marca: tres puntos que laten alternando entre los dos verdes del logo.
 * Para fondos oscuros (splash) pasar colores claros.
 */
@Composable
fun FrutLoader(
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    colorA: Color = FrutAppColors.Brand400,
    colorB: Color = FrutAppColors.Brand800
) {
    val transition = rememberInfiniteTransition()
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { i ->
            val t by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 700, delayMillis = i * 160),
                    repeatMode = RepeatMode.Reverse
                )
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(0.55f + 0.45f * t)
                    .background(lerp(colorB, colorA, t), CircleShape)
            )
        }
    }
}
