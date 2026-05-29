package cl.frutapp.app.data

import android.content.Context
import android.content.SharedPreferences

actual object SessionStorage {
    private var prefs: SharedPreferences? = null

    /** Debe llamarse una vez al arrancar (MainActivity) antes de usar la sesión. */
    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("frutapp_session", Context.MODE_PRIVATE)
        }
    }

    actual fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.apply()
    }

    actual fun getString(key: String): String? = prefs?.getString(key, null)

    actual fun remove(key: String) {
        prefs?.edit()?.remove(key)?.apply()
    }
}
