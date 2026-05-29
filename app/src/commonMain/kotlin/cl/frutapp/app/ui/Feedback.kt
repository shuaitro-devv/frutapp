package cl.frutapp.app.ui

/** Mensaje breve provisto por la plataforma (Android: Toast). */
expect fun showToast(message: String)

/** Abre el diálogo nativo de compartir con el texto dado (Android: ACTION_SEND). */
expect fun shareText(text: String)

/** Feedback estándar para acciones aún no construidas (evita que se vean "rotas"). */
fun comingSoon() = showToast("Disponible próximamente")
