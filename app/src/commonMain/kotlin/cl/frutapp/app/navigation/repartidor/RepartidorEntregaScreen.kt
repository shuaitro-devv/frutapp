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
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.platform.rememberSelectorImagenes
import cl.frutapp.app.ui.components.FirmaCaptureOverlay
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
            Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
                cl.frutapp.app.ui.components.DetalleSkeleton(cards = 5)
            }
            return
        }
        // El repartidor tipea aca los 4 digitos que el cliente le dice cara
        // a cara. El backend valida match contra el delivery_code generado al
        // EN_DESPACHO; si no coincide, devuelve 400 "Codigo incorrecto" y
        // mostramos el error sin transicionar el pedido.
        var codigoInput by remember { mutableStateOf("") }
        var entregando by remember { mutableStateOf(false) }
        // Fotos del paquete: hasta MAX_FOTOS_ENTREGA. Cada una tiene sus bytes
        // (para preview local + zoom) y el evidenceId del backend (para
        // eliminarla). "subiendoFoto" es global mientras hay una en camino;
        // "borrandoIndex" apunta al thumb que esta ejecutando el DELETE (null
        // = nada borrandose), asi no bloqueamos toda la grid al borrar una sola.
        var fotos by remember { mutableStateOf<List<FotoEntrega>>(emptyList()) }
        var subiendoFoto by remember { mutableStateOf(false) }
        var borrandoIndex by remember { mutableStateOf<Int?>(null) }
        // Firma del receptor: bytes PNG rendereados en cliente. No permitimos
        // borrar (queda pisada por la siguiente firma). subiendoFirma = red en
        // vuelo.
        var firmaBytes by remember { mutableStateOf<ByteArray?>(null) }
        var subiendoFirma by remember { mutableStateOf(false) }
        // Overlay activo: refactor de 3 booleans a un solo estado con tipo,
        // para que sea imposible tener dos overlays simultaneos por accidente.
        var overlay by remember { mutableStateOf<OverlayEntrega>(OverlayEntrega.None) }
        val borrandoFoto = borrandoIndex != null
        val hayRedEnVuelo = subiendoFoto || borrandoFoto || subiendoFirma
        val evidenceApi = remember { StaffEvidenceApi() }
        val selectorFoto = rememberSelectorImagenes { bytes ->
            if (hayRedEnVuelo) return@rememberSelectorImagenes
            if (fotos.size >= MAX_FOTOS_ENTREGA) {
                showToast("Máximo $MAX_FOTOS_ENTREGA fotos por entrega")
                return@rememberSelectorImagenes
            }
            // Fixture mock (pedidoId no es UUID): simulamos el flujo sin
            // pegarle al backend, si no el POST /staff/dispatches/{id}/evidence
            // devuelve 400 "orderId inválido" y confunde al repartidor de demo.
            if (!esBackendReal) {
                fotos = fotos + FotoEntrega(bytes = bytes, evidenceId = "mock-${fotos.size}")
                showToast("Foto adjuntada.")
                return@rememberSelectorImagenes
            }
            subiendoFoto = true
            scope.launch {
                runCatching { evidenceApi.subirEntrega(pedidoId, bytes, comentario = null) }
                    .onSuccess { dto ->
                        fotos = fotos + FotoEntrega(bytes = bytes, evidenceId = dto.id)
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
        val eliminarFotoEnIndex = eliminarFoto@{ index: Int ->
            if (hayRedEnVuelo) return@eliminarFoto
            val foto = fotos.getOrNull(index) ?: return@eliminarFoto
            val id = foto.evidenceId
            if (!esBackendReal || id.startsWith("mock")) {
                fotos = fotos.filterIndexed { i, _ -> i != index }
                return@eliminarFoto
            }
            borrandoIndex = index
            scope.launch {
                runCatching { evidenceApi.eliminarEntrega(pedidoId, id) }
                    .onSuccess {
                        fotos = fotos.filter { it.evidenceId != id }
                    }
                    .onFailure { e ->
                        if (e is kotlinx.coroutines.CancellationException) throw e
                        // 404 = idempotente: la fila ya no existe en backend,
                        // limpiamos el thumb local igual.
                        val es404 = e is io.ktor.client.plugins.ClientRequestException &&
                            e.response.status == io.ktor.http.HttpStatusCode.NotFound
                        if (es404) {
                            fotos = fotos.filter { it.evidenceId != id }
                        } else {
                            ErrorReporter.report(screen = "RepartidorEntrega", action = "delete_evidence", error = e)
                            showToast(mensajeAmigable(e, "eliminar la foto"))
                        }
                    }
                borrandoIndex = null
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
                FotosEntregaSection(
                    fotos = fotos,
                    subiendo = subiendoFoto,
                    borrandoIndex = borrandoIndex,
                    maxFotos = MAX_FOTOS_ENTREGA,
                    hayRedEnVuelo = hayRedEnVuelo,
                    onAgregar = { if (!hayRedEnVuelo) selectorFoto.camara() },
                    onVer = { index -> overlay = OverlayEntrega.VerFoto(index) },
                    onBorrar = eliminarFotoEnIndex,
                )
                Spacer(Modifier.height(8.dp))
                val firmaLocales = firmaBytes
                if (firmaLocales != null) {
                    FirmaPreviewCard(
                        bytes = firmaLocales,
                        subiendo = subiendoFirma,
                        onVer = { if (!subiendoFirma) overlay = OverlayEntrega.VerFirma },
                        onCambiar = {
                            if (!hayRedEnVuelo) overlay = OverlayEntrega.CapturarFirma
                        },
                    )
                } else {
                    AccionCard(
                        icon = Icons.Filled.Draw,
                        titulo = if (subiendoFirma) "Subiendo firma..." else "Firma del receptor",
                        sub = "Solicitar firma en la pantalla",
                        onClick = {
                            if (!hayRedEnVuelo) overlay = OverlayEntrega.CapturarFirma
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
        // Overlays: solo puede haber uno activo a la vez por diseño (sealed
        // class Overlay). Se renderean fuera del Column para quedar por encima
        // del bottom bar del scaffold.
        when (val ov = overlay) {
            is OverlayEntrega.None -> Unit
            is OverlayEntrega.VerFoto -> {
                val foto = fotos.getOrNull(ov.index)
                if (foto != null) {
                    FotoEntregaFullscreen(bytes = foto.bytes, onCerrar = { overlay = OverlayEntrega.None })
                } else {
                    // El index quedo stale (foto borrada mientras el visor estaba abierto)
                    overlay = OverlayEntrega.None
                }
            }
            is OverlayEntrega.VerFirma -> {
                val firma = firmaBytes
                if (firma != null) {
                    FotoEntregaFullscreen(bytes = firma, onCerrar = { overlay = OverlayEntrega.None })
                } else {
                    overlay = OverlayEntrega.None
                }
            }
            is OverlayEntrega.CapturarFirma -> {
                FirmaCaptureOverlay(
                    onCancelar = { overlay = OverlayEntrega.None },
                    onGuardar = { png ->
                        overlay = OverlayEntrega.None
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
}

/** Constante MAX de fotos por entrega — puedo pisar la del receptor si se
 *  fue del 1 al 3, pero para evidenciar entrega 3 angulos (frente / paquete /
 *  interior) alcanza con eso; abrir mas requiere pensar en storage por pedido. */
private const val MAX_FOTOS_ENTREGA = 3

/** Foto adjuntada a una entrega: bytes locales (para preview instantaneo sin
 *  bajar de MinIO) + evidenceId del backend (para el DELETE). */
private data class FotoEntrega(val bytes: ByteArray, val evidenceId: String)

/** Overlay activo en la pantalla de entrega. Excluyente por diseño (impide
 *  bugs futuros de 2 overlays abiertos a la vez). */
private sealed class OverlayEntrega {
    object None : OverlayEntrega()
    data class VerFoto(val index: Int) : OverlayEntrega()
    object VerFirma : OverlayEntrega()
    object CapturarFirma : OverlayEntrega()
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

/** Grid horizontal de fotos de entrega: hasta [maxFotos] thumbs con X para
 *  eliminar cada una + tile "+" para agregar mas. Cuando no hay ninguna
 *  muestra el estado inicial (card grande "Tomar foto"). Diseño pensado
 *  para 3 fotos (frente / paquete / interior) que caben en una fila. */
@Composable
private fun FotosEntregaSection(
    fotos: List<FotoEntrega>,
    subiendo: Boolean,
    borrandoIndex: Int?,
    maxFotos: Int,
    hayRedEnVuelo: Boolean,
    onAgregar: () -> Unit,
    onVer: (Int) -> Unit,
    onBorrar: (Int) -> Unit,
) {
    if (fotos.isEmpty() && !subiendo) {
        AccionCard(
            icon = Icons.Filled.CameraAlt,
            titulo = "Tomar foto",
            sub = "Toma hasta $maxFotos fotos del paquete entregado",
            onClick = { if (!hayRedEnVuelo) onAgregar() },
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Fotos del paquete",
                color = FrutAppColors.Brand800,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${fotos.size}/$maxFotos",
                color = FrutAppColors.InkSoft,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            fotos.forEachIndexed { index, foto ->
                FotoEntregaThumb(
                    foto = foto,
                    borrando = borrandoIndex == index,
                    onVer = { onVer(index) },
                    onBorrar = { onBorrar(index) },
                )
            }
            if (fotos.size < maxFotos) {
                TileAgregarFoto(
                    subiendo = subiendo,
                    habilitado = !hayRedEnVuelo,
                    onClick = onAgregar,
                )
            }
        }
        if (fotos.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "El cliente las ve en su tracking. Toca una para ver en grande.",
                color = FrutAppColors.InkSoft,
                fontSize = 11.sp,
            )
        }
    }
}

/** Thumb 80dp con la foto + X para eliminar. Spinner overlay mientras el
 *  DELETE del backend esta en vuelo. */
@Composable
private fun FotoEntregaThumb(
    foto: FotoEntrega,
    borrando: Boolean,
    onVer: () -> Unit,
    onBorrar: () -> Unit,
) {
    val bitmap = remember(foto.bytes) { decodeImagen(foto.bytes) }
    Box(modifier = Modifier.size(80.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FrutAppColors.Brand50, RoundedCornerShape(10.dp))
                .clickable(enabled = !borrando, onClick = onVer),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Foto del paquete",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(80.dp),
                )
            }
            if (borrando) {
                Box(modifier = Modifier.size(80.dp).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(10.dp)))
                androidx.compose.material3.CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        if (!borrando) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, FrutAppColors.Brand100, CircleShape)
                    .clickable(onClick = onBorrar),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar foto", tint = FrutAppColors.Brand800, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Tile "+" para agregar otra foto. Muestra spinner mientras hay una subida
 *  en curso (upload previo). Tile del mismo tamaño que los thumbs para que
 *  la row se vea alineada. */
@Composable
private fun TileAgregarFoto(subiendo: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(10.dp))
            .border(1.dp, FrutAppColors.Brand400, RoundedCornerShape(10.dp))
            .clickable(enabled = habilitado && !subiendo, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (subiendo) {
            androidx.compose.material3.CircularProgressIndicator(
                color = FrutAppColors.Brand600,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Icon(Icons.Filled.CameraAlt, contentDescription = "Agregar foto", tint = FrutAppColors.Brand600, modifier = Modifier.size(28.dp))
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
    val bitmap = remember(bytes) { decodeImagen(bytes) }
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
    val bitmap = remember(bytes) { decodeImagen(bytes) }
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
