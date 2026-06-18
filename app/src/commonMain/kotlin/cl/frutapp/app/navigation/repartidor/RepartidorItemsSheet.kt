package cl.frutapp.app.navigation.repartidor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.data.isUuidLike
import cl.frutapp.app.data.remote.StaffDispatchApi
import cl.frutapp.app.navigation.picker.EstadoItem
import cl.frutapp.app.navigation.picker.ItemPicklist
import cl.frutapp.app.navigation.picker.picklistMock
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.IconBubble
import cl.frutapp.app.ui.components.StatusChip
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.StaffOrderItemDto

/**
 * Modal bottom sheet con la lista detallada de items del despacho. Lo abre el botón
 * "Ver items" del RepartidorDetalleScreen — antes era onClick={} no-op. El repartidor
 * lo usa para chequear que recibe del picker lo que dice el pedido.
 *
 * Reusa los mismos ItemPicklist del picker (mock fixture) para que la info coincida
 * con lo que armo el picker; cuando exista backend ambos perfiles consultan el mismo
 * endpoint del pedido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepartidorItemsSheet(
    pedidoId: String,
    onCerrar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val esBackendReal = remember(pedidoId) { pedidoId.isUuidLike() }
    val dispatchApi = remember { StaffDispatchApi() }
    var items by remember(pedidoId) {
        mutableStateOf(if (esBackendReal) null else picklistMock(pedidoId).items)
    }
    LaunchedEffect(pedidoId) {
        if (!esBackendReal) return@LaunchedEffect
        runCatching { dispatchApi.detalle(pedidoId) }
            .onSuccess { detalle ->
                items = detalle.items.map { it.toItemPicklist() }
            }
            .onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                ErrorReporter.report(screen = "RepartidorItemsSheet", action = "fetch_detalle", error = e)
                showToast("No pudimos cargar los items. Vuelve a intentarlo.")
                onCerrar()
            }
    }
    val itemsFinal = items
    val totalUnidades = remember(itemsFinal) { itemsFinal?.sumOf { it.cantidad }?.toInt() ?: 0 }

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(
                    icon = Icons.Filled.Inventory2,
                    size = 40.dp,
                    bg = FrutAppColors.Brand50,
                    fg = FrutAppColors.Brand600,
                    iconSize = 20.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Items del pedido", color = FrutAppColors.Brand800, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (itemsFinal == null) "Cargando..."
                               else "${itemsFinal.size} productos · $totalUnidades unidades aprox.",
                        color = FrutAppColors.InkMuted,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = FrutAppColors.InkSoft)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (itemsFinal == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = FrutAppColors.Brand400) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(itemsFinal, key = { it.numero }) { item ->
                        ItemRow(item = item)
                    }
                }
            }
        }
    }
}

/** El DTO del backend trae numero/nombre/cantidad/unidad/pesoVariable/emoji.
 *  Para el sheet del repartidor pasillo/estante no aplican (los usa el picker), por
 *  eso van como "-" y el ItemRow oculta la linea cuando ve "-". */
private fun StaffOrderItemDto.toItemPicklist(): ItemPicklist = ItemPicklist(
    numero = numero,
    nombre = nombre,
    cantidad = cantidad,
    unidad = unidad,
    pasillo = "-",
    estante = "-",
    pesoVariable = pesoVariable,
    emoji = emoji,
    estado = EstadoItem.PENDIENTE
)

@Composable
private fun ItemRow(item: ItemPicklist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) { Text(item.emoji, fontSize = 22.sp) }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.numero}. ${item.nombre}", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            if (item.pasillo != "-" && item.estante != "-") {
                Text("Pasillo ${item.pasillo} · Estante ${item.estante}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatoCantidad(item.cantidad)} ${item.unidad}",
                color = FrutAppColors.Brand800,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (item.pesoVariable) {
                Spacer(Modifier.height(2.dp))
                StatusChip(
                    label = "Peso variable",
                    color = FrutAppColors.Brand600,
                    fontSize = 9.sp,
                    padH = 5.dp,
                    padV = 1.dp,
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }
    }
}

private fun formatoCantidad(v: Double): String {
    val r = (v * 10).toInt() / 10.0
    val e = r.toInt()
    val d = ((r - e) * 10).toInt()
    return if (d == 0) "$e" else "$e.$d"
}
