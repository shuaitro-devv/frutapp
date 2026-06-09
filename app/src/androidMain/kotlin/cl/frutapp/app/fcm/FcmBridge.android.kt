package cl.frutapp.app.fcm

import android.annotation.SuppressLint
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

actual object FcmBridge {
    /** Application context, seteado al arranque por [attach]. SuppressLint OK:
     *  es el ApplicationContext, no Activity. */
    @SuppressLint("StaticFieldLeak")
    @Volatile private var appContext: Context? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Llamado en MainActivity.onCreate(applicationContext). */
    fun attach(context: Context) {
        appContext = context.applicationContext
    }

    actual fun onLoginSuccess() {
        val ctx = appContext ?: return
        scope.launch { FcmTokenSync.ensureRegisteredOnLogin(ctx) }
    }

    actual fun onLogoutSuccess(jwt: String?) {
        val ctx = appContext ?: return
        scope.launch { FcmTokenSync.unregister(ctx, jwt) }
    }
}
