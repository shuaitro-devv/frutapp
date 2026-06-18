package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.ChatApi
import cl.frutapp.app.data.remote.ChatWsClient
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.ChatMensajeDto
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
 */
class ChatScreen(
    private val orderId: String,
    private val destinatarioRol: String,  // "picker" / "repartidor" / "cliente"
    private val tituloContraparte: String, // ej "Tu Seleccionador de Frescura"
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val api = remember { ChatApi() }
        val ws = remember { ChatWsClient() }
        var mensajes by remember { mutableStateOf<List<ChatMensajeDto>>(emptyList()) }
        var entrada by remember { mutableStateOf("") }
        var enviando by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

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

        Column(modifier = Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Column {
                    Text(tituloContraparte, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("En este pedido", color = FrutAppColors.InkSoft, fontSize = 11.sp)
                }
            }

            // Lista de mensajes
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(FrutAppColors.Brand50.copy(alpha = 0.3f))) {
                if (mensajes.isEmpty() && error == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Aún no hay mensajes.\nEscribe el primero abajo.",
                            color = FrutAppColors.InkSoft,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.Bottom
            ) {
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (entrada.isEmpty()) {
                        Text("Escribe un mensaje…", color = FrutAppColors.InkSoft, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (entrada.isNotBlank() && !enviando) FrutAppColors.Brand600 else FrutAppColors.Brand200,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            if (entrada.isBlank() || enviando) return@IconButton
                            val txt = entrada.trim()
                            enviando = true
                            scope.launch {
                                runCatching { api.enviar(orderId, destinatarioRol, txt) }
                                    .onSuccess { dto ->
                                        if (mensajes.none { it.id == dto.id }) {
                                            mensajes = mensajes + dto
                                        }
                                        entrada = ""
                                    }
                                    .onFailure { e ->
                                        if (e is kotlinx.coroutines.CancellationException) throw e
                                        error = "No se pudo enviar."
                                        ErrorReporter.report(screen = "Chat", action = "enviar", error = e)
                                    }
                                enviando = false
                            }
                        },
                        enabled = entrada.isNotBlank() && !enviando,
                    ) {
                        Icon(Icons.Filled.Send, "Enviar", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MensajeBurbuja(mensaje: ChatMensajeDto, esMio: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (esMio) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (esMio) FrutAppColors.Brand600 else Color.White,
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (esMio) 14.dp else 2.dp,
                        bottomEnd = if (esMio) 2.dp else 14.dp,
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                mensaje.cuerpo,
                color = if (esMio) Color.White else FrutAppColors.Ink,
                fontSize = 14.sp,
            )
        }
    }
}
