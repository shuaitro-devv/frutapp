package cl.frutapp.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import cl.frutapp.app.data.BiometricAuth
import cl.frutapp.app.data.SessionStorage
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.ui.initToast
import cl.frutapp.app.ui.theme.ActiveBrand

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // White-label: el flavor inyecta BRAND_ID en BuildConfig. Setear ActiveBrand
        // ANTES de setContent para que el color scheme construido en FrutAppTheme
        // tome la paleta correcta desde la primera composicion.
        ActiveBrand.set(BuildConfig.BRAND_ID)
        // Sesión persistida: inicializar storage y restaurar antes de pintar la UI.
        SessionStorage.init(applicationContext)
        TokenStore.restore()
        initToast(applicationContext)
        BiometricAuth.bind(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        BiometricAuth.unbind()
        super.onDestroy()
    }
}
