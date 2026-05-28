package cl.frutapp.app.navigation.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.AuthApi
import cl.frutapp.app.ui.components.AuthHeaderText
import cl.frutapp.app.ui.components.AuthScaffold
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.components.OtpInput
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.ResetPasswordRequest
import kotlinx.coroutines.launch

class ResetPasswordScreen(private val email: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var code by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        AuthScaffold(showBackButton = true, onBack = { navigator.pop() }) {
            AuthHeaderText(
                title = "Nueva contraseña",
                subtitle = "Ingresa el código que enviamos a $email y tu nueva contraseña."
            )

            OtpInput(value = code, onValueChange = { code = it }, modifier = Modifier.padding(top = 24.dp))

            Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FrutTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Nueva contraseña",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )
                Text(
                    text = "Mínimo 6 caracteres, incluye letras y números.",
                    color = FrutAppColors.InkSoft,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                FrutTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = "Confirmar contraseña",
                    leadingIcon = Icons.Default.Lock,
                    isPassword = true
                )
            }

            if (error != null) {
                Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
            }

            FrutButtonPrimary(
                text = if (loading) "Guardando…" else "Restablecer contraseña",
                enabled = code.length == 6 && password.isNotEmpty() && confirm == password && !loading,
                modifier = Modifier.padding(top = 20.dp),
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        runCatching {
                            AuthApi().resetPassword(
                                ResetPasswordRequest(email = email, code = code, newPassword = password)
                            )
                        }
                            .onSuccess { navigator.popUntilRoot() }
                            .onFailure { error = "Código inválido o expirado. Pídelo de nuevo." }
                        loading = false
                    }
                }
            )

            Text(
                text = "Volver al inicio de sesión",
                color = FrutAppColors.Brand600,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .clickable { navigator.popUntilRoot() }
            )
        }
    }
}
