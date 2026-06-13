package cl.frutapp.app.navigation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import cl.frutapp.app.data.PendingNotification
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.NotificacionesStore
import cl.frutapp.app.data.remote.NotificationApi
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.NotificationDto
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Inbox de notificaciones del usuario. Source of truth es el backend
 * (`GET /v1/notifications`). Al abrir la pantalla pega al API; cuando vuelven
 * los items, dispara `POST /v1/notifications/read-all` para vaciar el badge
 * (mismo UX que antes con el store local). Si el GET falla, muestra estado
 * de error amigable y permite reintentar.
 */
class NotificacionesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val api = remember { NotificationApi() }
        val scope = rememberCoroutineScope()
        var loading by remember { mutableStateOf(true) }
        var items by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            loading = true
            errorMsg = null
            runCatching { api.list() }
                .onSuccess { resp ->
                    items = resp.items
                    loading = false
                    // Vacia el badge local: el inbox ya esta abierto, no hace
                    // falta seguir mostrando los push como pendientes.
                    NotificacionesStore.resetAll()
                    // Marcar todas leídas en background — no bloquea el render.
                    if (resp.unreadCount > 0) {
                        scope.launch {
                            runCatching { api.markAllRead() }
                                .onFailure { e ->
                                    if (e is kotlinx.coroutines.CancellationException) throw e
                                    ErrorReporter.report(screen = "Notificaciones", action = "mark_all_read", error = e)
                                }
                        }
                    }
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    ErrorReporter.report(screen = "Notificaciones", action = "list", error = e)
                    errorMsg = "No pudimos cargar tus notificaciones. Tocá para reintentar."
                    loading = false
                }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
                    }
                    Text("Notificaciones", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                when {
                    loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FrutAppColors.Brand400)
                    }
                    errorMsg != null -> Box(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️", fontSize = 36.sp)
                            Text(errorMsg!!, color = FrutAppColors.InkMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    items.isEmpty() -> Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                        EmptyNotis()
                    }
                    else -> Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                        items.forEach { noti ->
                            // Si la noti es de tipo PEDIDO y tiene orderId en data,
                            // hacerla clickeable: publicamos en PendingNotification y
                            // dejamos que el LaunchedEffect global de App.kt navegue al
                            // pedido/ajuste/picker/repartidor segun el contexto.
                            // Mismo comportamiento que tocar el push en la barra del
                            // sistema → el inbox in-app es la otra puerta a la misma
                            // pantalla.
                            val triple = parseNotiData(noti.data)
                            val onClick: (() -> Unit)? = triple?.let { (orderId, type, status) ->
                                {
                                    PendingNotification.set(orderId, type, status)
                                    navigator.pop()
                                }
                            }
                            NotiRow(noti, onClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotiRow(n: NotificationDto, onClick: (() -> Unit)? = null) {
    val emoji = emojiFor(n.type)
    val cuandoHumano = humanizeIsoToRelative(n.createdAt)
    val baseModifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
        .background(if (n.leida) FrutAppColors.Brand50 else FrutAppColors.AmberSoft.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    Row(
        modifier = if (onClick != null) baseModifier.clickable { onClick() }.padding(14.dp) else baseModifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.title, color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                if (!n.leida) {
                    Box(modifier = Modifier.padding(start = 8.dp).size(8.dp).background(FrutAppColors.AmberCoin, CircleShape))
                }
            }
            Text(n.body, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Text(cuandoHumano, color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Mapeo type backend → emoji para el avatar circular. */
private fun emojiFor(type: String): String = when (type) {
    "PEDIDO" -> "📦"
    "COINS" -> "🪙"
    "RECICLA" -> "♻️"
    "RACHA" -> "🔥"
    "PROMO" -> "🎁"
    else -> "🔔"
}

/** Convierte un timestamp ISO-8601 (lo que devuelve Instant.toString()) en
 *  texto humano corto: "hace 5 min" / "hace 2 h" / "hace 3 días". Parseo
 *  minimo sin libs nuevas. Si el formato no matchea, devolvemos el string
 *  original truncado. */
private fun humanizeIsoToRelative(iso: String): String {
    val parsed = runCatching { kotlinx.datetime.Instant.parse(iso) }.getOrNull()
        ?: return iso.take(16)
    val now = kotlinx.datetime.Clock.System.now()
    val diff = now - parsed
    val mins = diff.inWholeMinutes
    return when {
        mins < 1 -> "ahora"
        mins < 60 -> "hace $mins min"
        mins < 60 * 24 -> "hace ${mins / 60} h"
        else -> "hace ${mins / (60 * 24)} días"
    }
}

/**
 * Parsea el `data` JSON de la notificacion del inbox (lo guarda el backend al
 * persistirla con `NotificationInboxRepository.create`). Devuelve un Triple con
 * (orderId, type, status) listo para [PendingNotification.set]. Null si no hay
 * orderId — esa noti no es clickeable (ej. una de COINS o RACHA sin pedido).
 *
 * El `data` que guarda el backend tiene shapes distintos segun el origen:
 *  - order_status (cliente): {"orderId":"...","orderNumero":"...","status":"..."}
 *  - picker_new_order: {"orderId":"...","orderNumero":"...","scope":"picker"}
 *  - picker_ajuste_resuelto: idem + "ajuste":"aprobado|rechazado"
 *  - repartidor_new_dispatch: {"orderId":"...","orderNumero":"...","scope":"repartidor"}
 *
 * Mapeamos scope/ajuste al `type` que entiende el routing de App.kt para que el
 * tap del inbox dispare la misma navegacion que el tap del push de la barra.
 */
private val notiDataJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

private fun parseNotiData(data: String?): Triple<String, String?, String?>? {
    if (data.isNullOrBlank()) return null
    return runCatching {
        val obj = notiDataJson.parseToJsonElement(data) as? kotlinx.serialization.json.JsonObject
            ?: return@runCatching null
        val orderId = (obj["orderId"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: return@runCatching null
        val scope = (obj["scope"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        val ajuste = (obj["ajuste"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        val status = (obj["status"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        val type = when {
            scope == "picker" && ajuste != null -> "picker_ajuste_resuelto"
            scope == "picker" -> "picker_new_order"
            scope == "repartidor" -> "repartidor_new_dispatch"
            else -> "order_status"
        }
        Triple(orderId, type, status)
    }.getOrNull()
}

@Composable
private fun EmptyNotis() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔕", fontSize = 44.sp)
        Text("Sin notificaciones nuevas", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text("Te avisaremos cuando algo importante pase con tus pedidos, coins o huella.", color = FrutAppColors.InkMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
