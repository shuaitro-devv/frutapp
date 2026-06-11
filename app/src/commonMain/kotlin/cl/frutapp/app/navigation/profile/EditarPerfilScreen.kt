package cl.frutapp.app.navigation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.AvatarApi
import cl.frutapp.app.platform.rememberSelectorImagenes
import cl.frutapp.app.ui.ErrorReporter
import cl.frutapp.app.ui.components.AvatarImage
import cl.frutapp.app.ui.components.CropAvatarSheet
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.mensajeAmigable
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Editar nombre y teléfono del perfil. Email queda solo lectura (cambiar email requiere
 * re-verificación, lo dejamos p/ después). Foto de perfil: placeholder "Próximamente"
 * hasta wirear upload real (MinIO/Cloudinary).
 *
 * Persistencia: solo cliente por ahora — actualiza TokenStore.user (que persiste en
 * SessionStorage). Cuando el backend tenga PATCH /v1/users/me, agregamos la llamada
 * antes del updateUser local.
 */
class EditarPerfilScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userActual = TokenStore.user
        var nombre by remember { mutableStateOf(userActual?.name ?: "") }
        var telefono by remember { mutableStateOf(userActual?.phone.orEmpty()) }
        val email = userActual?.email.orEmpty()
        val inicial = (nombre.firstOrNull()?.uppercase() ?: "F")
        val cambios = nombre.trim().isNotBlank() && (nombre != userActual?.name || telefono != userActual?.phone.orEmpty())

        // Foto de perfil: URL presignada que vino del backend en /me. Se actualiza
        // localmente al subir una nueva sin esperar al proximo /me.
        var avatarUrl by remember { mutableStateOf(userActual?.avatarUrl) }
        var subiendoFoto by remember { mutableStateOf(false) }
        // bytesPendientes != null abre el CropAvatarSheet sobre la pantalla. El
        // usuario ajusta encuadre y al confirmar sube SOLO el cuadrado recortado.
        var bytesPendientes by remember { mutableStateOf<ByteArray?>(null) }
        val scope = rememberCoroutineScope()
        val avatarApi = remember { AvatarApi() }
        val selectorImg = rememberSelectorImagenes { bytes -> bytesPendientes = bytes }

        bytesPendientes?.let { src ->
            CropAvatarSheet(
                bytes = src,
                onDismiss = { bytesPendientes = null },
                onListo = { recortados ->
                    bytesPendientes = null
                    subiendoFoto = true
                    scope.launch {
                        runCatching { avatarApi.upload(recortados) }
                            .onSuccess { newUrl ->
                                avatarUrl = newUrl
                                TokenStore.user?.let { u -> TokenStore.updateUser(u.copy(avatarUrl = newUrl)) }
                                showToast("Foto actualizada ✓")
                            }
                            .onFailure { e ->
                                if (e is CancellationException) throw e
                                ErrorReporter.report(screen = "EditarPerfil", action = "upload_avatar", error = e)
                                showToast(mensajeAmigable(e, "subir la foto"))
                            }
                        subiendoFoto = false
                    }
                }
            )
        }

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
                    Text(
                        "Editar perfil",
                        color = FrutAppColors.Brand800,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 24.dp)
                ) {
                    // Avatar circular: AvatarImage descarga la foto del backend si la
                    // tiene; sino muestra la inicial. El boton de camara abre la
                    // galeria; subiendoFoto loquea el tap para evitar dobles uploads.
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AvatarImage(url = avatarUrl, initial = inicial, size = 96.dp)
                            Box(
                                modifier = Modifier.size(34.dp)
                                    .background(Color.White, CircleShape)
                                    .border(2.dp, FrutAppColors.Brand50, CircleShape)
                                    .clickable(enabled = !subiendoFoto) { selectorImg.galeria() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (subiendoFoto) {
                                    CircularProgressIndicator(
                                        color = FrutAppColors.Brand600,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = "Cambiar foto", tint = FrutAppColors.Brand600, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    CampoTexto(
                        label = "Nombre completo",
                        value = nombre,
                        onValueChange = { nombre = it },
                        placeholder = "Tu nombre"
                    )
                    Spacer(Modifier.height(14.dp))
                    CampoTexto(
                        label = "Teléfono",
                        value = telefono,
                        onValueChange = { telefono = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } },
                        placeholder = "+56 9 1234 5678"
                    )
                    Spacer(Modifier.height(14.dp))
                    CampoLectura(
                        label = "Correo electrónico",
                        value = email,
                        detalle = "Para cambiarlo escríbenos por WhatsApp."
                    )

                    Spacer(Modifier.height(28.dp))

                    FrutButtonPrimary(
                        text = "Guardar cambios",
                        enabled = cambios,
                        onClick = {
                            val u = TokenStore.user ?: return@FrutButtonPrimary
                            val actualizado = u.copy(
                                name = nombre.trim(),
                                phone = telefono.trim().ifBlank { null }
                            )
                            TokenStore.updateUser(actualizado)
                            showToast("Perfil actualizado")
                            navigator.pop()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CampoTexto(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp)
                .background(FrutAppColors.Cream, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) Text(placeholder, color = FrutAppColors.InkSoft, fontSize = 14.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 14.sp),
                cursorBrush = SolidColor(FrutAppColors.Brand400),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CampoLectura(label: String, value: String, detalle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp)
                .background(FrutAppColors.Brand50, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(value, color = FrutAppColors.InkMuted, fontSize = 14.sp)
            Icon(Icons.Filled.Lock, contentDescription = null, tint = FrutAppColors.InkSoft, modifier = Modifier.size(16.dp))
        }
        Text(detalle, color = FrutAppColors.InkSoft, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
    }
}
