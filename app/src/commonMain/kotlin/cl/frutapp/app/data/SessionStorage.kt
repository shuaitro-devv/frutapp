package cl.frutapp.app.data

/**
 * Almacenamiento clave-valor persistente del dispositivo (expect/actual). En Android
 * usa SharedPreferences. Para el demo guarda en claro; en producción debería usar
 * almacenamiento cifrado (EncryptedSharedPreferences / Keystore).
 */
expect object SessionStorage {
    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
}
