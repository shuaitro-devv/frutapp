package cl.frutapp.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.app.ui.theme.FrutAppShapes

/**
 * Input FrutApp según mockups: borde redondeado, ícono a la izquierda,
 * fondo blanco, focus verde. Soporta password con toggle de ojo.
 */
@Composable
fun FrutTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    isError: Boolean = false,
    errorText: String? = null,
    /** Si != null, fuerza esa accion en el teclado (Next/Done/Send). Default
     *  null: usa el comportamiento del sistema (Done normalmente). */
    imeAction: ImeAction? = null,
    /** Callback cuando el usuario toca el boton del teclado configurado por
     *  [imeAction]. Si es null y [imeAction]=Done, oculta el teclado. */
    onImeAction: (() -> Unit)? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = FrutAppShapes.medium,
        singleLine = true,
        isError = isError,
        supportingText = if (isError && errorText != null) {
            { Text(errorText, color = FrutAppColors.Error) }
        } else null,
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = FrutAppColors.Brand600) }
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                        tint = FrutAppColors.InkMuted
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
            capitalization = capitalization,
            imeAction = imeAction ?: ImeAction.Default,
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onImeAction?.invoke() ?: keyboardController?.hide()
            },
            onSend = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() },
            onNext = { onImeAction?.invoke() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FrutAppColors.Brand400,
            unfocusedBorderColor = FrutAppColors.Brand100,
            focusedLabelColor = FrutAppColors.Brand600,
            cursorColor = FrutAppColors.Brand400,
            errorBorderColor = FrutAppColors.Error
        )
    )
}
