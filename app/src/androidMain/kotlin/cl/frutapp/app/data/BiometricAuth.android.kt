package cl.frutapp.app.data

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference

actual object BiometricAuth {
    /** Referencia débil para no filtrar la Activity (el object vive todo el proceso). */
    private var activityRef: WeakReference<FragmentActivity>? = null

    fun bind(activity: FragmentActivity) {
        activityRef = WeakReference(activity)
    }

    fun unbind() {
        activityRef = null
    }

    private fun activity(): FragmentActivity? = activityRef?.get()

    private const val ALLOWED =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    actual fun isAvailable(): Boolean {
        val act = activity() ?: return false
        return BiometricManager.from(act).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS
    }

    actual fun authenticate(onSuccess: () -> Unit, onError: () -> Unit) {
        val act = activity() ?: run { onError(); return }
        val prompt = BiometricPrompt(
            act,
            ContextCompat.getMainExecutor(act),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError()
                // onAuthenticationFailed (un intento erróneo): no navegamos, el usuario reintenta.
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquea FrutApp")
            .setSubtitle("Usa tu huella para entrar")
            .setNegativeButtonText("Usar contraseña")
            .build()
        prompt.authenticate(info)
    }
}
