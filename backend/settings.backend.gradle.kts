// Settings reducido SOLO para builds de Docker del backend.
// Excluye :app (que requiere Android SDK) para no romper builds en CI.
// El settings.gradle.kts raíz (con los 3 módulos) sigue siendo el real para Android Studio + desarrollo local.
//
// IMPORTANTE: cuando el Dockerfile copia este archivo a la raíz del builder como `settings.gradle.kts`,
// los módulos están en `./shared` y `./backend` (default, sin override de projectDir).

rootProject.name = "frutapp"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":shared")
include(":backend")
