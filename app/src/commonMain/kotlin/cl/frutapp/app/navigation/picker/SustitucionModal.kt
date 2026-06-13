@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.data.drawableForImageKey
import cl.frutapp.app.data.formatClp
import cl.frutapp.app.data.remote.StaffOrderApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.ProductDto
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private enum class TipoSustitucion { SUSTITUIR, REDUCIR, FALTANTE }

/**
 * Modal de sustitucion/reducir/faltante del picker, cableado al backend.
 *
 * Al abrir, llama a `GET /v1/staff/products/similar?productId=X` para mostrar
 * alternativas reales de la misma categoria (con foto, precio, disponibilidad).
 * Al confirmar, llama el endpoint correspondiente segun la opcion elegida:
 *  - SUSTITUIR → POST .../items/{itemId}/sustituir
 *  - REDUCIR   → POST .../items/{itemId}/reducir
 *  - FALTANTE  → POST .../items/{itemId}/faltante
 *
 * Si el backend falla, el modal queda abierto y muestra toast con error
 * amigable; el picklist no actualiza el estado del item. Si funciona, llama a
 * [onConfirmar] con el [EstadoItem] resultante para que el picklist lo marque
 * como resuelto y NO vuelva a aparecer como PENDIENTE.
 *
 * En modo mock (cuando [esBackendReal] es false o [pedidoId] / [itemBackendId]
 * son null), el modal NO llama al backend — confirma localmente. Util para los
 * demos del white-label que usan catalogo hardcodeado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SustitucionModal(
    item: ItemPicklist,
    pedidoId: String? = null,
    esBackendReal: Boolean = false,
    onCerrar: () -> Unit,
    onConfirmar: (EstadoItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val api = remember { StaffOrderApi() }
    var tipo by remember { mutableStateOf(TipoSustitucion.SUSTITUIR) }
    var alternativas by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var loadingAlt by remember { mutableStateOf(false) }
    var alternativaSel by remember { mutableStateOf(0) }
    var nuevaCantidad by remember { mutableStateOf(item.cantidad.toInt().coerceAtLeast(1) - 1) }
    var enviando by remember { mutableStateOf(false) }

    // Cargar productos similares al abrir (solo si tenemos el backendId del item
    // y estamos en modo backend real; sino el modal funciona "mock-style" para
    // los demos del white-label).
    val itemBackendId = item.backendId
    val productId = remember { item.productIdBackend() }
    LaunchedEffect(productId) {
        if (!esBackendReal || productId == null) return@LaunchedEffect
        loadingAlt = true
        runCatching { api.similares(productId, limit = 10) }
            .onSuccess { alternativas = it }
            .onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                ErrorReporter.report(screen = "SustitucionModal", action = "similares", error = e)
            }
        loadingAlt = false
    }

    ModalBottomSheet(
        onDismissRequest = { if (!enviando) onCerrar() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sustitución o faltante",
                    color = FrutAppColors.Brand800,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { if (!enviando) onCerrar() }) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = FrutAppColors.InkSoft)
                }
            }
            Spacer(Modifier.height(12.dp))
            ItemCabecera(item)
            Spacer(Modifier.height(14.dp))

            // 1. Sustituir por similar
            OpcionExpandible(
                seleccionada = tipo == TipoSustitucion.SUSTITUIR,
                onSelect = { tipo = TipoSustitucion.SUSTITUIR },
                icon = Icons.Filled.SwapHoriz,
                titulo = "1. Sustituir por similar",
                subtitulo = if (alternativas.isEmpty() && !loadingAlt) "Sin alternativas disponibles." else "Elige una alternativa equivalente disponible."
            ) {
                when {
                    loadingAlt -> Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = FrutAppColors.Brand400) }
                    alternativas.isEmpty() -> Text(
                        "No hay productos similares disponibles ahora. Usá Reducir o Reportar faltante.",
                        color = FrutAppColors.InkMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        alternativas.forEachIndexed { idx, alt ->
                            AlternativaRow(
                                producto = alt,
                                seleccionada = alternativaSel == idx,
                                onClick = { alternativaSel = idx }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 2. Reducir cantidad
            OpcionExpandible(
                seleccionada = tipo == TipoSustitucion.REDUCIR,
                onSelect = { tipo = TipoSustitucion.REDUCIR },
                icon = Icons.Filled.RemoveCircleOutline,
                titulo = "2. Reducir cantidad",
                subtitulo = "Entregar menos de lo solicitado."
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Nueva cantidad:", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                    StepperCantidad(
                        valor = nuevaCantidad,
                        min = 1,
                        max = item.cantidad.toInt().coerceAtLeast(1),
                        onChange = { nuevaCantidad = it }
                    )
                    Text(item.unidad, color = FrutAppColors.InkMuted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            // 3. Reportar faltante
            OpcionExpandible(
                seleccionada = tipo == TipoSustitucion.FALTANTE,
                onSelect = { tipo = TipoSustitucion.FALTANTE },
                icon = Icons.Filled.ReportProblem,
                titulo = "3. Reportar faltante",
                subtitulo = "No hay reemplazo disponible."
            )
            Spacer(Modifier.height(20.dp))

            val puedeConfirmar = when (tipo) {
                TipoSustitucion.SUSTITUIR -> alternativas.isNotEmpty() && alternativaSel in alternativas.indices
                TipoSustitucion.REDUCIR -> nuevaCantidad in 1 until item.cantidad.toInt()
                TipoSustitucion.FALTANTE -> true
            } && !enviando

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FrutButtonOutline(text = "Cancelar", onClick = onCerrar, enabled = !enviando, modifier = Modifier.weight(1f))
                FrutButtonPrimary(
                    text = if (enviando) "Procesando…" else "Confirmar acción",
                    enabled = puedeConfirmar,
                    onClick = {
                        if (!esBackendReal || pedidoId == null || itemBackendId == null) {
                            // Modo mock: confirmar local sin tocar red.
                            onConfirmar(tipo.aEstado())
                            return@FrutButtonPrimary
                        }
                        enviando = true
                        scope.launch {
                            val result = runCatching {
                                when (tipo) {
                                    TipoSustitucion.SUSTITUIR -> api.sustituirItem(pedidoId, itemBackendId, alternativas[alternativaSel].id)
                                    TipoSustitucion.REDUCIR -> api.reducirItem(pedidoId, itemBackendId, nuevaCantidad)
                                    TipoSustitucion.FALTANTE -> api.reportarFaltante(pedidoId, itemBackendId)
                                }
                            }
                            enviando = false
                            result
                                .onSuccess { onConfirmar(tipo.aEstado()) }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "SustitucionModal", action = "confirmar_${tipo.name}", error = e)
                                    showToast(mensajeAmigable(e, "registrar el cambio"))
                                }
                        }
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}

private fun TipoSustitucion.aEstado(): EstadoItem = when (this) {
    TipoSustitucion.SUSTITUIR -> EstadoItem.SUSTITUIDO
    TipoSustitucion.REDUCIR -> EstadoItem.REDUCIDO
    TipoSustitucion.FALTANTE -> EstadoItem.FALTANTE
}

/** Heuristica: si el backendId existe, el productId real esta colgado en el detalle
 *  del pedido del backend (StaffOrderItemDto.productId). Como no lo expusimos en
 *  ItemPicklist, lo dejamos como string ahora — el modal se carga con productId si
 *  ItemPicklist lo trae expandido. Cuando ampliemos ItemPicklist para incluir
 *  productId, este helper desaparece. */
