package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.data.remote.StaffEvidenceApi
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.platform.rememberSelectorImagenes
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.launch

/**
 * Modal de evidencia visual: el picker saca una foto del item del cliente
 * (opcionalmente con un comentario) y se sube al backend. El comentario es
 * opcional, max 500 chars (validado en backend).
 *
 * Flow:
 *   1. Estado inicial: sin foto, botón "Sacar foto" + "Subir desde galería".
 *   2. Tras capturar: preview + campo comentario + botones "Sacar otra"/"Subir".
 *   3. Subiendo: spinner sobre el botón Subir.
 *   4. Exito: cierra modal + feedback ok.
 *
 * @param backendOrderId UUID del pedido (no el numero "FRU-XXXX").
 * @param backendItemId UUID del item dentro del pedido.
 * @param itemNombre solo para mostrar en el header.
 * @param onCerrar el caller decide si refrescar o no su estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvidenciaModal(
    backendOrderId: String,
    backendItemId: String,
    itemNombre: String,
    onCerrar: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val api = remember { StaffEvidenceApi() }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    var comentario by remember { mutableStateOf("") }
    var subiendo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selector = rememberSelectorImagenes { resultado ->
        bytes = resultado
        error = null
    }

    ModalBottomSheet(
        onDismissRequest = { if (!subiendo) onCerrar() },
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Foto del item", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(itemNombre, color = FrutAppColors.InkSoft, fontSize = 12.sp)
                }
                IconButton(onClick = onCerrar, enabled = !subiendo) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = FrutAppColors.InkSoft)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Preview o placeholder.
            val foto = bytes
            if (foto == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                        .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = FrutAppColors.Brand400, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Sin foto aún", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                    }
                }
            } else {
                val img = remember(foto) { decodeImagen(foto) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color.Black, RoundedCornerShape(14.dp))
                ) {
                    if (img != null) {
                        Image(
                            bitmap = img,
                            contentDescription = "Evidencia",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Botones de captura.
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FrutButtonOutline(
                    text = if (foto == null) "Sacar foto" else "Sacar otra",
                    onClick = { selector.camara() },
                    modifier = Modifier.weight(1f)
                )
                FrutButtonOutline(
                    text = "Galería",
                    onClick = { selector.galeria() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Comentario opcional.
            Text("Comentario (opcional)", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FrutAppColors.Brand50, RoundedCornerShape(10.dp))
                    .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = comentario,
                    onValueChange = { if (it.length <= 500) comentario = it },
                    textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 13.sp),
                    modifier = Modifier.fillMaxWidth().height(60.dp)
                )
                if (comentario.isEmpty()) {
                    Text("Ej: la palta venía con un golpe en este lado", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                }
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = FrutAppColors.Error, fontSize = 12.sp)
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FrutButtonOutline(text = "Cancelar", onClick = onCerrar, enabled = !subiendo, modifier = Modifier.weight(1f))
                FrutButtonPrimary(
                    text = if (subiendo) "Subiendo…" else "Subir foto",
                    enabled = !subiendo && foto != null,
                    onClick = {
                        val b = bytes ?: return@FrutButtonPrimary
                        subiendo = true
                        error = null
                        scope.launch {
                            runCatching {
                                api.subir(
                                    orderId = backendOrderId,
                                    itemId = backendItemId,
                                    bytes = b,
                                    comentario = comentario.ifBlank { null }
                                )
                            }
                                .onSuccess {
                                    showToast("Foto subida.")
                                    onCerrar()
                                }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    subiendo = false
                                    error = mensajeAmigable(e, "subir la foto")
                                    ErrorReporter.report(screen = "EvidenciaModal", action = "upload", error = e)
                                }
                        }
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
    }
}
