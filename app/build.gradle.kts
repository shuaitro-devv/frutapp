import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    // Lee app/google-services.json y materializa los strings que el SDK Firebase
    // espera en runtime (no hay que escribirlos a mano).
    id("com.google.gms.google-services")
}

// Carga credenciales de firma desde keystore.properties (fuera del repo). Si no existe,
// release queda sin firmar (útil en CI/dev de otros). La keystore real solo vive en el
// equipo que firma releases para Play / instalación demo.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)

            // Ktor client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Voyager navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.transitions)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            // Huella / biometría (trae androidx.fragment para FragmentActivity)
            implementation("androidx.biometric:biometric:1.1.0")
            // Almacenamiento cifrado de la sesión (tokens)
            implementation("androidx.security:security-crypto:1.1.0-alpha06")
            // Firebase Cloud Messaging — BoM resuelve versiones compatibles entre si.
            // Sin firebase-analytics: no lo necesitamos y agrega ~3MB + tracking que
            // no autorizamos en T&C. Solo el messaging.
            implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
            implementation("com.google.firebase:firebase-messaging-ktx")
            // .await() sobre Task<T> de Firebase desde corutinas.
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")
        }
    }
}

android {
    namespace = "cl.frutapp.app"
    compileSdk = libs.versions.android.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "cl.frutapp.app"
        minSdk = libs.versions.android.min.sdk.get().toInt()
        targetSdk = libs.versions.android.target.sdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    // White-label: dos flavors que generan FrutApp.apk y Sofruco.apk desde el
    // mismo codigo. Cada flavor inyecta BRAND_ID por BuildConfig; MainActivity
    // lo lee y setea ActiveBrand antes de setContent.
    flavorDimensions += "brand"
    productFlavors {
        create("frutapp") {
            dimension = "brand"
            applicationId = "cl.frutapp.app"
            buildConfigField("String", "BRAND_ID", "\"frutapp\"")
            resValue("string", "app_name", "FrutApp")
        }
        create("sofruco") {
            dimension = "brand"
            applicationId = "cl.frutapp.app.sofruco"
            buildConfigField("String", "BRAND_ID", "\"sofruco\"")
            resValue("string", "app_name", "Sofruco")
        }
    }

    // Nombre de salida del APK por flavor+buildType. Ej: FrutApp-frutapp-debug.apk,
    // Sofruco-sofruco-release.apk. Para el demo basta correr `:app:assembleSofrucoDebug`
    // y `:app:assembleFrutappDebug` por separado.
    applicationVariants.all {
        val variant = this
        val brandName = if (variant.flavorName == "sofruco") "Sofruco" else "FrutApp"
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "$brandName-${variant.buildType.name}.apk"
            }
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            // Apunta al backend de producción (vivo) para el demo. Para backend local
            // usar "http://10.0.2.2:8080" (requiere correr :backend:run con Postgres).
            buildConfigField("String", "API_BASE_URL", "\"https://frutapp-api.grandline.cl\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://frutapp-api.grandline.cl\"")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        // compose lo maneja el plugin org.jetbrains.compose (Compose Multiplatform),
        // NO buildFeatures.compose del AGP (que usaría un compose-compiler incompatible con Kotlin 1.9.22)
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
