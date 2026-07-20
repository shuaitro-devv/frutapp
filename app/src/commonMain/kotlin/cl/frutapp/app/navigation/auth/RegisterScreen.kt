package cl.frutapp.app.navigation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.remote.AuthApi
import cl.frutapp.app.legal.LEGAL_VERSION
import cl.frutapp.app.legal.LegalDocKind
import cl.frutapp.app.navigation.legal.LegalDocScreen
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
        // rememberSaveable: sobrevive a navegacion a otra pantalla (ej. abrir Terminos y
        // Condiciones) y a process death. Con remember normal, volver del LegalDocScreen
        // recreaba el composable y borraba todo lo escrito.
        var name by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        // password/confirm intencionalmente con remember (no rememberSaveable): rememberSaveable
        // serializa al SavedStateBundle del Activity y Android puede flushearlo a disco en
        // process death. Mantener la password en texto plano fuera de memoria es un riesgo
        // innecesario — y el usuario no necesita que sobreviva navegacion: el link de Terminos
        // y Condiciones se abre via push (no destruye RegisterScreen), asi que remember basta.
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        var acceptTerms by rememberSaveable { mutableStateOf(false) }
        var codigoInv by rememberSaveable { mutableStateOf("") }
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
                    // capitalization=Words: el teclado capitaliza la primera letra de cada
                    // palabra del nombre. Si el usuario pega 'juan perez' lo dejamos como
                    // viene; solo afectamos lo que tecleen nuevo.
                    onValueChange = { name = it },
                    label = "Nombre completo",
                    leadingIcon = Icons.Default.Person,
                    capitalization = KeyboardCapitalization.Words
                )
                FrutTextField(
                    value = email,
                    // Email siempre minusculas — el backend ya hace lowercase, pero el
                    // visual mientras teclea era con may al inicio (KeyboardType.Email
                    // por defecto da capitalize=None, OK; pero por si el teclado lo cambio).
                    onValueChange = { email = it.lowercase() },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Default.Email,
                    keyboardType = KeyboardType.Email
                )
                FrutTextField(
                    value = phone,
                    // El label ya indica '+56'; el usuario solo debe escribir su numero local.
                    // Filtro a solo digitos siempre (descarta '+', espacios, guiones). El strip
                    // de '56' SOLO corre cuando el largo total sugiere que viene country-code
                    // dentro del mismo campo (>= 11 digitos = '56' + numero chileno completo de
                    // 9). Antes lo hacia en CADA keystroke con length>=2 y se comia los digitos
                    // legitimos del usuario al tipear ('5','6','9' → al llegar a '56' lo dejaba
                    // en vacio, despues '9' empezaba de cero — el usuario veia digitos
                    // desaparecer). Asi el strip solo actua cuando hace fisicamente sentido.
                    onValueChange = { nuevo ->
                        val soloDigitos = nuevo.filter { it.isDigit() }
                        phone = if (soloDigitos.length >= 11 && soloDigitos.startsWith("56")) {
                            soloDigitos.removePrefix("56")
                        } else {
                            soloDigitos
                        }
                    },
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
                FrutTextField(
                    value = codigoInv,
                    // Codigo 8 chars alfanumericos upper. Filtramos + normalizamos
                    // en input asi el user no tiene que preocuparse si lo pega
                    // con espacios o guiones.
                    onValueChange = { nuevo ->
                        codigoInv = nuevo.filter { it.isLetterOrDigit() }.uppercase().take(8)
                    },
                    label = "Código de invitación (opcional)",
                    leadingIcon = Icons.Default.PersonAdd,
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
                val consentText = buildAnnotatedString {
                    append("Acepto los ")
                    pushStringAnnotation("link", "TERMS")
                    withStyle(SpanStyle(color = FrutAppColors.Brand600, fontWeight = FontWeight.SemiBold)) {
                        append("Términos y Condiciones")
                    }
                    pop()
                    append(" y la ")
                    pushStringAnnotation("link", "PRIVACY")
                    withStyle(SpanStyle(color = FrutAppColors.Brand600, fontWeight = FontWeight.SemiBold)) {
                        append("Política de Privacidad")
                    }
                    pop()
                    append(".")
                }
                ClickableText(
                    text = consentText,
                    style = TextStyle(color = FrutAppColors.InkMuted, fontSize = 12.sp),
                    modifier = Modifier.padding(start = 4.dp),
                    onClick = { offset ->
                        consentText.getStringAnnotations("link", offset, offset).firstOrNull()?.let { ann ->
                            when (ann.item) {
                                "TERMS" -> navigator.push(LegalDocScreen(LegalDocKind.TERMS))
                                "PRIVACY" -> navigator.push(LegalDocScreen(LegalDocKind.PRIVACY))
                            }
                        }
                    }
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
                                    password = password,
                                    consentVersion = LEGAL_VERSION,
                                    codigoInvitacion = codigoInv.ifBlank { null },
                                )
                            )
                        }
                            .onSuccess {
                                // Marcamos el limbo: si el usuario cierra la app antes de
                                // tipear el codigo, al volver caera directo en VerifyCode.
                                cl.frutapp.app.data.TokenStore.markPendingEmail(email.trim())
                                navigator.push(VerifyCodeScreen(email = email.trim()))
                            }
                            .onFailure { e ->
                                cl.frutapp.app.ui.ErrorReporter.report(screen = "Register", action = "register", error = e)
                                val msg = e.message.orEmpty().lowercase()
                                error = when {
                                    msg.contains("409") || msg.contains("conflict") || msg.contains("already") ->
                                        "Ese correo ya está registrado. Intenta iniciar sesión o recupera tu contraseña."
                                    msg.contains("422") || msg.contains("validation") || msg.contains("invalid") ->
                                        "Revisa los datos: el correo debe ser válido y la contraseña al menos 6 caracteres con letras y números."
                                    else -> cl.frutapp.app.ui.mensajeAmigable(e, "crear la cuenta")
                                }
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
