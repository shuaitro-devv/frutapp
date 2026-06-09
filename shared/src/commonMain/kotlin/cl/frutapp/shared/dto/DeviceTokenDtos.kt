package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Registrar/actualizar token de FCM del device actual. El usuario sale del JWT;
 *  el body solo trae el token + metadata del device. */
@Serializable
data class RegisterDeviceTokenRequest(
    val fcmToken: String,
    /** ANDROID / IOS / WEB. Por ahora solo ANDROID. */
    val platform: String,
    /** applicationId (cl.frutapp.app, cl.frutapp.app.debug, cl.frutapp.app.sofruco...).
     *  Permite enviar push solo a la app del brand activo. */
    val appId: String? = null
)

@Serializable
data class DeleteDeviceTokenRequest(
    val fcmToken: String
)
