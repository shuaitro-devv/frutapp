# FrutApp · Reglas Proguard para release.
#
# R8 ofusca (renombra clases/metodos a a/b/c) y elimina codigo no usado. Las
# librerias abajo necesitan keeps porque usan reflection / generated code que
# R8 no detecta automaticamente y romperia en runtime.
#
# Iteramos: si tras un release crash hay una NoClassDefFoundError o
# SerializationException, agregamos -keep para esa lib y rebuild.

# Mantener fuentes de stacktrace utiles para diagnostico (puede leerse con
# retrace.txt + el mapping de R8 que queda en app/build/outputs/mapping/).
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ===== Kotlin Serialization =====
# Las classes @Serializable se acceden por reflection desde los serializers
# generados; R8 no las ve usadas. Mantenemos:
#  - Todas las classes con la anotacion
#  - Los Companion.serializer() generados
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Nuestros DTOs estan en shared.dto + app.data y backend modules. Para no
# enumerar uno por uno mantenemos todo el paquete shared.dto (es chico).
-keep class cl.frutapp.shared.dto.** { *; }
-keep,includedescriptorclasses class cl.frutapp.shared.dto.**$Companion { *; }
-keepclassmembers class cl.frutapp.shared.dto.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== Ktor client =====
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
# OkHttp engine: usa servicios via ServiceLoader.
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class okio.** { *; }
-dontwarn okio.**

# ===== Voyager (navegacion) =====
# Voyager usa reflection sobre las Screen subclasses para deeplinks /
# state restoration. Mantener las screens completas evita perder constructores.
-keep class cl.frutapp.app.navigation.** { *; }
-keep class cafe.adriel.voyager.** { *; }
-dontwarn cafe.adriel.voyager.**

# ===== Compose / Material3 =====
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class kotlin.Metadata { *; }

# ===== kotlinx.datetime =====
# Usa java.time bajo el capo en Android API 26+; basta mantener la API
# publica del paquete para que parsers/formatters funcionen.
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# ===== Firebase Cloud Messaging =====
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ===== Google Maps / Play Services =====
-keep class com.google.maps.** { *; }
-dontwarn com.google.maps.**

# ===== Koin (DI por reflection) =====
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# ===== Reglas defensivas generales =====
# Mantener anotaciones en runtime para reflection.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeVisibleTypeAnnotations

# Companion objects suelen contener serializers / singletons accedidos
# por nombre desde generated code.
-keepclassmembers class * {
    public static ** Companion;
}

# Enum classes — kotlinx.serialization las accede via values()/valueOf.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Nuestro codigo de app (entry points + reflection)
-keep class cl.frutapp.app.MainActivity { *; }
-keep class cl.frutapp.app.FrutAppApplication { *; }

# Modelos del store (data classes con estado Compose)
-keep class cl.frutapp.app.data.** { *; }

# ===== slf4j (logger interno de Ktor) =====
# El binding "real" lo trae el server; en cliente Ktor el binding es no-op.
# R8 ve referencias a clases que no estan en el classpath del cliente.
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }
