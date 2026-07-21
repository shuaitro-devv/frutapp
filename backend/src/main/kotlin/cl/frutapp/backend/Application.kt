package cl.frutapp.backend

import cl.frutapp.backend.config.ConfigCache
import cl.frutapp.backend.config.ConfigRepository
import cl.frutapp.backend.config.ConfigService
import cl.frutapp.backend.config.JwtConfig
import cl.frutapp.backend.config.MailConfig
import cl.frutapp.backend.modules.auth.AuthService
import cl.frutapp.backend.modules.auth.EmailSender
import cl.frutapp.backend.modules.auth.EmailVerificationTokenRepository
import cl.frutapp.backend.modules.auth.LogEmailSender
import cl.frutapp.backend.modules.auth.PasswordResetTokenRepository
import cl.frutapp.backend.modules.auth.RefreshTokenRepository
import cl.frutapp.backend.modules.auth.ResendEmailSender
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.modules.admin.AdminOrderService
import cl.frutapp.backend.modules.admin.AdminUserService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.catalog.CatalogRepository
import cl.frutapp.backend.modules.catalog.CatalogService
import cl.frutapp.backend.modules.orders.FrutCoinsRepository
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderService
import cl.frutapp.backend.modules.rbac.PermissionCache
import cl.frutapp.backend.modules.rbac.RbacRepository
import cl.frutapp.backend.plugins.configureCors
import cl.frutapp.backend.plugins.configureRateLimit
import cl.frutapp.backend.plugins.configureRequestSizeLimit
import cl.frutapp.backend.plugins.configureDatabases
import cl.frutapp.backend.plugins.configureWebSocketsPlugin
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.media.AvatarService
import cl.frutapp.backend.modules.media.StorageService
import cl.frutapp.backend.modules.notifications.DeviceTokenRepository
import cl.frutapp.backend.modules.notifications.FcmSender
import cl.frutapp.backend.modules.notifications.NotificationDispatcher
import cl.frutapp.backend.modules.notifications.NotificationInboxRepository
import cl.frutapp.backend.config.StorageConfig
import cl.frutapp.backend.modules.staff.StaffOrderService
import java.io.File
import cl.frutapp.backend.plugins.configureMonitoring
import cl.frutapp.backend.plugins.configureRouting
import cl.frutapp.backend.plugins.configureSecurity
import cl.frutapp.backend.plugins.configureSerialization
import cl.frutapp.backend.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Entrada principal del backend FrutApp.
 *
 * `EngineMain` lee `application.yaml` y arranca Netty. El cableado (config → DB →
 * servicios → seguridad → rutas) es explícito y se mantiene legible/testeable.
 */
fun main(args: Array<String>) = EngineMain.main(args)

/**
 * Construye un [FcmSender] si la env FIREBASE_SERVICE_ACCOUNT_JSON esta presente.
 * Acepta dos formatos:
 *  - Inline JSON: el contenido directo de la service account (envuelto entre {...}).
 *  - Path a archivo: cualquier valor que no empiece con '{' se trata como ruta de
 *    archivo absoluto/relativo y se lee del disco.
 *
 * Si la env no esta o el contenido es invalido, devuelve null y FCM queda
 * deshabilitado — el resto del backend sigue funcionando normal.
 */
private fun loadFcmSender(app: Application): FcmSender? {
    val raw = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON")?.trim()
    if (raw.isNullOrBlank()) return null
    val json = if (raw.startsWith("{")) raw
    else runCatching { File(raw).readText(Charsets.UTF_8) }
        .getOrElse {
            app.environment.log.warn("FCM: no pude leer service account desde {} ({})", raw, it.message)
            return null
        }
    return runCatching { FcmSender(json) }
        .getOrElse {
            app.environment.log.warn("FCM: service account inválida ({})", it.message)
            null
        }
}

/**
 * Construye un [StorageService] (MinIO) si las env vars `STORAGE_*` están todas
 * presentes. Si falta alguna, devuelve null y avatar queda deshabilitado —
 * util en CI/dev sin MinIO local.
 *
 * `init()` crea el bucket si no existe (idempotente). Si MinIO no responde,
 * se loguea warning y se devuelve null.
 */
