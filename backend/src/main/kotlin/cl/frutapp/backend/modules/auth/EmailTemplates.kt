package cl.frutapp.backend.modules.auth

/**
 * Plantillas de correo con la identidad FrutApp.
 *
 * NOMENCLATURA: todo correo se arma con [baseLayout] (header con logo y footer comunes)
 * + un bloque `content` propio. Para un correo nuevo: escribe su `content` (usando
 * [noticeBox] para los avisos) y envuélvelo con [baseLayout]; así todos comparten la
 * misma identidad visual.
 *
 * Reglas de HTML para correo: tablas (no flex/grid), estilos inline (Gmail descarta
 * <style>), e imágenes por URL absoluta https. El header lleva un color sólido de
 * fallback además del gradiente porque Outlook ignora `linear-gradient`.
 */
object EmailTemplates {

    private const val LOGO_URL = "https://frutapp.grandline.cl/img/logo-white.png"
    private const val SLOGAN = "De la cosecha a tu mesa"

    fun welcome(to: String, name: String): Email {
        val subject = "¡Te damos la bienvenida a FrutApp!"
        val text = buildString {
            appendLine("¡Hola, $name! Tu cuenta FrutApp ya está lista.")
            appendLine("Llevamos frutas y verduras frescas de la feria directo a tu mesa.")
            appendLine("Explora productos frescos, junta FrutCoins y recicla con cada pedido.")
        }
        val content = """
            <h1 style="margin:0 0 12px;color:#27500A;font-size:28px;line-height:1.25;font-weight:800;">
              ¡Hola, $name!
            </h1>
            <p style="margin:0 auto 26px;max-width:400px;color:#4B5563;font-size:16px;line-height:1.55;">
              Tu cuenta ya está lista. En FrutApp llevamos frutas y verduras frescas de la feria directo a tu mesa.
            </p>
            ${noticeBox("¿Qué puedes hacer ahora?", "Explorar productos frescos, juntar FrutCoins en cada compra y reciclar con tus pedidos. ¡Que aproveche!")}
        """.trimIndent()
        return Email(to = to, subject = subject, html = baseLayout("Bienvenido a FrutApp", content), text = text)
    }

    fun passwordReset(to: String, code: String): Email {
        val subject = "Tu código para restablecer la contraseña · FrutApp"
        val text = buildString {
            appendLine("Tu código de recuperación FrutApp es: $code")
            appendLine("Vence en 30 minutos.")
            appendLine("Si no lo solicitaste, ignora este correo; tu contraseña seguirá igual.")
        }
        val content = """
            <h1 style="margin:0 0 12px;color:#27500A;font-size:28px;line-height:1.25;font-weight:800;">
              Restablece tu contraseña
            </h1>
            <p style="margin:0 auto 26px;max-width:390px;color:#4B5563;font-size:16px;line-height:1.55;">
              Usa el siguiente código para continuar con el proceso de recuperación de tu cuenta.
            </p>

            <table role="presentation" cellspacing="0" cellpadding="0" align="center" style="margin:0 auto 26px;">
              <tr>
                <td style="background-color:#EAF3DE;border:1px solid #CFE7BF;border-radius:18px;padding:18px 28px;">
                  <span style="display:block;color:#27500A;font-size:34px;line-height:1;font-weight:800;letter-spacing:10px;">$code</span>
                </td>
              </tr>
            </table>

            <p style="margin:0 0 24px;color:#6B7280;font-size:14px;line-height:1.5;">
              Este código vence en <strong style="color:#27500A;">30 minutos</strong>.
            </p>

            ${noticeBox("Tu seguridad es importante", "Si no solicitaste este cambio, puedes ignorar este correo. Tu contraseña actual seguirá funcionando.")}
        """.trimIndent()
        return Email(to = to, subject = subject, html = baseLayout("Restablece tu contraseña - FrutApp", content), text = text)
    }

    fun passwordChanged(to: String, name: String): Email {
        val subject = "Tu contraseña fue actualizada · FrutApp"
        val text = buildString {
            appendLine("Hola $name, te confirmamos que la contraseña de tu cuenta FrutApp se cambió correctamente.")
            appendLine("Si no fuiste tú, contáctanos de inmediato para proteger tu cuenta.")
        }
        val content = """
            <h1 style="margin:0 0 12px;color:#27500A;font-size:28px;line-height:1.25;font-weight:800;">
              Tu contraseña fue actualizada
            </h1>
            <p style="margin:0 auto 26px;max-width:400px;color:#4B5563;font-size:16px;line-height:1.55;">
              Hola $name, te confirmamos que la contraseña de tu cuenta se cambió correctamente.
            </p>
            ${noticeBox("¿No fuiste tú?", "Si no realizaste este cambio, contáctanos de inmediato para proteger tu cuenta.")}
        """.trimIndent()
        return Email(to = to, subject = subject, html = baseLayout("Contraseña actualizada - FrutApp", content), text = text)
    }

    /** Caja de aviso reutilizable (verde suave) para destacar un mensaje dentro del content. */
    private fun noticeBox(title: String, body: String): String = """
        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#F6FBF2;border:1px solid #DDEED3;border-radius:16px;">
          <tr>
            <td style="padding:18px 20px;text-align:left;">
              <p style="margin:0 0 6px;color:#27500A;font-size:15px;font-weight:700;">$title</p>
              <p style="margin:0;color:#5F6F5A;font-size:13px;line-height:1.5;">$body</p>
            </td>
          </tr>
        </table>
    """.trimIndent()

    /** Envoltorio común (header con logo, slot de contenido y footer). */
    private fun baseLayout(title: String, content: String): String = """
        <!DOCTYPE html>
        <html lang="es">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>$title</title>
        </head>
        <body style="margin:0;padding:0;background-color:#EAF3DE;font-family:Arial,Helvetica,sans-serif;color:#1F2937;">
          <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="background-color:#EAF3DE;padding:40px 16px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:520px;background-color:#FFFFFF;border-radius:22px;overflow:hidden;box-shadow:0 12px 32px rgba(22,101,52,0.12);">

                  <tr>
                    <td align="center" style="background-color:#27500A;background:linear-gradient(135deg,#27500A,#4C8A1F);padding:32px 28px;">
                      <img src="$LOGO_URL" alt="FrutApp" width="180" style="display:block;max-width:180px;margin:0 auto;" />
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:36px 34px 26px;text-align:center;">
                      $content
                    </td>
                  </tr>

                  <tr>
                    <td align="center" style="background-color:#F3F0EA;padding:22px 28px;">
                      <p style="margin:0 0 6px;color:#27500A;font-size:14px;font-weight:700;">FrutApp</p>
                      <p style="margin:0;color:#7A8574;font-size:12px;line-height:1.5;">
                        $SLOGAN<br />
                        Este es un correo automático, por favor no respondas a este mensaje.
                      </p>
                    </td>
                  </tr>

                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
