package cl.frutapp.app.legal

/**
 * Versión vigente de los documentos legales que el usuario acepta al registrarse.
 * Si cambian de forma relevante, sube esta versión: el consentimiento guardado quedará
 * desactualizado y podrás volver a pedir aceptación.
 */
const val LEGAL_VERSION = "1.0"

private const val UPDATED = "29 de mayo de 2026"

/** Qué documento mostrar en el visor. */
enum class LegalDocKind { PRIVACY, TERMS }

data class LegalDoc(val title: String, val updated: String, val sections: List<LegalSection>)

/** Un bloque del documento. [heading] null = párrafo de introducción sin título. */
data class LegalSection(val heading: String?, val body: String)

fun legalDoc(kind: LegalDocKind): LegalDoc = when (kind) {
    LegalDocKind.PRIVACY -> PRIVACY_POLICY
    LegalDocKind.TERMS -> TERMS_AND_CONDITIONS
}

private val PRIVACY_POLICY = LegalDoc(
    title = "Política de Privacidad",
    updated = UPDATED,
    sections = listOf(
        LegalSection(
            null,
            "En FrutApp nos tomamos en serio tu privacidad. Esta Política explica qué datos " +
                "personales tratamos, con qué finalidad, con quién los compartimos y qué derechos " +
                "tienes. Al crear una cuenta y usar la app aceptas estas prácticas."
        ),
        LegalSection(
            "1. Qué datos recolectamos",
            "Solo los necesarios para prestarte el servicio:\n" +
                "• Identificación y contacto: nombre, correo y teléfono.\n" +
                "• Cuenta: contraseña (siempre cifrada, nunca en texto plano).\n" +
                "• Pedidos: productos, cantidades, peso, montos, dirección o sucursal y modalidad.\n" +
                "• Medios de pago: el tipo de medio y el monto. No almacenamos el número completo " +
                "de tu tarjeta; eso lo procesa la pasarela de pago.\n" +
                "• FrutCoins: saldo y movimientos.\n" +
                "• Contexto técnico: tipo de app, versión, modelo de dispositivo, sistema operativo " +
                "e idioma, además de datos de uso para seguridad y diagnóstico."
        ),
        LegalSection(
            "2. Para qué los usamos",
            "• Prestar el servicio: cuenta, cobro, despacho y seguimiento de tus pedidos.\n" +
                "• Programa FrutCoins.\n" +
                "• Seguridad y prevención de fraude.\n" +
                "• Soporte y atención de reclamos.\n" +
                "• Mejora y analítica del servicio (preferentemente con datos agregados).\n" +
                "• Comunicaciones transaccionales y, solo con tu consentimiento, promociones."
        ),
        LegalSection(
            "3. Base legal y consentimiento",
            "Tratamos tus datos para ejecutar el contrato del servicio que solicitas, con tu " +
                "consentimiento (que otorgas al registrarte) y para cumplir obligaciones legales " +
                "(por ejemplo, tributarias). Puedes retirar tu consentimiento cuando quieras."
        ),
        LegalSection(
            "4. Con quién los compartimos",
            "No vendemos tus datos. Solo los compartimos con proveedores que nos ayudan a operar " +
                "(pasarela de pago, facturación electrónica, despacho, infraestructura y correo), y " +
                "únicamente para esa finalidad."
        ),
        LegalSection(
            "5. Conservación",
            "Conservamos tus datos mientras tengas cuenta activa y los plazos que exija la ley. " +
                "Luego se eliminan o anonimizan."
        ),
        LegalSection(
            "6. Tus derechos",
            "Puedes acceder, rectificar, eliminar y oponerte al tratamiento de tus datos. Para " +
                "ejercerlos, escríbenos al correo de contacto y te responderemos en los plazos legales."
        ),
        LegalSection(
            "7. Seguridad",
            "Aplicamos medidas razonables: cifrado de contraseñas, conexiones seguras (HTTPS), " +
                "almacenamiento cifrado de la sesión y control de acceso."
        ),
        LegalSection(
            "8. Cambios",
            "Si actualizamos esta Política de forma relevante, te lo informaremos en la app y/o por " +
                "correo, indicando la nueva versión. El uso continuado implica su aceptación."
        )
    )
)

private val TERMS_AND_CONDITIONS = LegalDoc(
    title = "Términos y Condiciones",
    updated = UPDATED,
    sections = listOf(
        LegalSection(
            null,
            "Estos Términos regulan el uso de FrutApp. Al crear una cuenta y usar el servicio, " +
                "los aceptas."
        ),
        LegalSection(
            "1. Qué es FrutApp",
            "Una plataforma para comprar productos frescos con despacho a domicilio o retiro en " +
                "sucursal, y participar del programa de beneficios FrutCoins."
        ),
        LegalSection(
            "2. Tu cuenta",
            "Debes ser mayor de 18 años y entregar información veraz. Verificamos tu correo con un " +
                "código. Eres responsable de tu contraseña y de la actividad de tu cuenta."
        ),
        LegalSection(
            "3. Pedidos, precios y disponibilidad",
            "Los precios están en pesos chilenos e incluyen impuestos. Algunos productos se venden " +
                "por kilo: el monto mostrado es estimado y el cobro final puede ajustarse al peso real. " +
                "Si un producto no está disponible, podremos sustituirlo o ajustar el pedido. Puedes " +
                "elegir despacho o retiro; el retiro no tiene costo de envío."
        ),
        LegalSection(
            "4. Pagos",
            "Puedes pagar con los medios habilitados e incluso combinar más de uno (por ejemplo, " +
                "FrutCoins más otro medio), dentro de los límites definidos. El cobro puede hacerse en " +
                "dos momentos: pre-autorización al confirmar y captura del monto final. El pago lo " +
                "procesan pasarelas externas; no almacenamos el número de tu tarjeta."
        ),
        LegalSection(
            "5. Programa FrutCoins",
            "Los FrutCoins son puntos de fidelidad: no son dinero, no son reembolsables ni " +
                "transferibles y no tienen valor fuera de FrutApp. Se ganan y canjean según las reglas " +
                "vigentes (por ejemplo, un máximo del total pagable con FrutCoins), que podemos " +
                "modificar informándote en la app."
        ),
        LegalSection(
            "6. Cancelaciones y devoluciones",
            "Podrás solicitarlas conforme a la ley del consumidor y a las condiciones que " +
                "publiquemos. Los productos perecibles pueden tener condiciones especiales."
        ),
        LegalSection(
            "7. Responsabilidad y ley aplicable",
            "Hacemos nuestro mejor esfuerzo por mantener el servicio disponible y la información " +
                "correcta. Estos Términos se rigen por las leyes de Chile, sin perjuicio de los " +
                "derechos del consumidor."
        ),
        LegalSection(
            "8. Cambios",
            "Podemos actualizar estos Términos e informarte los cambios relevantes en la app y/o " +
                "por correo. El uso continuado implica su aceptación."
        )
    )
)
