package cl.frutapp.app.navigation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.navigation.PlaceholderHomeScreen
import cl.frutapp.app.ui.components.AuthHeaderText
import cl.frutapp.app.ui.components.AuthScaffold
import cl.frutapp.app.ui.components.FrutButtonGhost
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.components.OrDivider
import cl.frutapp.app.ui.components.SocialButtons
import cl.frutapp.app.ui.theme.FrutAppColors

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        AuthScaffold {
            AuthHeaderText(
                title = "¡Bienvenido!",
                subtitle = "Inicia sesión para disfrutar de productos frescos, ofertas y beneficios exclusivos."
            )
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FrutTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico o teléfono",
                    leadingIcon = Icons.Default.Person,
                    keyboardType = KeyboardType.Email
                )
                FrutTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    FrutButtonGhost(text = "¿Olvidaste tu contraseña?", onClick = { navigator.push(RecoverPasswordScreen()) })
                }
                FrutButtonPrimary(text = "Ingresar", onClick = { navigator.replace(PlaceholderHomeScreen()) })
                FrutButtonOutline(text = "Crear cuenta", onClick = { navigator.push(RegisterScreen()) })
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OrDivider()
                SocialButtons(onGoogle = {}, onApple = {})
            }

            Text(
                text = "Al continuar, aceptas nuestros Términos y Condiciones y Política de Privacidad.",
                color = FrutAppColors.InkSoft,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, start = 12.dp, end = 12.dp)
            )
        }
    }
}
