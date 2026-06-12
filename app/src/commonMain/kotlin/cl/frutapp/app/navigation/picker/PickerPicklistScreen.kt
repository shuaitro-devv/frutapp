package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.isUuidLike
import cl.frutapp.app.data.remote.StaffOrderApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.launch

/**
 * Picklist del pedido (picker-02). Header con ID + chip estado, stat strip con donut de
 * progreso, lista de items con checkbox grande, botones inferiores para incidencia o
 * marcar como listo. Mock data — los toggles de check son local-state-only por ahora.
 */
class PickerPicklistScreen(
    private val pedidoId: String,
    /** True cuando se entra desde el tab "En curso" — el pedido YA es del picker,
     *  no hay que hacer take (rebotaria con 409 ya_tomado). False cuando se entra
     *  desde la cola (camino feliz: tomar y empezar a editar). */
    private val yaTomado: Boolean = false,
    /** Numero legible del pedido (ej. "#FRU-2026-797724"). Se pasa desde la cola
     *  para que el header y la pantalla "Listo" muestren el numero amigable en
     *  vez del UUID. Si es null, se usa el pedidoId tal cual. */
    private val numero: String? = null,
    /** Sector/comuna del cliente para mostrar en la pantalla "Listo". */
    private val sector: String? = null,
    /** Nombre del cliente para el destino ("Pedido de Mauricio"). */
    private val cliente: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        // Si pedidoId parece un UUID, vino del backend real -> hacemos take/complete
        // contra los endpoints reales. Si es id legible (#FRU-XXX o nombre slug),
        // estamos en modo mockup (demo offline) y el flujo se queda 100% en memoria.
        val esBackendReal = remember(pedidoId) { pedidoId.isUuidLike() }
        val staffApi = remember { StaffOrderApi() }
        var completando by remember(pedidoId) { mutableStateOf(false) }

        // Si el pedido es real Y todavia no es mio, hacemos `take` automaticamente
        // al entrar (el usuario manifestó intencion al tap el card en la cola). Si
        // otro picker llego primero, mostramos toast y volvemos a la cola.
        LaunchedEffect(pedidoId) {
            if (!esBackendReal || yaTomado) return@LaunchedEffect
            runCatching { staffApi.take(pedidoId) }
                .onSuccess { res ->
                    if (!res.ok) {
                        showToast("Otro Casero ya tomó este pedido. Refrescando cola.")
                        navigator.pop()
                    }
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "PickerPicklist", action = "take", error = e)
                    showToast(mensajeAmigable(e, "tomar el pedido"))
                    navigator.pop()
                }
        }

        // Si es backend real: cargar el detalle (con items reales) del endpoint.
        // Si es mock: usar el fixture. picklist queda null durante la carga inicial.
        var picklist by remember(pedidoId) {
            mutableStateOf<PicklistData?>(if (esBackendReal) null else picklistMock(pedidoId))
        }
        LaunchedEffect(pedidoId) {
            if (!esBackendReal) return@LaunchedEffect
            runCatching { staffApi.detalle(pedidoId) }
                .onSuccess { picklist = it.toPicklistData() }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "PickerPicklist", action = "detalle", error = e)
                    // Fallback al mock para no dejar la pantalla vacia — el take ya se
                    // hizo y el pedido es nuestro, mejor mostrar algo que un spinner eterno.
                    picklist = picklistMock(pedidoId)
                    showToast("No pudimos cargar los items del backend, mostrando vista parcial.")
                }
        }
        val currentPicklist = picklist
        // OJO: NUNCA `return` en medio de un Composable function despues de
        // haber abierto un Column/Box. Compose genera grupos invisibles entre
        // el if y el resto del cuerpo; el return los salta y crashea con
        // IndexOutOfBoundsException en Stack.pop. Refactor a if/else.
        if (currentPicklist == null) {
            // Mientras carga el detalle, mostramos un splash minimal con el header
            // back+id para que la transicion no parpadee.
            Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                    }
                    Text(numero ?: pedidoId, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(color = FrutAppColors.Brand400)
                }
            }
        } else {
        val data = currentPicklist
        // State machine por item: Map<numeroItem, EstadoItem>. Cada item siempre tiene un
        // estado; el boton 'listo' se desbloquea cuando ninguno queda en PENDIENTE.
        // Cuando exista el endpoint, esto sera un PATCH al backend por item.
        var estados by remember(pedidoId, data) {
            mutableStateOf(data.items.associate { it.numero to it.estado })
        }
        var modalAbierto by remember { mutableStateOf<ModalPicklist?>(null) }
        var itemModal by remember { mutableStateOf<ItemPicklist?>(null) }
        var opcionesAbierto by remember { mutableStateOf(false) }
        var dialogoCancelar by remember { mutableStateOf(false) }
        // Item cuyo estado el usuario quiere DESRESOLVER (tap en card ya resuelta).
        // Pedimos confirmacion antes de regresar a PENDIENTE porque pierde info de
        // sustitucion/peso variable que el picker armo manualmente.
        var pedirReverso by remember { mutableStateOf<Int?>(null) }

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            TopBar(
                pedidoId = data.pedidoId,
                onBack = { navigator.pop() },
                onMenu = { opcionesAbierto = true }
            )
            StatStrip(
                total = data.totalItems,
                tiempoMin = data.tiempoEstimadoMin,
                sector = data.sector,
                destino = data.destino,
                completos = estados.values.count { it == EstadoItem.COMPLETADO },
                sustituidos = estados.values.count { it == EstadoItem.SUSTITUIDO },
                reducidos = estados.values.count { it == EstadoItem.REDUCIDO },
                faltantes = estados.values.count { it == EstadoItem.FALTANTE }
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Picklist del pedido", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Orden sugerido ▾", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(data.items, key = { it.numero }) { item ->
                    val estado = estados[item.numero] ?: EstadoItem.PENDIENTE
                    ItemCard(
                        item = item,
                        estado = estado,
                        onToggle = {
                            // Si esta PENDIENTE: peso variable → modal; sino → COMPLETADO.
                            // Si ya esta resuelto: pedir confirmacion antes de revertir a
                            // PENDIENTE (fix #4 del code-review). Tap accidental en una card
                            // grande no debe descartar silenciosamente una decision del picker.
                            if (estado == EstadoItem.PENDIENTE) {
                                if (item.pesoVariable) {
                                    itemModal = item
                                    modalAbierto = ModalPicklist.PESO
                                } else {
                                    estados = estados + (item.numero to EstadoItem.COMPLETADO)
                                }
                            } else {
                                pedirReverso = item.numero
                            }
                        },
                        onSwap = {
                            // SwapHoriz sobre un item COMPLETADO con peso variable pisa la
                            // confirmacion previa — pedimos confirmacion explicita (fix #5).
                            if (estado == EstadoItem.COMPLETADO && item.pesoVariable) {
                                pedirReverso = item.numero
                            } else {
                                itemModal = item
                                modalAbierto = ModalPicklist.SUSTITUCION
                            }
                        }
                    )
                }
            }
            BotonesInferior(
                onIncidencia = { navigator.push(PickerIncidenciaScreen(data.pedidoId)) },
                onListo = {
                    val pendientes = estados.values.count { it == EstadoItem.PENDIENTE }
                    if (pendientes != 0) {
                        showToast("Aún quedan $pendientes items por resolver")
                        return@BotonesInferior
                    }
                    if (!esBackendReal) {
                        // Modo mockup: avanza a la pantalla de "pedido listo" sin tocar red.
                        navigator.replace(PickerListoScreen(data.pedidoId, estados, numero = numero, sector = sector, cliente = cliente, tomadoEnIso = data.tomadoEnIso, backendId = if (esBackendReal) pedidoId else null))
                        return@BotonesInferior
                    }
                    if (completando) return@BotonesInferior
                    completando = true
                    scope.launch {
                        runCatching { staffApi.complete(pedidoId) }
                            .onSuccess {
                                showToast("Pedido listo para retiro 🌿")
                                navigator.replace(PickerListoScreen(data.pedidoId, estados, numero = numero, sector = sector, cliente = cliente, tomadoEnIso = data.tomadoEnIso, backendId = if (esBackendReal) pedidoId else null))
                            }
                            .onFailure { e ->
                                if (e is kotlinx.coroutines.CancellationException) throw e
                                ErrorReporter.report(screen = "PickerPicklist", action = "complete", error = e)
                                showToast(mensajeAmigable(e, "marcar el pedido como listo"))
                                completando = false
                            }
                    }
                }
            )
        }

        if (modalAbierto == ModalPicklist.PESO && itemModal != null) {
            PesoVariableModal(
                item = itemModal!!,
                onCerrar = { modalAbierto = null; itemModal = null },
                onConfirmar = { gramosReales ->
                    val item = itemModal!!
                    // Marcar local primero (UX optimista) y cerrar el modal.
                    estados = estados + (item.numero to EstadoItem.COMPLETADO)
                    modalAbierto = null
                    itemModal = null
                    // En backend real, persistir el peso. En mock no tocamos red.
                    // Si falla el backend, revertimos el estado local y avisamos —
                    // sin esto el picker creeria que confirmo y al apretar Listo
                    // el complete() encontraria items sin pesar.
                    if (esBackendReal && item.backendId != null) {
                        scope.launch {
                            runCatching { staffApi.setItemPeso(pedidoId, item.backendId, gramosReales) }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "PickerPicklist", action = "set_peso", error = e)
                                    estados = estados + (item.numero to EstadoItem.PENDIENTE)
                                    showToast(mensajeAmigable(e, "guardar el peso"))
                                }
                        }
                    }
                }
            )
        }
        if (modalAbierto == ModalPicklist.SUSTITUCION && itemModal != null) {
            SustitucionModal(
                item = itemModal!!,
                onCerrar = { modalAbierto = null; itemModal = null },
                onConfirmar = { nuevoEstado ->
                    estados = estados + (itemModal!!.numero to nuevoEstado)
                    modalAbierto = null
                    itemModal = null
                }
            )
        }
        if (opcionesAbierto) {
            PickerOpcionesSheet(
                onCerrar = { opcionesAbierto = false },
                onElegir = { opcion ->
                    when (opcion) {
                        PickerOpcion.PAUSAR -> {
                            // popUntilRoot uniforme con PickerListoScreen (fix #11).
                            showToast("Pedido pausado - vuelto a la cola")
                            navigator.popUntilRoot()
                        }
                        PickerOpcion.REPORTAR -> {
                            navigator.push(PickerIncidenciaScreen(data.pedidoId))
                        }
                        PickerOpcion.CANCELAR -> {
                            // Diálogo de confirmacion (fix #2).
                            dialogoCancelar = true
                        }
                        else -> showToast("${opcion.titulo} - Próximamente")
                    }
                }
            )
        }
        if (dialogoCancelar) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { dialogoCancelar = false },
                icon = { Icon(Icons.Filled.WarningAmber, null, tint = EstadoPaleta.faltante) },
                title = { Text("¿Cancelar pedido?", fontWeight = FontWeight.Bold) },
                text = { Text("Esta acción no se puede deshacer. El pedido saldrá de la cola y deberá registrarse un motivo a soporte.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        dialogoCancelar = false
                        showToast("Cancelado (mock)")
                        navigator.popUntilRoot()
                    }) { Text("Sí, cancelar", color = EstadoPaleta.faltante, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { dialogoCancelar = false }) { Text("Volver") }
                }
            )
        }
        pedirReverso?.let { numero ->
            val item = data.items.first { it.numero == numero }
            val estadoActual = estados[numero] ?: EstadoItem.PENDIENTE
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { pedirReverso = null },
                icon = { Icon(Icons.Filled.WarningAmber, null, tint = FrutAppColors.Brand600) },
                title = { Text("¿Deshacer resolución?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = "El item '${item.nombre}' está marcado como ${estadoActual.name.lowercase()}. " +
                               "Si lo deshaces, perderás la información que registraste para este item."
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        estados = estados + (numero to EstadoItem.PENDIENTE)
                        pedirReverso = null
                    }) { Text("Deshacer", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { pedirReverso = null }) { Text("Mantener") }
                }
            )
        }
        }
    }
}

