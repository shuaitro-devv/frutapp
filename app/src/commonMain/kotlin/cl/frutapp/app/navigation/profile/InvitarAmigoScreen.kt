package cl.frutapp.app.navigation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.shareText
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/** Invita a un amigo (flujo de referidos, dummy): código personal, copiar, compartir nativo. */
class InvitarAmigoScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val clipboard = LocalClipboardManager.current
        val user = TokenStore.user
        val code = remember(user?.email) {
            val base = user?.name?.substringBefore(' ')?.filter { it.isLetter() }?.uppercase()?.take(6)
            "FRUT-" + (base?.ifBlank { null } ?: "AMIGO")
        }
        val mensaje = "¡Únete a FrutApp! Frutas y verduras frescas de la feria, directo a tu casa. " +
            "Usa mi código $code en tu registro y ganamos FrutCoins los dos. 🍎"

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
                    }
                    Text("Invita a un amigo", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    // Hero
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(FrutAppColors.Brand600, FrutAppColors.Brand400)), RoundedCornerShape(20.dp))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Text("Invita y ganen los dos", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text(
                            "Tú ganas 100 FrutCoins y tu amigo 50 en su primera compra.",
                            color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Código + copiar
                    Text("Tu código", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                            .border(1.5.dp, FrutAppColors.Brand200, RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(code, color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Row(
                            modifier = Modifier.clickable {
                                clipboard.setText(AnnotatedString(code))
                                showToast("Código copiado")
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar", tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
                            Text("Copiar", color = FrutAppColors.Brand600, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                        }
                    }

                    Box(modifier = Modifier.padding(top = 16.dp)) {
                        FrutButtonPrimary(text = "Compartir invitación", onClick = { shareText(mensaje) })
                    }

                    // Cómo funciona
                    Text("¿Cómo funciona?", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                    StepRow(1, "Comparte tu código con un amigo.")
                    StepRow(2, "Se registra y hace su primera compra.")
                    StepRow(3, "¡Ambos ganan FrutCoins!")

                    // Stat dummy
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp)
                            .background(FrutAppColors.AmberSoft, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Has invitado a 3 amigos", color = FrutAppColors.Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Llevas +300 FrutCoins por referidos", color = FrutAppColors.AmberCoin, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(n: Int, texto: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$n", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(texto, color = FrutAppColors.Ink, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
    }
}
