package cl.frutapp.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val ENC_FILE = "frutapp_session_enc"
private const val PLAIN_FILE = "frutapp_session"

actual object SessionStorage {
    private var prefs: SharedPreferences? = null

    /** Inicializa el almacenamiento cifrado. Si el Keystore/clave se corrompe, borra y
     *  reintenta; como último recurso usa prefs normal (la sesión funciona igual, sin
     *  cifrar) para NUNCA crashear por esto. Llamar una vez al arrancar (MainActivity). */
    fun init(context: Context) {
        if (prefs != null) return
        val ctx = context.applicationContext
        prefs = try {
            createEncrypted(ctx)
        } catch (e: Exception) {
            runCatching { ctx.deleteSharedPreferences(ENC_FILE) }
            runCatching { createEncrypted(ctx) }.getOrElse {
                ctx.getSharedPreferences(PLAIN_FILE, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncrypted(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx,
            ENC_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // commit() (síncrono) para que la sesión quede persistida aunque el proceso muera enseguida.
    actual fun putString(key: String, value: String) {
        prefs?.edit()?.putString(key, value)?.commit()
    }

    actual fun getString(key: String): String? = prefs?.getString(key, null)

    actual fun remove(key: String) {
        prefs?.edit()?.remove(key)?.commit()
    }
}
