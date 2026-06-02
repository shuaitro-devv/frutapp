package cl.frutapp.app.navigation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.AuthApi
import cl.frutapp.app.navigation.home.HomeScreen
import cl.frutapp.app.ui.components.AuthHeaderText
import cl.frutapp.shared.dto.ResendVerificationRequest
import cl.frutapp.shared.dto.VerifyEmailRequest
import kotlinx.coroutines.launch
import cl.frutapp.app.ui.components.AuthScaffold
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutCard
import cl.frutapp.app.ui.components.OtpInput
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.delay

class VerifyCodeScreen(private val email: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var code by remember { mutableStateOf("") }
        var loading by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        var seconds by remember { mutableStateOf(45) }
        // Incrementar este trigger relanza el LaunchedEffect y reinicia el countdown
        // (con key=Unit el efecto no se relanzaba y "Reenviar" dejaba el contador congelado).
        var resendTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(resendTrigger) {
            seconds = 45
            while (seconds > 0) {
                delay(1000)
                seconds -= 1
            }
        }

        // Funcion compartida: usada tanto por el boton 'Verificar codigo' como por el
        // auto-submit cuando completas los 6 digitos. UX: el usuario no deberia tener que
        // pegar el codigo y despues volver a tocar un boton — apenas entran los 6 digitos
        // disparamos verify automaticamente.
        val submit: () -> Unit = {
            if (!loading && code.length == 6) {
                error = null
                loading = true
                scope.launch {
                    runCatching { AuthApi().verifyEmail(VerifyEmailRequest(email = email, code = code)) }
                        .onSuccess { resp ->
                            TokenStore.save(resp.accessToken, resp.refreshToken, resp.user)
                            navigator.replaceAll(HomeScreen())
                        }
                        .onFailure { e ->
                            cl.frutapp.app.ui.ErrorReporter.report(screen = "VerifyCode", action = "verify_email", error = e)
                            val msg = e.message.orEmpty().lowercase()
                            error = if (msg.contains("400") || msg.contains("404") || msg.contains("invalid") || msg.contains("expired"))
                                "Código inválido o expirado. Revisa tu correo o reenvíalo."
                            else cl.frutapp.app.ui.mensajeAmigable(e, "verificar el código")
                        }
                    loading = false
                }
            }
        }

        // 'Volver': sale del limbo de verificacion → limpia pendingEmail y replaceAll a Login.
        // Antes era pop()/popUntilRoot, pero como ahora VerifyCode puede ser la pantalla raiz
        // (cuando el splash detecta pendingEmail), pop no llevaba a ningun lado: el usuario
        // quedaba atrapado sin forma de cambiar de correo o empezar de nuevo.
        val salirDelLimbo: () -> Unit = {
            TokenStore.clearPendingEmail()
            navigator.replaceAll(LoginScreen())
        }

        AuthScaffold(showBackButton = true, onBack = salirDelLimbo) {
            Box(
                modifier = Modifier.size(56.dp).background(FrutAppColors.Brand50, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(30.dp))
            }
            Box(modifier = Modifier.padding(top = 16.dp)) {
                AuthHeaderText(
                    title = "Verifica tu código",
                    subtitle = "Hemos enviado un código de 6 dígitos a $email. Ingresa el código para continuar."
                )
            }

            OtpInput(
                value = code,
                onValueChange = { nuevo ->
                    code = nuevo
                    // Auto-submit al completar los 6 digitos.
                    if (nuevo.length == 6) submit()
                },
                modifier = Modifier.padding(top = 28.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(16.dp))
                if (seconds > 0) {
                    Text(
                        text = "  Reenviar código en 00:${seconds.toString().padStart(2, '0')}",
                        color = FrutAppColors.InkMuted,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "  Reenviar código",
                        color = FrutAppColors.Brand600,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            resendTrigger += 1
                            scope.launch { runCatching { AuthApi().resendVerification(ResendVerificationRequest(email = email)) } }
                        }
                    )
                }
            }

            FrutCard(containerColor = FrutAppColors.Brand50, modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = FrutAppColors.Brand400, modifier = Modifier.size(24.dp))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("¿No recibiste el código?", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Revisa tu bandeja de spam o correos no deseados.", color = FrutAppColors.InkMuted, fontSize = 12.sp)
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = FrutAppColors.Error, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp))
            }

            FrutButtonPrimary(
                text = if (loading) "Verificando…" else "Verificar código",
                onClick = submit,
                enabled = code.length == 6 && !loading,
                modifier = Modifier.padding(top = 20.dp)
            )
            FrutButtonOutline(
                text = "Volver al inicio de sesión",
                onClick = salirDelLimbo,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