private fun loadStorageService(app: Application): StorageService? {
    val endpoint = System.getenv("STORAGE_ENDPOINT")
    val publicEndpoint = System.getenv("STORAGE_PUBLIC_ENDPOINT")
    val accessKey = System.getenv("STORAGE_ACCESS_KEY")
    val secretKey = System.getenv("STORAGE_SECRET_KEY")
    val bucket = System.getenv("STORAGE_BUCKET")
    if (endpoint.isNullOrBlank() || publicEndpoint.isNullOrBlank() ||
        accessKey.isNullOrBlank() || secretKey.isNullOrBlank() || bucket.isNullOrBlank()) {
        return null
    }
    return runCatching {
        val service = StorageService(
            StorageConfig(
                endpoint = endpoint,
                publicEndpoint = publicEndpoint,
                accessKey = accessKey,
                secretKey = secretKey,
                bucket = bucket
            )
        )
        service.init()
        service
    }.getOrElse {
        app.environment.log.warn("Storage: no pude conectar a MinIO ({})", it.message)
        null
    }
}

fun Application.module() {
    val jwtConfig = JwtConfig.from(environment.config)

    configureSerialization()
    configureCors()
    configureRateLimit()
    configureStatusPages()
    configureMonitoring()
    // RequestSizeLimit se instala DESPUES de Monitoring: Monitoring setea el
    // attribute CallStartTime que otros plugins downstream leen. Si nuestro
    // plugin cortaba el pipeline en fase Setup (antes de Monitoring), el
    // attribute no existia y los handlers siguientes (CallFailed hook)
    // tiraban IllegalStateException que StatusPages convertia en 400 con
    // "Solicitud invalida.", enmascarando el 413 real. Con este orden el
    // attribute ya esta seteado cuando nuestro hook responde 413.
    configureRequestSizeLimit()
    configureDatabases()
    configureWebSocketsPlugin()

    // Config de negocio desde BD (app_config): carga el caché antes de servir y luego
    // lo refresca periódicamente (cambiar un parámetro = editar la fila, sin redeploy).
    val configRepository = ConfigRepository()
    val rbacRepository = RbacRepository()
    runBlocking {
        runCatching { ConfigCache.refresh(configRepository) }
            .onFailure { environment.log.warn("Carga inicial de config falló (uso defaults)", it) }
        runCatching { PermissionCache.refresh(rbacRepository) }
            .onFailure { environment.log.warn("Carga inicial de permisos falló", it) }
    }

    val mailConfig = MailConfig.from(environment.config)
    val emailSender: EmailSender = if (mailConfig.enabled) {
        environment.log.info("Correo: Resend habilitado (from={})", mailConfig.from)
        ResendEmailSender(mailConfig.resendApiKey, mailConfig.from)
    } else {
        environment.log.info("Correo: modo demo (LogEmailSender). Define RESEND_API_KEY para enviar de verdad.")
        LogEmailSender()
    }

    val tokenService = TokenService(jwtConfig)
    // Storage S3-compatible (MinIO) — opcional. Si no está configurado (sin
    // env vars STORAGE_*), el backend sigue funcionando y avatar queda
    // deshabilitado silenciosamente (util en CI/dev sin MinIO local).
    val storageService: StorageService? = loadStorageService(this)
    val avatarService = storageService?.let { AvatarService(it) }
    // Repositorio de users compartido — UserRepository es sin estado, pero
    // extraerlo evita 3 instanciaciones + lo hace pasable a configureRouting
    // para el modulo publico de referrals (verify de codigos sin auth).
    val userRepository = UserRepository()
    val authService = AuthService(
        users = userRepository,
        refreshTokens = RefreshTokenRepository(),
        passwordResetTokens = PasswordResetTokenRepository(),
        emailVerificationTokens = EmailVerificationTokenRepository(),
        tokens = tokenService,
        emailSender = emailSender,
        rbac = rbacRepository,
        avatarUrlResolver = avatarService?.let { svc -> { uid -> svc.urlFor(uid) } }
    )
    val catalogRepository = CatalogRepository()
    val catalogService = CatalogService(catalogRepository)
    val orderRepository = OrderRepository()
    // FCM: opcional. Si FIREBASE_SERVICE_ACCOUNT_JSON (inline JSON o path a archivo)
    // no esta definido, el dispatcher queda en modo no-op y los push events se
    // descartan silenciosamente. Permite que el backend siga funcionando sin Firebase
    // (entornos de test, CI, dev local sin secret).
    val deviceTokenRepository = DeviceTokenRepository()
    val notificationInboxRepository = NotificationInboxRepository()
    val fcmSender: FcmSender? = loadFcmSender(this)
    val notificationDispatcher = NotificationDispatcher(
        orderRepository, deviceTokenRepository, notificationInboxRepository, fcmSender
    )
    // ChatRepository compartido entre OrderService (contador de no-leidos)
    // y ChatService (escritura/lectura del chat). Una instancia comun para
    // no abrir dos pools de query.
    val chatRepository = cl.frutapp.backend.modules.chat.ChatRepository()
    val orderService = OrderService(
        orderRepository,
        catalogRepository,
        FrutCoinsRepository(),
        onTransitionFired = { id, from, to ->
            notificationDispatcher.onOrderTransition(id, from, to)
        },
        onOrderCreated = { id, locId, numero ->
            notificationDispatcher.onOrderReadyForPickers(id, locId, numero)
        },
        onAjusteResuelto = { id, aprobado ->
            notificationDispatcher.onAjusteResueltoByCliente(id, aprobado)
        },
        chatUnreadOne = { id, rol -> chatRepository.contarNoLeidos(id, rol) },
        chatUnreadBatch = { ids, rol -> chatRepository.contarNoLeidosBatch(ids, rol) },
    )
    val adminUserService = AdminUserService(
        userRepository, rbacRepository, PasswordResetTokenRepository(), tokenService, emailSender
    )
    val adminOrderService = AdminOrderService(orderRepository)
    val configService = ConfigService(configRepository)
    val userEventService = UserEventService()
    val evidenceService = storageService?.let {
        cl.frutapp.backend.modules.media.EvidenceService(
            storage = it,
            repo = cl.frutapp.backend.modules.media.EvidenceRepository(),
            events = userEventService
        )
    }
    val webpayConfig = cl.frutapp.backend.config.WebpayConfig.from(environment.config)
    val webpayPagoService = cl.frutapp.backend.modules.pagos.WebpayPagoService(
        client = cl.frutapp.backend.modules.pagos.WebpayClient(webpayConfig),
        repo = cl.frutapp.backend.modules.pagos.WebpayRepository(),
        orders = orderRepository,
        events = userEventService,
        cfg = webpayConfig,
        // Tras confirmar el pago, recien ahi avisamos a los pickers que el
        // pedido esta en cola (en OrderService.create lo saltamos para
        // pedidos esperandoWebpay).
        onOrderPaid = { id, locId, numero ->
            notificationDispatcher.onOrderReadyForPickers(id, locId, numero)
        },
    )
    val ubicacionService = cl.frutapp.backend.modules.ubicacion.UbicacionService(
        cl.frutapp.backend.modules.ubicacion.UbicacionRepository()
    )
    val chatHub = cl.frutapp.backend.modules.chat.ChatHub()
    val chatService = cl.frutapp.backend.modules.chat.ChatService(
        repo = chatRepository,
        hub = chatHub,
        notifications = notificationDispatcher,
        storage = storageService,
    )
    val reviewService = cl.frutapp.backend.modules.reviews.ReviewService(
        repo = cl.frutapp.backend.modules.reviews.ReviewRepository(),
        storage = storageService,
    )
    val rewardService = cl.frutapp.backend.modules.rewards.RewardService(
        repo = cl.frutapp.backend.modules.rewards.RewardRepository(),
    )
    val basketService = cl.frutapp.backend.modules.baskets.BasketService(
        repo = cl.frutapp.backend.modules.baskets.BasketRepository(),
    )
    environment.log.info(
        if (webpayConfig.esSandbox) "Webpay: SANDBOX habilitado (creds publicas de integracion). returnUrl={}/v1/pagos/webpay/retorno"
        else "Webpay: PRODUCCION habilitada. returnUrl={}/v1/pagos/webpay/retorno",
        webpayConfig.returnUrlBase
    )
    val referralBonusService = cl.frutapp.backend.modules.referrals.ReferralBonusService(
        users = userRepository,
        events = userEventService,
    )
    val staffOrderService = StaffOrderService(
        userEventService,
        notificationDispatcher,
        avatarUrlResolver = avatarService?.let { svc -> { uid -> svc.urlFor(uid) } },
        referralBonusHook = { orderId ->
            // Fire-and-forget REAL (fix v0.1.18): la version anterior era
            // `runCatching { … }` inline suspend, que hacia el POST
            // /delivered esperar a que se calcule y persista el bono.
            // Ahora lanzamos en el scope del Application (SupervisorJob),
            // asi el repartidor recibe 204 al instante y el bono corre en
            // background. try/catch explicito para no comer
            // CancellationException si el server hace shutdown.
            launch {
                try {
                    referralBonusService.tryAwardOnFirstDelivery(orderId)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    environment.log.warn("Referral bonus fallo para pedido {}: {}", orderId, t.message)
                }
            }
            Unit
        },
    )

    configureSecurity(jwtConfig, tokenService)
    configureRouting(
        authService, catalogService, orderService,
        adminUserService, adminOrderService, staffOrderService, userEventService,
        deviceTokenRepository, notificationInboxRepository, avatarService, evidenceService,
        webpayPagoService,
        ubicacionService,
        chatService, chatHub, tokenService,
        configService, configRepository,
        reviewService,
        rewardService,
        basketService,
        userRepository,
    )

    // Refresca la config de negocio cada 60s (cambios en app_config sin redeploy).
    launch {
        while (true) {
            delay(60_000)
            runCatching { ConfigCache.refresh(configRepository) }
                .onFailure { environment.log.warn("Refresh de config falló", it) }
            runCatching { PermissionCache.refresh(rbacRepository) }
                .onFailure { environment.log.warn("Refresh de permisos falló", it) }
        }
    }

    if (fcmSender != null) {
        environment.log.info("FCM: push notifications HABILITADAS")
    } else {
        environment.log.info("FCM: sin service account → push deshabilitado (define FIREBASE_SERVICE_ACCOUNT_JSON)")
    }
    if (storageService != null) {
        environment.log.info("Storage: MinIO HABILITADO (avatar de perfil disponible)")
    } else {
        environment.log.info("Storage: sin MinIO → avatar deshabilitado (define STORAGE_ENDPOINT/ACCESS_KEY/SECRET_KEY)")
    }

    // Job de auto-cancel de pedidos CREADO (Webpay abandonado). Corre siempre
    // (no gated) — es limpieza operativa, no demo. Cadencia y timeout se leen
    // de app_config (BusinessConfig.PEDIDO_AUTOCANCEL_JOB_EVERY_MIN /
    // PEDIDO_TIMEOUT_MIN) asi que un admin puede tunearlos sin redeploy.
    launch {
        while (true) {
            val everyMin = cl.frutapp.backend.config.BusinessConfig.PEDIDO_AUTOCANCEL_JOB_EVERY_MIN
            delay(everyMin.toLong() * 60_000L)
            runCatching { orderService.autoCancelExpirados() }
                .onSuccess { n -> if (n > 0) environment.log.info("Auto-cancel: {} pedidos CREADO expirados", n) }
                .onFailure { environment.log.warn("Auto-cancel falló", it) }
        }
    }

    // Demo: auto-avance de pedidos (gated por env DEMO_AUTO_ADVANCE). En producción real = false.
    val demoAutoAdvance = environment.config.propertyOrNull("demo.autoAdvance")?.getString().toBoolean()
    if (demoAutoAdvance) {
        val seconds = environment.config.propertyOrNull("demo.autoAdvanceSeconds")?.getString()?.toLongOrNull() ?: 45L
        environment.log.info("Demo: auto-avance de pedidos ACTIVADO (cada {}s)", seconds)
        launch {
            while (true) {
                delay(seconds * 1000)
                runCatching { orderService.autoAdvanceAll() }
                    .onFailure { environment.log.warn("Auto-avance demo falló", it) }
            }
        }
    }
}
