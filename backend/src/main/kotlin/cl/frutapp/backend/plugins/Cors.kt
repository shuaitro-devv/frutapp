package cl.frutapp.backend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

/**
 * CORS restringido a hosts conocidos. El cliente principal es la app movil
 * (no aplica CORS) — esto cubre cuando el browser de un sponsor o tester
 * mira la API desde la landing o el panel admin futuro.
 *
 * Para abrirlo a otro dominio temporal, agregar el host explicitamente; no
 * volver a `anyHost()` porque deja la API expuesta a CSRF cross-origin desde
 * cualquier sitio.
 */
fun Application.configureCors() {
    install(CORS) {
        // Landing y subdominios del workspace `grandline.cl`. La app movil no
        // pasa por aca; este whitelist sirve para herramientas web internas.
        allowHost("frutapp.grandline.cl", schemes = listOf("https"))
        allowHost("frutapp-api.grandline.cl", schemes = listOf("https"))
        allowHost("frutapp-storage.grandline.cl", schemes = listOf("https"))
        allowHost("grandline.cl", schemes = listOf("https"))
        allowHost("localhost:9999")  // landing dev local
        allowHost("localhost:8080")  // backend dev local
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
}
