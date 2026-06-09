// Root build file — solo configura plugins comunes y repositorios
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ktor) apply false
    // Google Services para FCM: lee app/google-services.json y genera los
    // resources que el SDK de Firebase espera al arrancar. Solo se aplica en :app.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