private fun ItemPicklist.productIdBackend(): String? = backendProductId

@Composable
private fun ItemCabecera(item: ItemPicklist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FrutAppColors.Brand50, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val key = item.imageKey
            if (key != null) {
                Image(
                    painter = painterResource(drawableForImageKey(key)),
                    contentDescription = item.nombre,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp)
                )
            } else Text(item.emoji, fontSize = 26.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.nombre, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${formatoCant(item.cantidad)} ${item.unidad}", color = FrutAppColors.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepperCantidad(valor: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(onClick = { if (valor > min) onChange(valor - 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.RemoveCircleOutline, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Text("$valor", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = { if (valor < max) onChange(valor + 1) }, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.SwapHoriz, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun OpcionExpandible(
    seleccionada: Boolean,
    onSelect: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    contenido: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(
                width = if (seleccionada) 2.dp else 1.dp,
                color = if (seleccionada) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitulo, color = FrutAppColors.InkMuted, fontSize = 11.sp)
            }
            RadioCircle(seleccionada = seleccionada)
        }
        if (seleccionada && contenido != null) contenido()
    }
}

@Composable
private fun RadioCircle(seleccionada: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(
                width = 2.dp,
                color = if (seleccionada) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (seleccionada) Box(modifier = Modifier.size(10.dp).background(FrutAppColors.Brand400, CircleShape))
    }
}

@Composable
private fun AlternativaRow(producto: ProductDto, seleccionada: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(
                width = if (seleccionada) 2.dp else 1.dp,
                color = if (seleccionada) FrutAppColors.Brand400 else FrutAppColors.Brand100,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(drawableForImageKey(producto.imageKey)),
                contentDescription = producto.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(producto.name, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(formatClp(producto.priceClp) + "/" + producto.unit, color = FrutAppColors.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (producto.disponible) "Disponible" else "Agotado",
                color = if (producto.disponible) FrutAppColors.Brand600 else FrutAppColors.InkMuted,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold
            )
            RadioCircle(seleccionada = seleccionada)
        }
    }
}
