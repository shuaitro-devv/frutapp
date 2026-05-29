package cl.frutapp.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import cl.frutapp.app.data.BiometricAuth
import cl.frutapp.app.data.SessionStorage
import cl.frutapp.app.data.TokenStore

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Sesión persistida: inicializar storage y restaurar antes de pintar la UI.
        SessionStorage.init(applicationContext)
        TokenStore.restore()
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
