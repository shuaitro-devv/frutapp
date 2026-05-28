package cl.frutapp.app.navigation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.AuthApi
import cl.frutapp.app.ui.components.AuthHeaderText
import cl.frutapp.app.ui.components.AuthScaffold
import cl.frutapp.app.ui.components.FrutButtonGhost
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.components.OrDivider
import cl.frutapp.app.ui.components.SocialButtons
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.RegisterRequest
import kotlinx.coroutines.launch

class RegisterScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        var acceptTerms by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }

        AuthScaffold(showBackButton = true, onBack = { navigator.pop() }) {
            AuthHeaderText(
                title = "Crear cuenta",
                subtitle = "Completa tus datos para comenzar a disfrutar de productos frescos, ofertas y beneficios exclusivos."
            )
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FrutTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Nombre completo",
                    leadingIcon = Icons.Default.Person
                )
                FrutTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )
                FrutTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Número de teléfono (+56)",
                    leadingIcon = Icons.Default.Phone,
                    keyboardType = KeyboardType.Phone
                )
                FrutTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Crear contraseña",
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

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptTerms,
                    onCheckedChange = { acceptTerms = it },
                    colors = CheckboxDefaults.colors(checkedColor = FrutAppColors.Brand400)
                )
                Text(
                    text = "Acepto los Términos y Condiciones y la Política de Privacidad.",
                    color = FrutAppColors.InkMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            if (error != null) {
                Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            FrutButtonPrimary(
                text = if (loading) "Creando…" else "Crear cuenta",
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        runCatching {
                            AuthApi().register(
                                RegisterRequest(
                                    name = name.trim(),
                                    email = email.trim(),
                                    phone = phone.trim().ifBlank { null },
                                    password = password
                                )
                            )
                        }
                            .onSuccess { resp ->
                                TokenStore.save(resp.accessToken, resp.refreshToken, resp.user)
                                navigator.push(VerifyCodeScreen(email = email.trim().ifBlank { "correo@ejemplo.com" }))
                            }
                            .onFailure {
                                error = "No pudimos crear la cuenta. Revisa el correo (puede estar registrado) y la contraseña (mín. 6, con letras y números)."
                            }
                        loading = false
                    }
                },
                enabled = acceptTerms && !loading && password.isNotEmpty() && confirm == password,
                modifier = Modifier.padding(top = 8.dp)
            )

            Column(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OrDivider()
                SocialButtons(onGoogle = {}, onApple = {})
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Ya tienes cuenta? ", color = FrutAppColors.InkMuted, fontSize = 14.sp)
                FrutButtonGhost(text = "Inicia sesión", onClick = { navigator.pop() })
            }
        }
    }
}
