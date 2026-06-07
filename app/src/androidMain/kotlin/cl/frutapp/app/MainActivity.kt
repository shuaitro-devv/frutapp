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
        // Sesión persistida: inicializar storage y restaurar antes de pintar la UI.
        SessionStorage.init(applicationContext)
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
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        BiometricAuth.unbind()
        super.onDestroy()
    }
}
