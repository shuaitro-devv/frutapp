package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.ChatApi
import cl.frutapp.app.data.remote.ChatWsClient
import cl.frutapp.app.platform.Imagenes
import cl.frutapp.app.platform.decodeImagen
import cl.frutapp.app.platform.rememberSelectorImagenes
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.IconBubble
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.ChatMensajeDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Chat in-app por pedido. Tres flujos:
 *
 *  - Cliente <-> Picker (cuando hay picker asignado)
 *  - Cliente <-> Repartidor (cuando hay repartidor asignado)
 *  - Staff <-> Cliente (idem)
 *
 * [destinatarioRol] determina con quien habla el usuario:
 *  - Cliente abriendo chat con picker: destinatarioRol = "picker".
 *  - Cliente abriendo chat con repartidor: destinatarioRol = "repartidor".
 *  - Picker/Repartidor: destinatarioRol = "cliente".
 *
 * El backend valida el rol del autor (segun la asignacion del pedido) y
 * filtra que solo se vean los mensajes de ese flujo en el historial cliente.
 * Para staff vemos todos los mensajes del pedido (asi ven la cadena completa
 * si soporte quiere contexto).
 *
 * Soporta adjuntar **una** imagen por mensaje (JPEG/PNG). El selector reusa
 * el mismo `rememberSelectorImagenes` que [EvidenciaModal] — los bytes vienen
 * comprimidos a 1280px JPG 80% por la plataforma, no hay que tocar nada acá.
 */
