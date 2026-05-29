@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.FrutAppShapes
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Tarjeta de producto (mockups 06 Home / 07 Catálogo): foto sobre fondo verde
 * pálido, nombre, precio por unidad y botón "+" verde para agregar al carrito.
 */
@Composable
fun ProductCard(
    name: String,
    price: String,
    image: DrawableResource,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    unit: String = "kg",
    onClick: () -> Unit = {},
    /** Cantidad de este producto (línea por defecto) ya en el carrito. 0 = muestra "+". */
    quantity: Int = 0,
    onIncrement: () -> Unit = onAdd,
    onDecrement: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val addScale = remember { Animatable(1f) }
    var added by remember { mutableStateOf(false) }
    var addJob by remember { mutableStateOf<Job?>(null) }
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = FrutAppShapes.large,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(FrutAppColors.Brand50),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(image),
                    contentDescription = name,
                    modifier = Modifier.fillMaxWidth().padding(10.dp).height(100.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = name,
                        color = FrutAppColors.Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    // Precio + unidad en UNA sola línea (sin envolver: si no cabe, elipsis).
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = FrutAppColors.Brand600, fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                                append(price)
                            }
                            withStyle(SpanStyle(color = FrutAppColors.InkMuted, fontWeight = FontWeight.Medium, fontSize = 12.sp)) {
                                append("/$unit")
                            }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (quantity <= 0) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .scale(addScale.value)
                            .background(if (added) FrutAppColors.Brand600 else FrutAppColors.Brand400, CircleShape)
                            .clickable {
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
                        modifier = Modifier
                            .scale(addScale.value)
                            .height(36.dp)
                            .background(FrutAppColors.Brand400, RoundedCornerShape(18.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepBtn(Icons.Default.Remove, "Quitar uno", onDecrement)
                        Text(
                            "$quantity",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp)
                        )
                        StepBtn(Icons.Default.Add, "Agregar uno") {
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
        }
    }
}

@Composable
private fun StepBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}
