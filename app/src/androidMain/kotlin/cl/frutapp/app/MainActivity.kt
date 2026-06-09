package cl.frutapp.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cl.frutapp.app.data.BiometricAuth
import cl.frutapp.app.data.SessionStorage
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.fcm.FcmBridge
import cl.frutapp.app.ui.initToast
import cl.frutapp.app.ui.theme.ActiveBrand

class MainActivity : FragmentActivity() {

    /** Launcher para pedir POST_NOTIFICATIONS en Android 13+. El resultado no se
     *  usa de inmediato: aunque diga "no", FCM igual recibe el push (solo la
     *  notificacion visible queda bloqueada — se puede reactivar desde Ajustes). */
    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> /* no-op: el usuario puede activar despues desde Ajustes del sistema */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Sesión persistida: inicializar storage y restaurar antes de pintar la UI.
        SessionStorage.init(applicationContext)
        // FCM bridge: cuelga el applicationContext para que TokenStore (commonMain)
        // pueda disparar registro/baja del token tras login/logout sin importar Android.
        FcmBridge.attach(applicationContext)
        // White-label: si el usuario eligio un brand desde Perfil, respetar ese
        // override; sino usar el BRAND_ID que el flavor dejo en BuildConfig.
        // Setear ANTES de setContent para que la primera composicion ya use la
        // paleta correcta y no haya flash de colores.
        ActiveBrand.set(ActiveBrand.persistedOverride() ?: BuildConfig.BRAND_ID)
        TokenStore.restore()
        initToast(applicationContext)
        BiometricAuth.bind(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationsPermissionIfNeeded()
        setContent {
            App()
        }
    }

    /** En Android 13+ (API 33) la app debe pedir POST_NOTIFICATIONS para que el
     *  sistema muestre las notificaciones que FCM recibe. Pre-13 esta concedido por
     *  default. Lo pedimos al arrancar (mejor que justo despues del login: el
     *  prompt va al primer onboarding y queda asociado al "te avisaremos del pedido"). */
    private fun requestNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onDestroy() {
        BiometricAuth.unbind()
        super.onDestroy()
    }
}
