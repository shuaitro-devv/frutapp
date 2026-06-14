package cl.frutapp.app.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import cl.frutapp.app.MainActivity
import cl.frutapp.app.R
import cl.frutapp.app.data.NotificacionesStore
import cl.frutapp.app.data.TipoNotificacion
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Recibe los pushes de FCM. Android arranca este servicio aunque la app este
 * cerrada (no force-stop). Dos callbacks que importan:
 *
 *  - onNewToken: el SDK regenero/rota el token. Lo persistimos local y, si hay
 *    sesion activa, lo subimos al backend (POST /v1/device/token via FcmTokenSync).
 *  - onMessageReceived: llego un mensaje. Si trae notification payload Android lo
 *    muestra solo cuando la app esta en background; cuando esta en foreground o
 *    es data-only, lo construimos nosotros con NotificationCompat. El deeplink al
 *    pedido va via intent extra "orderId".
 */
class FrutAppMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        FcmTokenStore.saveLocal(applicationContext, token)
        // Sin lazy: el sync chequea si hay sesion adentro y vuelve si no la hay.
        scope.launch { FcmTokenSync.sendIfLoggedIn(applicationContext, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        ensureChannel(applicationContext)
        // Titulo/cuerpo: del payload "notification" si vino, sino del payload "data"
        // (el dispatcher backend manda ambos; data tiene fallback con la misma copy).
        val title = message.notification?.title ?: message.data["title"] ?: "FrutApp"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val orderId = message.data["orderId"]
        // Tambien propagamos type (order_status, picker_ajuste_resuelto, ...) y
        // status (ESPERANDO_AJUSTE_CLIENTE, EN_DESPACHO, ...) para que el deep
        // link sepa si llevarte al ajuste, al tracking, o a la cola del rol.
        val type = message.data["type"]
        val status = message.data["status"]
        showNotification(applicationContext, title, body, orderId, type, status)
        // Persiste tambien en el inbox in-app: la pantalla Notificaciones lee de
        // NotificacionesStore. Mapea el data["type"] del dispatcher al enum local
        // para que el icono de cada notif tenga sentido (caja para pedido, regalo
        // para promo, etc). Defecto a PEDIDO porque hoy todos los push del backend
        // son de tipo order_status / picker_new_order / repartidor_new_dispatch.
        val tipo = when (message.data["type"]) {
            "picker_new_order", "repartidor_new_dispatch", "order_status" -> TipoNotificacion.PEDIDO
            "coins" -> TipoNotificacion.COINS
            "recicla" -> TipoNotificacion.RECICLA
            "racha" -> TipoNotificacion.RACHA
            "promo" -> TipoNotificacion.PROMO
            else -> TipoNotificacion.PEDIDO
        }
        NotificacionesStore.add(titulo = title, detalle = body, tipo = tipo)
    }
}

private val notifIdSeq = AtomicInteger(1000)

internal const val CHANNEL_ID_ORDERS = "frutapp_orders"

internal fun ensureChannel(context: Context) {
    val nm = context.getSystemService(NotificationManager::class.java) ?: return
    if (nm.getNotificationChannel(CHANNEL_ID_ORDERS) != null) return
    val channel = NotificationChannel(
        CHANNEL_ID_ORDERS,
        "Estado de tus pedidos",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Avisos cuando avanza tu pedido (en preparación, en camino, entregado)."
    }
    nm.createNotificationChannel(channel)
}

internal fun showNotification(
    context: Context,
    title: String,
    body: String,
    orderId: String?,
    type: String? = null,
    status: String? = null
) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        if (orderId != null) putExtra("orderId", orderId)
        if (type != null) putExtra("type", type)
        if (status != null) putExtra("status", status)
    }
    val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    val pending = PendingIntent.getActivity(
        context,
        orderId?.hashCode() ?: 0,
        intent,
        pendingFlags
    )
    val notif = NotificationCompat.Builder(context, CHANNEL_ID_ORDERS)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pending)
        .build()

    val nm = context.getSystemService(NotificationManager::class.java) ?: return
    // collapse_key del backend hace que el sistema reemplace; usamos id por orderId
    // para que multiples updates del mismo pedido NO acumulen 4 notifs en la barra.
    // Usamos los 128 bits del UUID combinados con XOR en vez de String.hashCode()
    // — el hashCode de un UUID-string esta sujeto a clustering (cadenas largas
    // similares colisionan mas) y un colision aqui significa que la noti del
    // pedido X tapa la del pedido Y. Si no es UUID valido (formato inesperado)
    // caemos al hashCode como fallback.
    val notifId = if (orderId != null) {
        runCatching {
            val uuid = java.util.UUID.fromString(orderId)
            (uuid.mostSignificantBits xor uuid.leastSignificantBits).toInt()
        }.getOrNull() ?: orderId.hashCode()
    } else notifIdSeq.incrementAndGet()
    nm.notify(notifId, notif)
}
