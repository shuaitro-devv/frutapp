package cl.frutapp.app.fcm

import android.content.Context

/**
 * Persistencia local del token de FCM. Soluciona el caso "el usuario instala la
 * app, se loguea horas despues": el SDK emite onNewToken al instalar (sin sesion);
 * lo guardamos en disco, y al hacer login mandamos el guardado.
 *
 * Esto es SharedPreferences plana — el token de FCM no es secreto (lo extrae
 * cualquiera que tenga el APK) asi que no necesita EncryptedSharedPreferences.
 */
object FcmTokenStore {
    private const val PREFS = "fcm_token_prefs"
    private const val KEY_TOKEN = "fcm_token"

    fun saveLocal(context: Context, token: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    fun readLocal(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

    fun clearLocal(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TOKEN)
            .apply()
    }
}
