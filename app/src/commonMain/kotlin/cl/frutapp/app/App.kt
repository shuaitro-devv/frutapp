package cl.frutapp.app

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import cafe.adriel.voyager.navigator.Navigator
import cl.frutapp.app.navigation.SplashScreen
import cl.frutapp.app.ui.theme.FrutAppTheme

@Composable
fun App() {
    FrutAppTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // Voyager Navigator: el grafo de navegación arranca en Splash.
            // Cada pantalla se irá agregando a medida que lleguen los mockups.
            Navigator(SplashScreen())
        }
    }
}
