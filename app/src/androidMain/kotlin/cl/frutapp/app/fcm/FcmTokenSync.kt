package cl.frutapp.app.fcm

import android.content.Context
import cl.frutapp.app.BuildConfig
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.DeviceTokenApi
import cl.frutapp.shared.dto.RegisterDeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Puente entre el SDK de FCM y nuestro backend. Tres entradas:
 *
 * 1. [sendIfLoggedIn] — el messaging service lo llama cuando llega [onNewToken];
 *    si hay sesion activa, sube el token al backend.
 * 2. [ensureRegisteredOnLogin] — MainActivity / LoginScreen lo llama tras login
 *    exitoso: si tenemos token local (guardado por onNewToken antes del login),
 *    lo subimos. Si no hay, lo pedimos al SDK con await y lo subimos.
 * 3. [unregister] — al hacer logout, sacamos el token del backend para que
 *    pushes de pedidos del usuario anterior no lleguen al sucesor en el mismo
 *    celu (igual lo gestionamos del lado server con UNIQUE(fcm_token), pero
 *    asi tambien hay limpieza del lado cliente).
 */
object FcmTokenSync {
    private const val TAG = "FcmTokenSync"
    private val api = DeviceTokenApi()

    /** Llamado desde [FrutAppMessagingService.onNewToken]. */
    suspend fun sendIfLoggedIn(context: Context, token: String) {
        if (!TokenStore.isLoggedIn) {
            Log.d(TAG, "Token FCM recibido sin sesion activa, queda en local")
            return
        }
        sendToBackend(token)
    }

    /** Llamado tras login exitoso (App.kt). Garantiza que el token esta en el server. */
    suspend fun ensureRegisteredOnLogin(context: Context) {
        val local = FcmTokenStore.readLocal(context)
        val token = local ?: runCatching {
            FirebaseMessaging.getInstance().token.await()
        }.getOrNull()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "No pude obtener token de FCM tras login")
            return
        }
        if (local == null) FcmTokenStore.saveLocal(context, token)
        sendToBackend(token)
    }

    /** Llamado tras logout. [jwt] viene capturado sincronicamente por
     *  [FcmBridge.onLogoutSuccess] ANTES de que TokenStore.clear lo nulee — sin
     *  ese snapshot, el DELETE saldria sin Authorization y el server respondria
     *  401, dejando el token huerfano. */
    suspend fun unregister(context: Context, jwt: String?) {
        val token = FcmTokenStore.readLocal(context) ?: return
        runCatching { api.delete(token, jwt) }
            .onFailure { Log.w(TAG, "No pude borrar token en backend: ${it.message}") }
        FcmTokenStore.clearLocal(context)
    }

    private suspend fun sendToBackend(token: String) {
        runCatching {
            api.register(
                RegisterDeviceTokenRequest(
                    fcmToken = token,
                    platform = "ANDROID",
                    appId = BuildConfig.APPLICATION_ID
                )
            )
        }.onSuccess {
            Log.i(TAG, "Token FCM registrado en backend para ...${token.takeLast(10)}")
        }.onFailure {
            Log.w(TAG, "No pude registrar token FCM: ${it.message}")
        }
    }
}
