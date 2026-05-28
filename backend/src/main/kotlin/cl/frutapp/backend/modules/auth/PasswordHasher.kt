package cl.frutapp.backend.modules.auth

import at.favre.lib.crypto.bcrypt.BCrypt

/** Hashing de contraseñas con bcrypt (cost 12). */
object PasswordHasher {
    private const val COST = 12

    fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(COST, password.toCharArray())

    fun verify(password: String, hash: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), hash).verified
}
