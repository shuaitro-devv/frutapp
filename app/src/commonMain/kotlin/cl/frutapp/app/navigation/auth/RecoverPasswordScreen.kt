package cl.frutapp.app.navigation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.AuthApi
import cl.frutapp.app.ui.components.AuthHeaderText
import cl.frutapp.app.ui.components.AuthScaffold
import cl.frutapp.app.ui.components.FrutButtonGhost
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutCard
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.ForgotPasswordRequest
import kotlinx.coroutines.launch

class RecoverPasswordScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var email by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        AuthScaffold(showBackButton = true, onBack = { navigator.pop() }) {
            AuthHeaderText(
                title = "Recupera tu contraseña",
                subtitle = "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña."
            )
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FrutTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )
                FrutButtonPrimary(
                    text = if (loading) "Enviando…" else "Enviar código de recuperación",
                    enabled = email.isNotBlank() && !loading,
                    onClick = {
                        error = null
                        loading = true
                        val correo = email.trim()
                        scope.launch {
                            runCatching { AuthApi().forgotPassword(ForgotPasswordRequest(email = correo)) }
                                .onSuccess { navigator.push(ResetPasswordScreen(email = correo)) }
                                .onFailure { e ->
                                    cl.frutapp.app.ui.ErrorReporter.report(screen = "RecoverPassword", action = "forgot_password", error = e)
                                    error = cl.frutapp.app.ui.mensajeAmigable(e, "enviar el código")
                                }
                            loading = false
                        }
                    }
                )
                if (error != null) {
                    Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Text(text = "o", color = FrutAppColors.InkSoft, fontSize = 13.sp, modifier = Modifier.padding(vertical = 14.dp))

            FrutCard(containerColor = FrutAppColors.Brand50, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = FrutAppColors.Brand400,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("Tu seguridad es importante", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Te enviaremos un enlace seguro para que puedas restablecer tu contraseña.",
                            color = FrutAppColors.InkMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), contentAlignment = Alignment.Center) {
                FrutButtonGhost(text = "← Volver al inicio de sesión", onClick = { navigator.pop() })
            }
        }
    }
}
