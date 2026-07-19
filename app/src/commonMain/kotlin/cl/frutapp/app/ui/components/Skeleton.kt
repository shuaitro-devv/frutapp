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

/** Skeleton para pantallas de detalle (repartidor + picker): header con
 *  numero de pedido + N cards apiladas. Aproxima la forma final para que la
 *  transicion de placeholder a contenido no salte. */
@Composable
fun DetalleSkeleton(
    modifier: Modifier = Modifier,
    conMapa: Boolean = false,
    cards: Int = 4,
) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        // Header con id de pedido
        SkeletonBox(Modifier.fillMaxWidth(0.45f).height(22.dp))
        Spacer(Modifier.height(20.dp))
        if (conMapa) {
            SkeletonBox(Modifier.fillMaxWidth().height(180.dp), RoundedCornerShape(16.dp))
            Spacer(Modifier.height(16.dp))
        }
        repeat(cards) {
            SkeletonBox(Modifier.fillMaxWidth().height(80.dp), RoundedCornerShape(14.dp))
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Skeleton para "cola" del staff (repartidor + picker): tarjeta con contador
 *  arriba + filas de pedidos. Distinto de OrderListSkeleton porque el staff
 *  ve numeros + priorities, no fotos de producto. */
@Composable
fun ColaStaffSkeleton(modifier: Modifier = Modifier, rows: Int = 5) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Header "N pedidos"
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SkeletonBox(Modifier.fillMaxWidth(0.4f).height(16.dp))
                Spacer(Modifier.height(6.dp))
                SkeletonBox(Modifier.fillMaxWidth(0.6f).height(11.dp))
            }
            SkeletonBox(Modifier.size(50.dp, 32.dp))
        }
        Spacer(Modifier.height(20.dp))
        // Search bar
        SkeletonBox(Modifier.fillMaxWidth().height(44.dp), RoundedCornerShape(22.dp))
        Spacer(Modifier.height(16.dp))
        repeat(rows) {
            SkeletonBox(Modifier.fillMaxWidth().height(120.dp), RoundedCornerShape(14.dp))
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Skeleton para picklist (item por item con checkbox y peso). */
@Composable
fun PicklistSkeleton(modifier: Modifier = Modifier, rows: Int = 6) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
        SkeletonBox(Modifier.fillMaxWidth(0.5f).height(20.dp))
        Spacer(Modifier.height(16.dp))
        repeat(rows) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.size(24.dp), RoundedCornerShape(6.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.45f).height(14.dp))
                    Spacer(Modifier.height(6.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.25f).height(11.dp))
                }
                SkeletonBox(Modifier.size(60.dp, 32.dp))
            }
        }
    }
}
