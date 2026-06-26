package cl.frutapp.app.navigation.auth

import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.AuthApi
import cl.frutapp.app.navigation.homeForUser
import cl.frutapp.app.ui.components.AuthHeaderText
import cl.frutapp.shared.dto.LoginRequest
import kotlinx.coroutines.launch
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
        val scope = rememberCoroutineScope()
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        // Handler extraido para reusar entre el boton "Ingresar" y el Enter
        // del teclado en el campo password. Guard de loading evita doble-submit.
        val iniciarLogin: () -> Unit = handler@{
            if (loading) return@handler
            if (email.isBlank() || password.isBlank()) return@handler
            error = null
            loading = true
            scope.launch {
                runCatching { AuthApi().login(LoginRequest(email = email.trim(), password = password)) }
                    .onSuccess { resp ->
                        TokenStore.save(resp.accessToken, resp.refreshToken, resp.user)
                        // Reset del flag in-memory del coachmark: el usuario
                        // que loguea ahora puede ser distinto del anterior; sin
                        // esto, el flag del anterior bloquearia el tour del nuevo.
                        cl.frutapp.app.data.CoachmarkStore.resetMemoriaProceso()
                        navigator.replace(homeForUser(resp.user))
                    }
                    .onFailure { e ->
                        cl.frutapp.app.ui.ErrorReporter.report(screen = "Login", action = "login", error = e)
                        val msg = e.message.orEmpty().lowercase()
                        error = when {
                            msg.contains("verifica tu correo") || msg.contains("verifica el correo") ->
                                "Verifica tu correo: revisa tu inbox y haz clic en el botón del mail para activar la cuenta."
                            msg.contains("429") || msg.contains("too many") ->
                                "Demasiados intentos. Espera un momento y vuelve a intentar."
                            msg.contains("401") || msg.contains("unauthorized") || msg.contains("422") ->
                                "Correo o contraseña incorrectos."
                            else -> cl.frutapp.app.ui.mensajeAmigable(e, "iniciar sesión")
                        }
                    }
                loading = false
            }
        }

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
                    keyboardType = KeyboardType.Email,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                )
                FrutTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    onImeAction = { iniciarLogin() },
                )
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    FrutButtonGhost(text = "¿Olvidaste tu contraseña?", onClick = { navigator.push(RecoverPasswordScreen()) })
                }
                if (error != null) {
                    Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp)
                }
                FrutButtonPrimary(
                    text = if (loading) "Ingresando…" else "Ingresar",
                    enabled = !loading,
                    onClick = iniciarLogin
                )
                FrutButtonOutline(text = "Crear cuenta", onClick = { navigator.push(RegisterScreen()) })
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OrDivider()
                SocialButtons(onGoogle = {}, onApple = {})
            }

            // Fondo blanco semi-opaco + padding p/ que se lea siempre encima de las
            // frutas decorativas del AuthScaffold (BottomFruits). Antes el texto quedaba
            // con plátano/manzana/etc. detrás y era ilegible.
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
                    .background(Color.White.copy(alpha = 0.92f), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Al continuar, aceptas nuestros Términos y Condiciones y Política de Privacidad.",
                    color = FrutAppColors.InkSoft,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
