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
            implementation(libs.ktor.client.websockets)
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
            // Ktor client engine: okhttp en vez de android porque android engine
            // NO soporta WebSockets. okhttp si, y el resto del API (HTTP regular)
            // funciona identico — el switch es invisible para los demas calls.
            implementation(libs.ktor.client.okhttp)
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
            // GPS / ubicacion: Fused Location Provider (Google Play Services).
            implementation("com.google.android.gms:play-services-location:21.3.0")
            // Mapa: SDK + binding Compose nativo (maps-compose).
            implementation("com.google.android.gms:play-services-maps:19.0.0")
            implementation("com.google.maps.android:maps-compose:4.4.1")
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
        // El CI pasa -PappVersionName=<tag> al buildear un release desde un tag
        // (workflow release-apk.yml). Sin ese prop caemos al fallback local
        // para que el desarrollador vea 0.0.0-dev y sepa que no viene de tag.
        versionCode = (findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (findProperty("appVersionName") as String?) ?: "0.0.0-dev"
        // Google Maps API key: se lee de local.properties (mapsApiKey=AIza...)
        // o env var MAPS_API_KEY (CI). Sin clave el mapa carga gris pero el
        // resto de la app funciona normal (mejor que crashear). El manifest
        // tiene placeholder ${MAPS_API_KEY} que se sustituye con este valor.
        val mapsKey = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }.getProperty("mapsApiKey")
            ?: System.getenv("MAPS_API_KEY")
            ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsKey
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
            // R8 ofusca (renombra clases/metodos) + tree-shake (elimina codigo
            // no usado). Sube la barra del reverse engineering: una APK
            // decompilada con jadx pasa de leer codigo Kotlin claro a un grafo
            // ofuscado. NO es invulnerable (un atacante motivado con Frida
            // burla esto), pero detiene el casual.
            // R8 minify DESACTIVADO temporalmente: rompia render de Compose
            // (FAB del bottom nav con cuadrado negro, animaciones parpadeando)
            // porque las reglas Proguard no cubren todos los callsites de
            // Compose runtime. Iteracion pendiente — mientras tanto la
            // proteccion real viene de:
            //  1) Firma con keystore propio (sin la llave nadie reemplaza la
            //     APK instalada por una modificada).
            //  2) Network Security Config (HTTPS-only, sin user CAs en release).
            //  3) Manifest: allowBackup=false, usesCleartextTraffic=false.
            //  4) buildType release con isDebuggable=false (default).
            // Para volver a activar R8: poner isMinifyEnabled=true y descomentar
            // proguardFiles. Iterar reglas viendo logcat al primer crash.
            isMinifyEnabled = false
            isShrinkResources = false
            // proguardFiles(
            //     getDefaultProguardFile("proguard-android-optimize.txt"),
            //     "proguard-rules.pro"
            // )
            // TEMPORAL: el google-services.json solo tiene registrado el
            // package `cl.frutapp.app.debug`. Hasta que registremos en Firebase
            // Console el package no-debug (cl.frutapp.app), reusamos el suffix
            // .debug para que FCM funcione tambien en release. Cuando se quiera
            // publicar en Play Store, quitar este suffix y agregar la app en
            // Firebase Console + bajar nuevo google-services.json.
            applicationIdSuffix = ".debug"
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
