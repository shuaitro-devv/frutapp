package cl.frutapp.backend.config

import io.ktor.server.config.ApplicationConfig

/**
 * Config del storage S3-compatible (MinIO en VPS).
 *
 * - [endpoint]: host que usa el backend para subir/leer objetos (conexión interna en la red docker).
 * - [publicEndpoint]: host con que se firman las URLs presignadas que abrirá la app. Puede diferir
 *   del [endpoint] (en el emulador Android, `10.0.2.2` apunta al host de desarrollo).
 *
 * Las llaves se inyectan por env vars (`STORAGE_*`) en producción; los defaults son solo para dev local.
 *
 * Patrón replicado del backend de polizapp (proyecto hermano del mismo dueño). Ver
 * `docs/00-tecnico/Standard_Subida_Imagenes.md` para el estándar cross-app.
 */
data class StorageConfig(
    val endpoint: String,
    val publicEndpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
) {
    companion object {
        fun from(config: ApplicationConfig) = StorageConfig(
            endpoint = config.property("storage.endpoint").getString(),
            publicEndpoint = config.property("storage.publicEndpoint").getString(),
            accessKey = config.property("storage.accessKey").getString(),
            secretKey = config.property("storage.secretKey").getString(),
            bucket = config.property("storage.bucket").getString(),
        )
    }
}