class ChatScreen(
    private val orderId: String,
    private val destinatarioRol: String,  // "picker" / "repartidor" / "cliente"
    private val tituloContraparte: String, // ej "Tu Seleccionador de Frescura"
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val api = remember { ChatApi() }
        val ws = remember { ChatWsClient() }
        var mensajes by remember { mutableStateOf<List<ChatMensajeDto>>(emptyList()) }
        var entrada by remember { mutableStateOf("") }
        var imagenPendiente by remember { mutableStateOf<ByteArray?>(null) }
        var enviando by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var elegirMedio by remember { mutableStateOf(false) }

        val selector = rememberSelectorImagenes { bytes ->
            imagenPendiente = bytes
            error = null
        }

        // Cargar historial al entrar + abrir WS.
        LaunchedEffect(orderId) {
            runCatching { api.historial(orderId) }
                .onSuccess { mensajes = it }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    error = "No pudimos cargar la conversación."
                    ErrorReporter.report(screen = "Chat", action = "historial", error = e)
                }
            // Marcar leidos al abrir (fire-and-forget).
            scope.launch { runCatching { api.marcarLeidos(orderId) } }
            // Abrir WS y escuchar pushes.
            ws.conectar(scope, orderId)
            ws.mensajes.collect { nuevo ->
                // Append solo si no esta ya (el envio local agrego el dto antes
                // del broadcast → evitamos duplicar el autor).
                if (mensajes.none { it.id == nuevo.id }) {
                    mensajes = mensajes + nuevo
                }
            }
        }

        DisposableEffect(orderId) {
            onDispose {
                // Cerrar WS cuando la pantalla sale del scope.
                runBlocking { runCatching { ws.detener() } }
            }
        }

        val listState = rememberLazyListState()
        LaunchedEffect(mensajes.size) {
            if (mensajes.isNotEmpty()) listState.animateScrollToItem(mensajes.size - 1)
        }

        // imePadding en el Column raiz: cuando aparece el teclado, todo se
        // desplaza arriba (input + lista). Sin esto el input queda detras del
        // teclado y la lista no se ajusta.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .statusBarsPadding()
                .imePadding()
        ) {
            // Header con avatar de la contraparte
            ChatHeader(
                rolContraparte = destinatarioRol,
                titulo = tituloContraparte,
                onBack = { navigator.pop() }
            )

            // Lista de mensajes
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(FrutAppColors.Brand50.copy(alpha = 0.3f))) {
                if (mensajes.isEmpty() && error == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Aún no hay mensajes.\nEscribe el primero abajo.",
                            color = FrutAppColors.InkSoft,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (error != null && mensajes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(mensajes, key = { it.id }) { m ->
                            // Es mio si lo envie como el rol que se mostro
                            // como autor (cliente si destinatarioRol != cliente,
                            // o staff si destinatarioRol == cliente).
                            val esMio = if (destinatarioRol == "cliente") {
                                m.autorRol != "cliente"
                            } else {
                                m.autorRol == "cliente"
                            }
                            MensajeBurbuja(mensaje = m, esMio = esMio)
                        }
                    }
                }
            }

            // Preview de imagen seleccionada (chip antes de enviar)
            val previaBytes = imagenPendiente
            if (previaBytes != null) {
                PreviewImagen(
                    bytes = previaBytes,
                    onQuitar = { imagenPendiente = null }
                )
            }

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(
                    onClick = { elegirMedio = true },
                    enabled = !enviando,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = "Adjuntar imagen",
                        tint = if (enviando) FrutAppColors.InkSoft else FrutAppColors.Brand600,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(FrutAppColors.Brand50, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    BasicTextField(
                        value = entrada,
                        onValueChange = { if (it.length <= 1000) entrada = it },
                        textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (entrada.isEmpty()) {
                        Text("Escribe un mensaje…", color = FrutAppColors.InkSoft, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                val puedeEnviar = (entrada.isNotBlank() || imagenPendiente != null) && !enviando
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (puedeEnviar) FrutAppColors.Brand600 else FrutAppColors.Brand200,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (!puedeEnviar) return@IconButton
                            val txt = entrada.trim()
                            val foto = imagenPendiente
                            enviando = true
                            scope.launch {
                                runCatching { api.enviar(orderId, destinatarioRol, txt, foto) }
                                    .onSuccess { dto ->
                                        if (mensajes.none { it.id == dto.id }) {
                                            mensajes = mensajes + dto
                                        }
                                        entrada = ""
                                        imagenPendiente = null
                                    }
                                    .onFailure { e ->
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        error = "No se pudo enviar."
                                        ErrorReporter.report(screen = "Chat", action = "enviar", error = e)
                                    }
                                enviando = false
                            }
                        },
                        enabled = puedeEnviar,
                    ) {
                        Icon(Icons.Filled.Send, "Enviar", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Sheet "Galeria / Camara" — separado para que el usuario elija
        // sin ocupar dos botones en el input row.
        if (elegirMedio) {
            val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { elegirMedio = false },
                sheetState = sheet,
                containerColor = Color.White,
                dragHandle = null
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 12.dp)) {
                    Text("Adjuntar imagen", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FrutButtonOutline(
                            text = "Cámara",
                            onClick = { elegirMedio = false; selector.camara() },
                            modifier = Modifier.weight(1f)
                        )
                        FrutButtonOutline(
                            text = "Galería",
                            onClick = { elegirMedio = false; selector.galeria() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatHeader(rolContraparte: String, titulo: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
        }
        IconBubble(
            initial = inicialPorRol(rolContraparte, titulo),
            size = 40.dp,
            bg = colorRolBg(rolContraparte),
            fg = colorRolFg(rolContraparte),
            textSize = 16.sp
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("En este pedido", color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MensajeBurbuja(mensaje: ChatMensajeDto, esMio: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!esMio) {
            IconBubble(
                initial = inicialPorRol(mensaje.autorRol, mensaje.autorRol),
                size = 28.dp,
                bg = colorRolBg(mensaje.autorRol),
                fg = colorRolFg(mensaje.autorRol),
                textSize = 12.sp
            )
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (esMio) Alignment.End else Alignment.Start) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        if (esMio) FrutAppColors.Brand600 else Color.White,
                        RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (esMio) 14.dp else 4.dp,
                            bottomEnd = if (esMio) 4.dp else 14.dp,
                        )
                    )
                    .padding(
                        // Imagen ocupa toda la burbuja (sin padding lateral)
                        horizontal = if (mensaje.imagenUrl != null && mensaje.cuerpo.isBlank()) 4.dp else 12.dp,
                        vertical = if (mensaje.imagenUrl != null && mensaje.cuerpo.isBlank()) 4.dp else 8.dp
                    )
            ) {
                if (mensaje.imagenUrl != null) {
                    ImagenRemota(
                        url = mensaje.imagenUrl!!,
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .heightIn(min = 120.dp, max = 280.dp)
                    )
                    if (mensaje.cuerpo.isNotBlank()) Spacer(Modifier.height(6.dp))
                }
                if (mensaje.cuerpo.isNotBlank()) {
                    Text(
                        mensaje.cuerpo,
                        color = if (esMio) Color.White else FrutAppColors.Ink,
                        fontSize = 14.sp,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    horaCorta(mensaje.createdAt),
                    color = FrutAppColors.InkSoft,
                    fontSize = 10.sp
                )
                if (esMio) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (mensaje.leidoEn != null) Icons.Filled.DoneAll else Icons.Filled.Done,
                        contentDescription = if (mensaje.leidoEn != null) "Leído" else "Enviado",
                        tint = if (mensaje.leidoEn != null) FrutAppColors.Brand600 else FrutAppColors.InkSoft,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        if (esMio) {
            Spacer(Modifier.width(6.dp))
            // Espacio simetrico al avatar, sin avatar (el usuario sabe quien es).
            Spacer(Modifier.size(28.dp))
        }
    }
}

@Composable
private fun PreviewImagen(bytes: ByteArray, onQuitar: () -> Unit) {
    val img = remember(bytes) { decodeImagen(bytes) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FrutAppColors.Brand50)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.Black, RoundedCornerShape(10.dp))
                .border(1.dp, FrutAppColors.Brand200, RoundedCornerShape(10.dp))
        ) {
            if (img != null) {
                Image(
                    bitmap = img,
                    contentDescription = "Adjunto",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Imagen lista para enviar", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("Toca enviar o cancela", color = FrutAppColors.InkSoft, fontSize = 11.sp)
        }
        IconButton(onClick = onQuitar) {
            Icon(Icons.Filled.Close, "Quitar", tint = FrutAppColors.InkSoft)
        }
    }
}

/** Descarga + decodifica una imagen de URL presignada y la muestra. Cache de
 *  decodificado por URL para que reabrir el chat no vuelva a descargar.
 *  Para uso de chat: las URLs son cortas (~horas) y los mensajes son pocos
 *  comparado con el catalogo, asi que un mapa en memoria alcanza. */
@Composable
private fun ImagenRemota(url: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = ChatImagenCache.get(url), url) {
        if (value != null) return@produceState
        runCatching {
            val bytes = Imagenes.descargar(url)
            decodeImagen(bytes)
        }
            .onSuccess { img ->
                if (img != null) {
                    ChatImagenCache.put(url, img)
                    value = img
                }
            }
            .onFailure { /* deja null; el placeholder queda */ }
    }
    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val img = bitmap
        if (img == null) {
            CircularProgressIndicator(
                color = FrutAppColors.Brand400,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        } else {
            Image(
                bitmap = img,
                contentDescription = "Imagen del mensaje",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Cache de bitmaps decodificados de imagenes de chat. Vive el proceso. */
private object ChatImagenCache {
    private val cache = mutableMapOf<String, ImageBitmap>()
    fun get(url: String): ImageBitmap? = cache[url]
    fun put(url: String, bitmap: ImageBitmap) { cache[url] = bitmap }
}

/** Formato corto "HH:mm" desde un ISO 8601. Si falla el parseo, devuelve "". */
private fun horaCorta(iso: String): String = runCatching {
    val instant = Instant.parse(iso)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val h = local.hour.toString().padStart(2, '0')
    val m = local.minute.toString().padStart(2, '0')
    "$h:$m"
}.getOrDefault("")

/** Inicial de avatar: primero el rol (si el titulo es generico/vacio),
 *  si no la primera letra del titulo. Asi cliente abriendo chat con picker
 *  ve "S" (Seleccionador) por default, pero si pasamos un nombre real va ese. */
private fun inicialPorRol(rol: String, fallbackTitulo: String): String {
    val limpio = fallbackTitulo.trim().firstOrNull { it.isLetter() }
    if (limpio != null && fallbackTitulo.length > 3) return limpio.uppercase()
    return when (rol) {
        "cliente" -> "C"
        "picker" -> "S"        // Seleccionador de Frescura
        "repartidor" -> "R"
        else -> "?"
    }
}

private fun colorRolBg(rol: String): Color = when (rol) {
    "cliente" -> Color(0xFFDBEAFE)       // azul claro
    "picker" -> FrutAppColors.Brand50    // verde claro
    "repartidor" -> Color(0xFFFFEDD5)    // naranja claro
    else -> FrutAppColors.Brand50
}

private fun colorRolFg(rol: String): Color = when (rol) {
    "cliente" -> Color(0xFF1D4ED8)       // azul fuerte
    "picker" -> FrutAppColors.Brand600   // verde fuerte
    "repartidor" -> Color(0xFFEA580C)    // naranja fuerte
    else -> FrutAppColors.Brand600
}
