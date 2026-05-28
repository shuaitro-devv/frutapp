package cl.frutapp.backend.modules.auth

/**
 * Plantillas de correo con la identidad FrutApp (verde + wordmark). HTML compatible
 * con clientes de correo (tablas + estilos inline). Cuando se hostee el logo se puede
 * cambiar el wordmark por un <img>.
 */
object EmailTemplates {

    fun passwordReset(to: String, code: String): Email {
        val subject = "Tu código para restablecer la contraseña · FrutApp"
        val text = buildString {
            appendLine("Tu código de recuperación FrutApp es: $code")
            appendLine("Vence en 30 minutos.")
            appendLine("Si no lo solicitaste, ignora este correo; tu contraseña seguirá igual.")
        }
        val html = """
            <!DOCTYPE html>
            <html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
            <body style="margin:0;background:#EAF3DE;font-family:Arial,Helvetica,sans-serif;">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#EAF3DE;padding:24px 0;">
                <tr><td align="center">
                  <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="max-width:480px;background:#ffffff;border-radius:16px;overflow:hidden;">
                    <tr><td style="background:#27500A;padding:20px 24px;">
                      <span style="color:#ffffff;font-size:24px;font-weight:bold;">Frut</span><span style="color:#97C459;font-size:24px;font-weight:bold;">App</span>
                    </td></tr>
                    <tr><td style="padding:28px 28px 8px;">
                      <h1 style="margin:0 0 6px;color:#27500A;font-size:20px;">Restablece tu contraseña</h1>
                      <p style="margin:0;color:#5C6B4A;font-size:14px;line-height:1.5;">Usa este código para continuar. Vence en 30 minutos.</p>
                    </td></tr>
                    <tr><td align="center" style="padding:20px 28px 24px;">
                      <div style="display:inline-block;background:#EAF3DE;color:#27500A;font-size:30px;font-weight:bold;letter-spacing:8px;padding:16px 24px;border-radius:12px;">$code</div>
                    </td></tr>
                    <tr><td style="padding:0 28px 28px;">
                      <p style="margin:0;color:#8A9377;font-size:12px;line-height:1.5;">Si no solicitaste este cambio, ignora este correo. Tu contraseña actual seguirá funcionando.</p>
                    </td></tr>
                    <tr><td style="background:#F1EFE8;padding:16px 28px;text-align:center;">
                      <p style="margin:0;color:#8A9377;font-size:12px;">FrutApp · De la cosecha a tu mesa</p>
                    </td></tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
        """.trimIndent()
        return Email(to = to, subject = subject, html = html, text = text)
    }
}
