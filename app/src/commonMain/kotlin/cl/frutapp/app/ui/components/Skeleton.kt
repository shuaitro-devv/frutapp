package cl.frutapp.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Skeletons de carga reutilizables (ESTÁNDAR del proyecto).
 *
 * Para el loading de cualquier pantalla nueva: compón con [SkeletonBox] (bloque con shimmer)
 * o usa un skeleton ya armado como [OrderListSkeleton]. Reemplaza a los spinners para una
 * sensación más "pro".
 */

/** Pincel "shimmer" animado (gris verdoso que barre). Base de todos los skeletons. */
@Composable
fun rememberShimmerBrush(): Brush {
    val base = Color(0xFFE6EEDD)
    val highlight = Color(0xFFF4F9EE)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    return Brush.linearGradient(listOf(base, highlight, base), start = Offset(x - 500f, 0f), end = Offset(x, 0f))
}

/** Bloque "esqueleto" reutilizable: dale tamaño con el modifier y úsalo para armar cualquier loading. */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier.clip(shape).background(rememberShimmerBrush()))
}

/** Skeleton de lista (ej. Mis pedidos): filas con miniatura + 3 líneas. */
@Composable
fun OrderListSkeleton(modifier: Modifier = Modifier, rows: Int = 5) {
    Column(modifier) {
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(Modifier.size(56.dp), RoundedCornerShape(14.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.55f).height(14.dp))
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.3f).height(11.dp))
                    Spacer(Modifier.height(8.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.4f).height(13.dp))
                }
            }
        }
    }
}
