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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.navigation.home.HomeScreen
import cl.frutapp.app.ui.components.AuthHeaderText
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
        var code by remember { mutableStateOf("") }
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

        AuthScaffold(showBackButton = true, onBack = { navigator.pop() }) {
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
                onValueChange = { code = it },
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
                        modifier = Modifier.clickable { resendTrigger += 1 }
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

            FrutButtonPrimary(
                text = "Verificar código",
                onClick = { navigator.replaceAll(HomeScreen()) },
                enabled = code.length == 6,
                modifier = Modifier.padding(top = 20.dp)
            )
            FrutButtonOutline(
                text = "Volver al inicio de sesión",
                onClick = { navigator.popUntilRoot() },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
