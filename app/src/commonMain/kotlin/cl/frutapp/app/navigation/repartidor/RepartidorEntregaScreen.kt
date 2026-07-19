package cl.frutapp.app.navigation.repartidor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.isUuidLike
import cl.frutapp.app.data.remote.StaffDispatchApi
import cl.frutapp.app.data.remote.StaffEvidenceApi
import cl.frutapp.app.platform.rememberSelectorImagenes
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.launch

/**
 * repartidor-04 — Confirmar entrega. Cliente, instrucciones, codigo de verificacion 4
 * digitos en cajas grandes, acciones de tomar foto / firma, resumen, stepper de estado
 * casi completo y botones de problema o confirmar.
 */
class RepartidorEntregaScreen(private val pedidoId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val esBackendReal = remember(pedidoId) { pedidoId.isUuidLike() }
        val dispatchApi = remember { StaffDispatchApi() }
        var despachoState by remember(pedidoId) {
            mutableStateOf(if (esBackendReal) null else despachoPorId(pedidoId))
        }
        LaunchedEffect(pedidoId) {
            if (!esBackendReal) return@LaunchedEffect
            runCatching { dispatchApi.detalle(pedidoId) }
                .onSuccess { despachoState = it.toDespachoItem() }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "RepartidorEntrega", action = "fetch_detalle", error = e)
                    showToast("No pudimos cargar los datos de entrega. Vuelve a intentarlo.")
                    navigator.pop()
                }
        }
        val despacho = despachoState ?: run {
            Box(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FrutAppColors.Brand400)
            }
            return
        }
        // El repartidor tipea aca los 4 digitos que el cliente le dice cara
        // a cara. El backend valida match contra el delivery_code generado al
        // EN_DESPACHO; si no coincide, devuelve 400 "Codigo incorrecto" y
        // mostramos el error sin transicionar el pedido.
        var codigoInput by remember { mutableStateOf("") }
        var entregando by remember { mutableStateOf(false) }
        // Foto del paquete: opcional; el repartidor la saca antes de confirmar
        // la entrega para dejar evidencia de que dejo el paquete al cliente.
        // Guardamos los bytes localmente para mostrar preview sin re-descargar
        // desde MinIO, y el evidenceId para poder eliminarla del backend si
        // el repartidor la descarta antes de confirmar. "subiendo" y "borrando"
        // deshabilitan la card para evitar dobles taps mientras hay red en curso.
        var fotoBytes by remember { mutableStateOf<ByteArray?>(null) }
        var fotoEvidenceId by remember { mutableStateOf<String?>(null) }
        var subiendoFoto by remember { mutableStateOf(false) }
        var borrandoFoto by remember { mutableStateOf(false) }
        // Toggle del visor fullscreen: se abre al tocar el thumb, se cierra al
        // tocar la imagen (mismo patron que VisorEvidenciaFullscreen del cliente).
        var verFotoFullscreen by remember { mutableStateOf(false) }
        // Firma del receptor: la ruta paralela a la foto. Guardamos los bytes
        // PNG rendereados en cliente + el id devuelto por backend. No permitimos
        // borrar la firma (por ahora): si el repartidor se equivoca, tapea
        // "Firma del receptor" de nuevo y la sobreescribe (una firma nueva se
        // toma como la vigente porque la query cliente ordena por uploadedAt DESC).
        var firmaBytes by remember { mutableStateOf<ByteArray?>(null) }
        var subiendoFirma by remember { mutableStateOf(false) }
        var capturandoFirma by remember { mutableStateOf(false) }
        var verFirmaFullscreen by remember { mutableStateOf(false) }
        val evidenceApi = remember { StaffEvidenceApi() }
        val selectorFoto = rememberSelectorImagenes { bytes ->
            if (subiendoFoto || borrandoFoto || subiendoFirma) return@rememberSelectorImagenes
            // Fixture mock (pedidoId no es UUID): simulamos el flujo sin
            // pegarle al backend, si no el POST /staff/dispatches/{id}/evidence
            // devuelve 400 "orderId inválido" y confunde al repartidor de demo.
            if (!esBackendReal) {
                fotoBytes = bytes
                fotoEvidenceId = "mock"
                showToast("Foto adjuntada.")
                return@rememberSelectorImagenes
            }
            subiendoFoto = true
            scope.launch {
                runCatching { evidenceApi.subirEntrega(pedidoId, bytes, comentario = null) }
                    .onSuccess { dto ->
                        fotoBytes = bytes
                        fotoEvidenceId = dto.id
                        showToast("Foto adjuntada.")
                    }
                    .onFailure { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        ErrorReporter.report(screen = "RepartidorEntrega", action = "upload_evidence", error = e)
                        showToast(mensajeAmigable(e, "subir la foto"))
                    }
                subiendoFoto = false
            }
        }
        val eliminarFoto = eliminarFoto@{
            if (borrandoFoto || subiendoFoto) return@eliminarFoto
            val id = fotoEvidenceId ?: return@eliminarFoto
            // Mock: solo limpia estado local (no hay endpoint que llamar).
            if (!esBackendReal || id == "mock") {
                fotoBytes = null
                fotoEvidenceId = null
                return@eliminarFoto
            }
            borrandoFoto = true
            scope.launch {
                runCatching { evidenceApi.eliminarEntrega(pedidoId, id) }
                    .onSuccess {
                        fotoBytes = null
                        fotoEvidenceId = null
                    }
                    .onFailure { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        // 404 = la evidencia ya no existe (borrada por otra
                        // sesion o retry despues de un timeout donde la
                        // primera pego bien). Idempotente: limpiamos estado
                        // local y listo, sin toast de error.
                        val es404 = e is io.ktor.client.plugins.ClientRequestException &&
                            e.response.status == io.ktor.http.HttpStatusCode.NotFound
                        if (es404) {
                            fotoBytes = null
                            fotoEvidenceId = null
                        } else {
                            ErrorReporter.report(screen = "RepartidorEntrega", action = "delete_evidence", error = e)
                            showToast(mensajeAmigable(e, "eliminar la foto"))
                        }
                    }
                borrandoFoto = false
            }
            Unit
        }
        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Confirmar entrega", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Pedido ${despacho.id}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
                }
                Row(
                    modifier = Modifier.background(FrutAppColors.Brand50, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Check, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("En destino", color = FrutAppColors.Brand800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                ClienteCard(despacho = despacho)
                Spacer(Modifier.height(10.dp))
                InstruccionesCard()
                Spacer(Modifier.height(14.dp))
                Text("Código de verificación", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Pídele al cliente el código de 4 dígitos que ve en su app", color = FrutAppColors.InkSoft, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                CodigoInputBoxes(codigo = codigoInput, onCodigo = { codigoInput = it })
                Spacer(Modifier.height(14.dp))
                val bytesLocales = fotoBytes
                if (bytesLocales != null) {
                    FotoEntregaPreviewCard(
                        bytes = bytesLocales,
                        borrando = borrandoFoto,
                        onVer = { if (!borrandoFoto) verFotoFullscreen = true },
                        onCambiar = { if (!borrandoFoto && !subiendoFoto) selectorFoto.camara() },
                        onBorrar = eliminarFoto,
                    )
                } else {
                    AccionCard(
                        icon = Icons.Filled.CameraAlt,
                        titulo = if (subiendoFoto) "Subiendo foto..." else "Tomar foto",
                        sub = "Toma foto al paquete entregado",
                        onClick = {
                            if (!subiendoFoto && !borrandoFoto && !subiendoFirma) selectorFoto.camara()
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                val firmaLocales = firmaBytes
                if (firmaLocales != null) {
                    FirmaPreviewCard(
                        bytes = firmaLocales,
                        subiendo = subiendoFirma,
                        onVer = { if (!subiendoFirma) verFirmaFullscreen = true },
                        onCambiar = {
                            if (!subiendoFirma && !subiendoFoto && !borrandoFoto) capturandoFirma = true
                        },
                    )
                } else {
                    AccionCard(
                        icon = Icons.Filled.Draw,
                        titulo = if (subiendoFirma) "Subiendo firma..." else "Firma del receptor",
                        sub = "Solicitar firma en la pantalla",
                        onClick = {
                            if (!subiendoFirma && !subiendoFoto && !borrandoFoto) capturandoFirma = true
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
                ResumenPedidoCard(items = despacho.items, unidades = despacho.unidades)
                Spacer(Modifier.height(12.dp))
                EstadoStepper(activo = 3)
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrutButtonOutline(text = "Problema", onClick = { navigator.push(RepartidorIncidenciaScreen(pedidoId)) }, modifier = Modifier.weight(1f))
                FrutButtonPrimary(
                    // No dejamos disparar delivered mientras hay una operacion
                    // de red por la foto (subiendo o borrando): si pega antes
                    // el POST /delivered, el backend transiciona a ENTREGADO y
                    // el POST /evidence rebota (status != EN_DESPACHO), quedando
                    // la entrega sin foto y con un toast confuso al repartidor.
                    text = when {
                        entregando -> "Confirmando..."
                        subiendoFoto -> "Espera la foto..."
                        borrandoFoto -> "Espera la foto..."
                        subiendoFirma -> "Espera la firma..."
                        else -> "Confirmar entrega"
                    },
                    enabled = !entregando && !subiendoFoto && !borrandoFoto && !subiendoFirma && codigoInput.length == 4,
                    onClick = {
                        if (entregando || subiendoFoto || borrandoFoto || subiendoFirma) return@FrutButtonPrimary
                        if (codigoInput.length != 4) {
                            showToast("Pídele al cliente el código de 4 dígitos.")
                            return@FrutButtonPrimary
                        }
                        if (!esBackendReal) {
                            navigator.popUntilRoot()
                            return@FrutButtonPrimary
                        }
                        entregando = true
                        scope.launch {
                            runCatching { dispatchApi.delivered(pedidoId, codigoInput) }
                                .onSuccess {
                                    showToast("¡Entrega confirmada! 🌿")
                                    navigator.popUntilRoot()
                                }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "RepartidorEntrega", action = "delivered", error = e)
                                    showToast(mensajeAmigable(e, "confirmar la entrega"))
                                    entregando = false
                                }
                        }
                    },
                    modifier = Modifier.weight(1.4f)
                )
            }
        }
        // Visor fullscreen del thumb — se abre al tocar la foto en la card.
        // Toca la imagen para cerrar (mismo gesto que VisorEvidenciaFullscreen
        // del cliente). Vive fuera del Column principal para overlay real por
        // encima del bottom bar.
        val fotoParaVer = fotoBytes
        if (verFotoFullscreen && fotoParaVer != null) {
            FotoEntregaFullscreen(bytes = fotoParaVer, onCerrar = { verFotoFullscreen = false })
        }
        val firmaParaVer = firmaBytes
        if (verFirmaFullscreen && firmaParaVer != null) {
            FotoEntregaFullscreen(bytes = firmaParaVer, onCerrar = { verFirmaFullscreen = false })
        }
        // Overlay de captura de firma. Al Guardar, mandamos el PNG al backend
        // (o simulamos en fixture) y actualizamos el estado local.
        if (capturandoFirma) {
            cl.frutapp.app.ui.components.FirmaCaptureOverlay(
                onCancelar = { capturandoFirma = false },
                onGuardar = { png ->
                    capturandoFirma = false
                    if (!esBackendReal) {
                        firmaBytes = png
                        showToast("Firma guardada.")
                        return@FirmaCaptureOverlay
                    }
                    subiendoFirma = true
                    scope.launch {
                        runCatching { evidenceApi.subirFirma(pedidoId, png) }
                            .onSuccess {
                                firmaBytes = png
                                showToast("Firma guardada.")
                            }
                            .onFailure { e ->
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                ErrorReporter.report(screen = "RepartidorEntrega", action = "upload_signature", error = e)
                                showToast(mensajeAmigable(e, "guardar la firma"))
                            }
                        subiendoFirma = false
                    }
                },
            )
        }
    }
}

@Composable
private fun ClienteCard(despacho: DespachoItem) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Text(despacho.cliente, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Filled.LocationOn, null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("${despacho.direccion}, ${despacho.sector}", color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InstruccionesCard() {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp)
    ) {
        Text("Instrucciones de entrega", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Entregar en recepción · Llamar al llegar", color = FrutAppColors.InkSoft, fontSize = 12.sp)
    }
}

@Composable
private fun CodigoBoxes(codigo: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        codigo.forEach { digito ->
            Box(
                modifier = Modifier.weight(1f).height(64.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(2.dp, FrutAppColors.Brand400, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("$digito", color = FrutAppColors.Brand800, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 4 cajas editables que reflejan el codigo que el repartidor escribe. El
 *  TextField real es invisible (alpha 0 sobre las cajas), asi el teclado
 *  funciona pero el render sigue siendo el de las cajas grandes. */
@Composable
private fun CodigoInputBoxes(codigo: String, onCodigo: (String) -> Unit) {
    androidx.compose.foundation.layout.Box {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(4) { i ->
                val digito = codigo.getOrNull(i)?.toString() ?: ""
                val activa = i == codigo.length
                Box(
                    modifier = Modifier.weight(1f).height(64.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(
                            width = if (activa) 2.dp else 1.dp,
                            color = if (activa) FrutAppColors.Brand600 else FrutAppColors.Brand200,
                            shape = RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(digito, color = FrutAppColors.Brand800, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // TextField invisible que captura el teclado. Solo digitos, max 4.
        androidx.compose.foundation.text.BasicTextField(
            value = codigo,
            onValueChange = { nuevo ->
                val limpio = nuevo.filter { it.isDigit() }.take(4)
                onCodigo(limpio)
            },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .alpha(0f),
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent),
        )
    }
}

@Composable
private fun AccionCard(icon: ImageVector, titulo: String, sub: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = FrutAppColors.InkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ResumenPedidoCard(items: Int, unidades: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).background(FrutAppColors.Brand50, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Inventory2, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Resumen del pedido", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("$items productos · $unidades unidades", color = FrutAppColors.InkSoft, fontSize = 12.sp)
            Text("Sin incidencias", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Card con preview de la foto tomada por el repartidor.
 *   - Tap en el thumb → visor fullscreen (verla en grande).
 *   - Botón "Cambiar" (texto verde) → abre la cámara para reemplazar.
 *   - Botón X en la esquina → elimina la foto sin reemplazo.
 *  Mientras el DELETE esta en vuelo el thumb muestra spinner y bloquea taps. */
@Composable
private fun FotoEntregaPreviewCard(
    bytes: ByteArray,
    borrando: Boolean,
    onVer: () -> Unit,
    onCambiar: () -> Unit,
    onBorrar: () -> Unit,
) {
    val bitmap = androidx.compose.runtime.remember(bytes) { cl.frutapp.app.platform.decodeImagen(bytes) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(FrutAppColors.Brand50, RoundedCornerShape(10.dp))
                .clickable(enabled = !borrando, onClick = onVer),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto del paquete",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp),
                )
            }
            if (borrando) {
                Box(modifier = Modifier.size(72.dp).background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp)))
                androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Foto adjuntada", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (borrando) "Eliminando..." else "El cliente la ve en su tracking. Toca la foto para verla en grande.",
                color = FrutAppColors.InkSoft,
                fontSize = 12.sp,
            )
            if (!borrando) {
                Text(
                    text = "Cambiar",
                    color = FrutAppColors.Brand600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onCambiar),
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(FrutAppColors.Brand50, CircleShape)
                .clickable(enabled = !borrando, onClick = onBorrar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Eliminar foto", tint = FrutAppColors.Brand800, modifier = Modifier.size(18.dp))
        }
    }
}

/** Card compacta para la firma capturada. Mismo layout que la foto pero sin
 *  X (la firma se sobrescribe al capturar una nueva; no permitimos borrarla
 *  sola porque legalmente es "el receptor firmo, aca esta la evidencia"). */
@Composable
private fun FirmaPreviewCard(
    bytes: ByteArray,
    subiendo: Boolean,
    onVer: () -> Unit,
    onCambiar: () -> Unit,
) {
    val bitmap = androidx.compose.runtime.remember(bytes) { cl.frutapp.app.platform.decodeImagen(bytes) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(10.dp))
                .clickable(enabled = !subiendo, onClick = onVer),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Firma del receptor",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp),
                )
            }
            if (subiendo) {
                Box(modifier = Modifier.size(72.dp).background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(10.dp)))
                androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (subiendo) "Subiendo firma..." else "Firma capturada",
                color = FrutAppColors.Brand800,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (subiendo) "Espera un momento" else "Toca la firma para verla en grande.",
                color = FrutAppColors.InkSoft,
                fontSize = 12.sp,
            )
            if (!subiendo) {
                Text(
                    text = "Volver a firmar",
                    color = FrutAppColors.Brand600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).clickable(onClick = onCambiar),
                )
            }
        }
    }
}

/** Overlay fullscreen para ver la foto del thumb en grande. Toca para cerrar. */
@Composable
private fun FotoEntregaFullscreen(bytes: ByteArray, onCerrar: () -> Unit) {
    val bitmap = androidx.compose.runtime.remember(bytes) { cl.frutapp.app.platform.decodeImagen(bytes) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(onClick = onCerrar),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto del paquete",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Toca para cerrar", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}