internal enum class ModalPicklist { PESO, SUSTITUCION }

@Composable
private fun TopBar(pedidoId: String, onBack: () -> Unit, onMenu: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
        }
        Text(
            text = pedidoId,
            color = FrutAppColors.Brand800,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier
                .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(FrutAppColors.Brand600, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("En preparación", color = FrutAppColors.Brand800, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        IconButton(onClick = onMenu) {
            Icon(Icons.Filled.MoreVert, "Más", tint = FrutAppColors.Brand800)
        }
    }
}

@Composable
private fun StatStrip(
    total: Int,
    tiempoMin: Int,
    sector: String,
    destino: String,
    completos: Int,
    sustituidos: Int,
    reducidos: Int,
    faltantes: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(Icons.Filled.Inventory2, "$total", "Total", modifier = Modifier.weight(1f))
        Divider()
        StatCell(Icons.Filled.AccessTime, "$tiempoMin min", "Estimado", modifier = Modifier.weight(1f))
        Divider()
        StatCell(Icons.Filled.LocationOn, sector, destino, modifier = Modifier.weight(1.4f))
        Divider()
        DonutSegmentado(
            total = total,
            completos = completos,
            sustituidos = sustituidos,
            reducidos = reducidos,
            faltantes = faltantes,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun StatCell(icon: androidx.compose.ui.graphics.vector.ImageVector, valor: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(2.dp))
        Text(valor, color = FrutAppColors.Brand800, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(label, color = FrutAppColors.InkMuted, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.width(1.dp).height(36.dp).background(FrutAppColors.Brand100))
}

@Composable
private fun DonutSegmentado(
    total: Int,
    completos: Int,
    sustituidos: Int,
    reducidos: Int,
    faltantes: Int,
    modifier: Modifier = Modifier
) {
    val resueltos = completos + sustituidos + reducidos + faltantes
    Box(modifier = modifier.size(48.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(48.dp)) {
            val stroke = 5.dp.toPx()
            // Anillo base (gris claro) — representa el 100%, los items sin resolver quedan aqui.
            drawArc(
                color = FrutAppColors.Brand100,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )
            if (total == 0) return@Canvas
            // Arcos por tipo de resolucion, consecutivos desde el top (-90°). Asi el donut
            // se llena de izquierda a derecha mostrando la composicion real del progreso.
            val perItem = 360f / total
            var start = -90f
            fun arc(count: Int, color: Color) {
                if (count <= 0) return
                val sweep = perItem * count
                drawArc(
                    color = color,
                    startAngle = start, sweepAngle = sweep, useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
                )
                start += sweep
            }
            arc(completos, FrutAppColors.Brand400)
            arc(sustituidos, EstadoPaleta.sustituido)
            arc(reducidos, EstadoPaleta.reducido)
            arc(faltantes, EstadoPaleta.faltante)
        }
        Text("$resueltos/$total", color = FrutAppColors.Brand800, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ItemCard(item: ItemPicklist, estado: EstadoItem, onToggle: () -> Unit, onSwap: () -> Unit) {
    // Toda la card es tappeable para marcar/desmarcar. El borde refleja el estado:
    // verde fuerte si esta resuelto, gris si pendiente.
    val resuelto = estado.resuelto()
    val borde = if (resuelto) bordeColorPorEstado(estado) else FrutAppColors.Brand100
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(
                width = if (resuelto) 2.dp else 1.dp,
                color = borde,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(FrutAppColors.Brand50, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text(item.emoji, fontSize = 26.sp) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("${item.numero}. ${item.nombre}", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${formatoCant(item.cantidad)} ${item.unidad}",
                color = FrutAppColors.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            if (item.pesoVariable) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .background(FrutAppColors.Brand50, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Scale, null, tint = FrutAppColors.Brand600, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Peso variable", color = FrutAppColors.Brand600, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            // Chip de resolucion cuando el estado es distinto de COMPLETADO (que ya
            // se nota con el check verde): sustituido / reducido / faltante.
            ChipResolucion(estado = estado)
            Spacer(Modifier.height(4.dp))
            Text("Pasillo ${item.pasillo} · Estante ${item.estante}", color = FrutAppColors.InkMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            EstadoBoxGrande(estado = estado, onClick = onToggle)
            IconButton(onClick = onSwap, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.SwapHoriz, "Sustituir", tint = FrutAppColors.InkSoft, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Color de borde fuerte segun el tipo de resolucion. Para PENDIENTE devuelve Brand100
 *  (visualmente 'sin destacar'); para los demas delega en EstadoVisual. */
private fun bordeColorPorEstado(estado: EstadoItem): Color =
    if (estado == EstadoItem.PENDIENTE) FrutAppColors.Brand100 else estado.visual().color

@Composable
private fun ChipResolucion(estado: EstadoItem) {
    if (estado == EstadoItem.PENDIENTE || estado == EstadoItem.COMPLETADO) return
    val v = estado.visual()
    Spacer(Modifier.height(4.dp))
    cl.frutapp.app.ui.components.StatusChip(
        label = v.label,
        color = v.color,
        fontSize = 10.sp,
        padH = 6.dp,
        padV = 2.dp,
        shape = RoundedCornerShape(6.dp)
    )
}

@Composable
private fun EstadoBoxGrande(estado: EstadoItem, onClick: () -> Unit) {
    // PENDIENTE es caso especial: fondo blanco con borde gris (un 'check vacio' clasico).
    // El resto delega en EstadoVisual: fondo y borde del mismo color, icono blanco.
    val esPendiente = estado == EstadoItem.PENDIENTE
    val v = estado.visual()
    val bg = if (esPendiente) Color.White else v.color
    val borde = if (esPendiente) FrutAppColors.Brand100 else v.color
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color = bg, shape = RoundedCornerShape(10.dp))
            .border(width = 2.dp, color = borde, shape = RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        v.icon?.let { Icon(it, v.label, tint = Color.White, modifier = Modifier.size(24.dp)) }
    }
}

@Composable
private fun BotonesInferior(onIncidencia: () -> Unit, onListo: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FrutButtonOutline(text = "Reportar", onClick = onIncidencia, modifier = Modifier.weight(1f))
        FrutButtonPrimary(text = "Marcar como listo", onClick = onListo, modifier = Modifier.weight(1.4f))
    }
}

internal fun formatoCant(v: Double): String {
    val r = (v * 10).toInt() / 10.0
    val e = r.toInt()
    val d = ((r - e) * 10).toInt()
    return if (d == 0) "$e" else "$e.$d"
}
