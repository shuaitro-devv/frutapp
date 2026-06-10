package cl.frutapp.backend.modules.media

import cl.frutapp.backend.config.StorageConfig
import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.http.Method
import io.minio.RemoveObjectArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

/**
 * Almacenamiento de objetos sobre MinIO / S3.
 *
 * Usa dos clientes: [io] para subir/leer/borrar (endpoint interno docker) y [firma] para generar URLs
 * presignadas (endpoint público que la app puede abrir). Firmar es cómputo local: el cliente de firma
 * nunca abre conexión, solo calcula la URL+firma SigV4.
 *
 * Patrón replicado del backend de polizapp.
 */
class StorageService(private val cfg: StorageConfig) {

    private companion object {
        const val REGION = "us-east-1"
    }

    // Región fija: evita que el SDK consulte la ubicación del bucket por red al firmar/subir.
    // Clave para `firma`: su endpoint público NO es alcanzable desde el backend, así que sin
    // región fija getPresignedObjectUrl se colgaría intentando resolverla.
    private val io: MinioClient = MinioClient.builder()
        .endpoint(cfg.endpoint).region(REGION).credentials(cfg.accessKey, cfg.secretKey).build()

    private val firma: MinioClient = MinioClient.builder()
        .endpoint(cfg.publicEndpoint).region(REGION).credentials(cfg.accessKey, cfg.secretKey).build()

    /** Crea el bucket si no existe. Idempotente. Llamar al arrancar. */
    fun init() {
        val existe = io.bucketExists(BucketExistsArgs.builder().bucket(cfg.bucket).build())
        if (!existe) io.makeBucket(MakeBucketArgs.builder().bucket(cfg.bucket).build())
    }

    /** Sube un objeto y devuelve su key. */
    suspend fun subir(key: String, bytes: ByteArray, contentType: String): String =
        withContext(Dispatchers.IO) {
            ByteArrayInputStream(bytes).use { stream ->
                io.putObject(
                    PutObjectArgs.builder()
                        .bucket(cfg.bucket)
                        .`object`(key)
                        .stream(stream, bytes.size.toLong(), -1)
                        .contentType(contentType)
                        .build(),
                )
            }
            key
        }

    /** Borra un objeto. Idempotente: si no existe, no falla. */
    suspend fun borrar(key: String) = withContext(Dispatchers.IO) {
        runCatching {
            io.removeObject(RemoveObjectArgs.builder().bucket(cfg.bucket).`object`(key).build())
        }
        Unit
    }

    /** URL temporal de lectura (GET) firmada para que la app abra el objeto. */
    fun urlFirmada(key: String, expiraSegundos: Int = 3600): String =
        firma.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(cfg.bucket)
                .`object`(key)
                .expiry(expiraSegundos, TimeUnit.SECONDS)
                .build(),
        )
}
