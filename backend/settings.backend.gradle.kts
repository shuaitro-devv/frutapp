// Settings reducido SOLO para builds de Docker del backend.
// Excluye :app (que requiere Android SDK) para no romper builds en CI.
// El settings.gradle.kts raíz (con los 3 módulos) sigue siendo el real para Android Studio + desarrollo local.

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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":shared")
include(":backend")

project(":shared").projectDir = file("../shared")
project(":backend").projectDir = file("../backend")
