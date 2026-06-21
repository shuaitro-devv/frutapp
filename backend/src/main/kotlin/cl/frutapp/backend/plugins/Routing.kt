package cl.frutapp.backend.plugins

import cl.frutapp.backend.config.ConfigRepository
import cl.frutapp.backend.config.ConfigService
import cl.frutapp.backend.config.adminConfigRoutes
import cl.frutapp.backend.config.configRoutes
import cl.frutapp.backend.modules.admin.AdminOrderService
import cl.frutapp.backend.modules.admin.AdminUserService
import cl.frutapp.backend.modules.admin.adminOrderRoutes
import cl.frutapp.backend.modules.admin.adminUserRoutes
import cl.frutapp.backend.modules.auth.AuthService
import cl.frutapp.backend.modules.auth.authRoutes
import cl.frutapp.backend.modules.catalog.CatalogService
import cl.frutapp.backend.modules.catalog.catalogRoutes
import cl.frutapp.backend.modules.health.healthRoutes
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.media.AvatarService
import cl.frutapp.backend.modules.media.EvidenceService
import cl.frutapp.backend.modules.media.avatarRoutes
import cl.frutapp.backend.modules.media.evidenceRoutes
import cl.frutapp.backend.modules.notifications.DeviceTokenRepository
import cl.frutapp.backend.modules.notifications.NotificationInboxRepository
import cl.frutapp.backend.modules.notifications.deviceTokenRoutes
import cl.frutapp.backend.modules.notifications.notificationInboxRoutes
import cl.frutapp.backend.modules.pagos.WebpayPagoService
import cl.frutapp.backend.modules.pagos.pagoRoutes
import cl.frutapp.backend.modules.ubicacion.UbicacionService
import cl.frutapp.backend.modules.ubicacion.ubicacionRoutes
import cl.frutapp.backend.modules.chat.ChatHub
import cl.frutapp.backend.modules.chat.ChatService
import cl.frutapp.backend.modules.chat.chatRoutes
import cl.frutapp.backend.modules.reviews.ReviewService
import cl.frutapp.backend.modules.reviews.reviewRoutes
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.modules.orders.OrderService
import cl.frutapp.backend.modules.orders.orderRoutes
import cl.frutapp.backend.modules.staff.StaffOrderService
import cl.frutapp.backend.modules.staff.staffOrderRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(
    authService: AuthService,
    catalogService: CatalogService,
    orderService: OrderService,
    adminUserService: AdminUserService,
    adminOrderService: AdminOrderService,
    staffOrderService: StaffOrderService,
    userEventService: UserEventService,
    deviceTokenRepository: DeviceTokenRepository,
    notificationInboxRepository: NotificationInboxRepository,
    avatarService: AvatarService?,
    evidenceService: EvidenceService?,
    webpayPagoService: WebpayPagoService,
    ubicacionService: UbicacionService,
    chatService: ChatService,
    chatHub: ChatHub,
    tokenService: TokenService,
    configService: ConfigService,
    configRepository: ConfigRepository,
    reviewService: ReviewService,
) {
    routing {
        healthRoutes()
        authRoutes(authService, userEventService)
        catalogRoutes(catalogService)
        orderRoutes(orderService)
        staffOrderRoutes(staffOrderService, catalogService)
        configRoutes()
        adminConfigRoutes(configService, configRepository)
        adminUserRoutes(adminUserService)
        adminOrderRoutes(adminOrderService)
        deviceTokenRoutes(deviceTokenRepository)
        notificationInboxRoutes(notificationInboxRepository)
        if (avatarService != null) avatarRoutes(avatarService)
        if (evidenceService != null) evidenceRoutes(evidenceService)
        pagoRoutes(webpayPagoService)
        ubicacionRoutes(ubicacionService)
        chatRoutes(chatService, chatHub, tokenService)
        reviewRoutes(reviewService)
    }
}
