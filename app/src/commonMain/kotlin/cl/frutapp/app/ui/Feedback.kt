package cl.frutapp.app.ui

/** Mensaje breve provisto por la plataforma (Android: Toast). */
expect fun showToast(message: String)

/** Feedback estándar para acciones aún no construidas (evita que se vean "rotas"). */
fun comingSoon() = showToast("Disponible próximamente")
