package cl.frutapp.backend

import cl.frutapp.backend.config.BusinessConfig
import cl.frutapp.backend.config.ConfigCache
import cl.frutapp.backend.config.ConfigRepository
import cl.frutapp.backend.config.DbConfig
import cl.frutapp.backend.config.JwtConfig
import cl.frutapp.backend.db.DatabaseFactory
import cl.frutapp.backend.error.ConflictException
import cl.frutapp.backend.error.UnauthorizedException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.admin.AdminUserService
import cl.frutapp.backend.modules.auth.AuthService
import cl.frutapp.backend.modules.auth.Email
import cl.frutapp.backend.modules.auth.EmailSender
import cl.frutapp.backend.modules.auth.EmailVerificationTokenRepository
import cl.frutapp.backend.modules.auth.PasswordResetTokenRepository
import cl.frutapp.backend.modules.auth.RefreshTokenRepository
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.auth.UserRow
import cl.frutapp.backend.modules.catalog.CatalogRepository
import cl.frutapp.backend.modules.orders.FrutCoinsRepository
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderService
import cl.frutapp.backend.modules.orders.OrderStatus
import cl.frutapp.backend.modules.rbac.PermissionCache
import cl.frutapp.backend.modules.rbac.RbacRepository
import cl.frutapp.shared.dto.AdminCreateUserRequest
import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.LoginRequest
import cl.frutapp.shared.dto.RefreshRequest
import cl.frutapp.shared.dto.ResetPasswordRequest
import cl.frutapp.shared.dto.SetRolesRequest
import cl.frutapp.shared.dto.OrderItemRequest
import cl.frutapp.shared.dto.PaymentInput
import cl.frutapp.shared.dto.RegisterRequest
import cl.frutapp.shared.dto.TransitionRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests de integración contra un Postgres REAL pero EFÍMERO (Testcontainers): cada corrida
 * levanta el contenedor, corre Flyway (V1..V7) y al terminar lo destruye → cero data basura
 * en QA ni en ningún lado. Cubre el flujo crítico: config, auth, pedidos, máquina de estados.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BackendIntegrationTest {

    private val pg = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:16")).apply {
        withDatabaseName("frutapp"); withUsername("frutapp"); withPassword("frutapp")
    }

    private val users = UserRepository()
    private val rbac = RbacRepository()
    // Captura los correos para poder extraer códigos (verificación / invitación) en los tests.
    private val sentEmails = mutableListOf<Email>()
    private val emailSender = object : EmailSender {
        override suspend fun send(email: Email) { sentEmails.add(email) }
    }
    private val tokenService = TokenService(
        JwtConfig("test-secret-no-prod-1234567890", "frutapp-api", "frutapp-app", "frutapp", 15L, 60L)
    )
    private val auth = AuthService(
        users, RefreshTokenRepository(), PasswordResetTokenRepository(),
        EmailVerificationTokenRepository(), tokenService, emailSender, rbac
    )
    private val catalog = CatalogRepository()
    private val frutCoins = FrutCoinsRepository()
    private val orders = OrderService(OrderRepository(), catalog, frutCoins)
    private val adminUsers = AdminUserService(users, rbac, PasswordResetTokenRepository(), tokenService, emailSender)

    @BeforeAll
    fun setup() {
        // Si hay un Postgres EFÍMERO provisto por env (TEST_DB_*), úsalo (local Windows, donde
        // Testcontainers no autodetecta Docker). Si no, levanta uno con Testcontainers (CI/Linux).
        val envPort = System.getenv("TEST_DB_PORT")
        val dbConfig = if (!envPort.isNullOrBlank()) {
            DbConfig(
                host = System.getenv("TEST_DB_HOST") ?: "localhost",
                port = envPort.toInt(),
                name = System.getenv("TEST_DB_NAME") ?: "frutapp",
                user = System.getenv("TEST_DB_USER") ?: "frutapp",
                password = System.getenv("TEST_DB_PASSWORD") ?: "frutapp",
                maxPoolSize = 3
            )
        } else {
            pg.start()
            DbConfig(pg.host, pg.firstMappedPort, pg.databaseName, pg.username, pg.password, 3)
        }
        DatabaseFactory.init(dbConfig)
        runBlocking {
            ConfigCache.refresh(ConfigRepository())
            PermissionCache.refresh(rbac)
        }
    }

    @AfterAll
    fun teardown() {
        if (pg.isRunning) pg.stop()
    }

    // --- helpers ---

    private suspend fun registerVerified(): UserRow {
        val email = "t${System.nanoTime()}@frutapp.local"
        auth.register(RegisterRequest(name = "Test", email = email, phone = null, password = "abc123", consentVersion = "1.0"))
        val u = users.findByEmail(email) ?: error("usuario no creado")
        users.markEmailVerified(u.id)
        return u
    }

    private suspend fun kgProductPriced(): cl.frutapp.shared.dto.ProductDto =
        catalog.listProducts(null, null).first { it.unit == "kg" && it.priceClp >= 1000 }

    // --- tests ---

    @Test
    fun `config se carga de la BD y BusinessConfig la refleja`() {
        val byKey = runBlocking { ConfigRepository().loadAll() }.associateBy { it.key }
        assertEquals("2990", byKey["costo_envio"]?.value)
        assertEquals(2990, BusinessConfig.COSTO_ENVIO)
        assertEquals(15000, BusinessConfig.ENVIO_GRATIS_DESDE)
        assertEquals(0.20, BusinessConfig.FRUTCOINS_MAX_PORC_PAGO, 0.0001)
    }

    @Test
    fun `login bloqueado hasta verificar el correo`() = runBlocking {
        val email = "g${System.nanoTime()}@frutapp.local"
        auth.register(RegisterRequest("G", email, null, "abc123", "1.0"))
        assertFailsWith<UnauthorizedException> { auth.login(LoginRequest(email, "abc123")) }
        users.markEmailVerified(users.findByEmail(email)!!.id)
        val res = auth.login(LoginRequest(email, "abc123"))
        assertTrue(res.accessToken.isNotEmpty())
        assertTrue(res.refreshToken.isNotEmpty())
    }

    @Test
    fun `crear pedido reprecia subtotal, envio, total y frutcoins`() = runBlocking {
        val u = registerVerified()
        val p = kgProductPriced()
        val gramos = 1000; val cantidad = 2
        val dto = orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, cantidad, gramos))))
        val subEsperado = (p.priceClp * gramos / 1000) * cantidad
        val envioEsperado = if (subEsperado >= BusinessConfig.ENVIO_GRATIS_DESDE) 0 else BusinessConfig.COSTO_ENVIO
        assertEquals(subEsperado, dto.subtotalEstimado)
        assertEquals(envioEsperado, dto.envio)
        assertEquals(subEsperado + envioEsperado, dto.totalEstimado)
        assertEquals(dto.totalEstimado / 100, dto.frutcoinsGanadas)
        assertEquals("DELIVERY", dto.fulfillmentType)
        // sin payments en el request -> un solo medio (TARJETA) por el total
        assertEquals(1, dto.payments.size)
        assertEquals("TARJETA", dto.payments[0].method)
        assertEquals(dto.totalEstimado, dto.payments[0].monto)
    }

    @Test
    fun `retiro en sucursal no cobra envio`() = runBlocking {
        val u = registerVerified()
        val p = kgProductPriced()
        val dto = orders.create(
            u.id,
            CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 1, 250)), fulfillmentType = "RETIRO", sucursal = "Sucursal Test")
        )
        assertEquals(0, dto.envio)
        assertEquals("RETIRO", dto.fulfillmentType)
    }

    @Test
    fun `pago con FrutCoins se capa por el tope y por el saldo`() = runBlocking {
        val u = registerVerified()
        val p = kgProductPriced()
        // Pedido 1: gana FrutCoins (total alto)
        orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 5, 1000))))
        val saldo = frutCoins.balanceAndHistory(u.id).balance
        assertTrue(saldo > 0, "debería haber ganado FrutCoins")
        // Pedido 2 chico, pidiendo pagar con TODO el saldo en FrutCoins
        val o2 = orders.create(
            u.id,
            CreateOrderRequest(
                items = listOf(OrderItemRequest(p.id, 1, 100)),
                fulfillmentType = "RETIRO",
                payments = listOf(PaymentInput("FRUTCOINS", saldo), PaymentInput("TARJETA"))
            )
        )
        val tope = (o2.totalEstimado * BusinessConfig.FRUTCOINS_MAX_PORC_PAGO).toInt()
        val frut = o2.payments.first { it.method == "FRUTCOINS" }.monto
        val cash = o2.payments.first { it.method == "TARJETA" }.monto
        assertEquals(minOf(tope, saldo), frut)        // capado por min(20% del total, saldo)
        assertEquals(o2.totalEstimado - frut, cash)   // el resto lo cubre el otro medio
        // el ledger refleja el canje (saldo bajó por el gasto)
        val saldoFinal = frutCoins.balanceAndHistory(u.id).balance
        assertEquals(saldo - frut + o2.frutcoinsGanadas, saldoFinal)
    }

    @Test
    fun `maquina de estados valida transiciones`() {
        // Cuerpo de bloque (no expresión): assertFailsWith devuelve la excepción, y con
        // `= runBlocking{}` la función quedaría devolviendo ≠ Unit y JUnit5 la ignora.
        runBlocking {
            val u = registerVerified()
            val p = kgProductPriced()
            val o = orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 1, 1000))))
            // válido: PAGADO -> EN_PICKING
            val t1 = orders.transition(o.id, TransitionRequest("EN_PICKING"))
            assertEquals("EN_PICKING", t1.status)
            // inválido: EN_PICKING -> ENTREGADO (saltarse pasos)
            assertFailsWith<ValidationException> { orders.transition(o.id, TransitionRequest("ENTREGADO")) }
        }
    }

    @Test
    fun `auto-avance mueve el pedido un paso por el camino feliz`() = runBlocking {
        val u = registerVerified()
        val p = kgProductPriced()
        val o = orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 1, 1000))))
        assertEquals("PAGADO", o.status)
        orders.autoAdvanceAll()
        val after = orders.detail(u.id, o.id)
        assertEquals("EN_PICKING", after.status)
    }

    @Test
    fun `usuario registrado obtiene rol cliente`() = runBlocking {
        val u = registerVerified()
        assertTrue(rbac.rolesOf(u.id).contains("cliente"))
    }

    @Test
    fun `admin crea staff con rol e invitacion`() {
        runBlocking {
            val email = "staff${System.nanoTime()}@frutapp.local"
            val dto = adminUsers.createUser(AdminCreateUserRequest("Picker Uno", email, null, listOf("picker")))
            assertTrue(dto.roles.contains("picker"))
            val u = users.findByEmail(email)!!
            assertTrue(u.emailVerified)                     // creado por admin -> verificado
            assertEquals(listOf("picker"), rbac.rolesOf(u.id))
            // contraseña aleatoria: no puede loguear hasta fijarla por invitación
            assertFailsWith<UnauthorizedException> { auth.login(LoginRequest(email, "abc123")) }
        }
    }

    @Test
    fun `invitacion - el staff fija su clave y entra`() {
        runBlocking {
            sentEmails.clear()
            val email = "inv${System.nanoTime()}@frutapp.local"
            adminUsers.createUser(AdminCreateUserRequest("Inv Staff", email, null, listOf("picker")))
            // el código de 6 dígitos viaja en el correo de invitación
            val code = Regex("\\d{6}").find(sentEmails.last().text)!!.value
            // el staff fija su clave con el flujo de reset existente y entra
            auth.resetPassword(ResetPasswordRequest(email, code, "nueva123"))
            val res = auth.login(LoginRequest(email, "nueva123"))
            assertTrue(res.accessToken.isNotEmpty())
            assertTrue(rbac.rolesOf(users.findByEmail(email)!!.id).contains("picker"))
        }
    }

    @Test
    fun `validaciones rechazan entradas invalidas`() {
        runBlocking {
            assertFailsWith<ValidationException> { auth.register(RegisterRequest("X", "no-es-correo", null, "abc123", "1.0")) }
            val email = "dup${System.nanoTime()}@frutapp.local"
            auth.register(RegisterRequest("X", email, null, "abc123", "1.0"))
            assertFailsWith<ConflictException> { auth.register(RegisterRequest("X", email, null, "abc123", "1.0")) }
            users.markEmailVerified(users.findByEmail(email)!!.id)
            assertFailsWith<UnauthorizedException> { auth.login(LoginRequest(email, "otra999")) }
            assertFailsWith<ValidationException> {
                adminUsers.createUser(AdminCreateUserRequest("Y", "staffx${System.nanoTime()}@frutapp.local", null, listOf("rol-inexistente")))
            }
        }
    }

    @Test
    fun `refresh rota y revoca el token anterior`() {
        runBlocking {
            val u = registerVerified()
            val first = auth.login(LoginRequest(u.email, "abc123"))
            val rotated = auth.refresh(RefreshRequest(first.refreshToken))
            assertTrue(rotated.accessToken.isNotEmpty())
            // el refresh anterior ya quedó revocado (rotación)
            assertFailsWith<UnauthorizedException> { auth.refresh(RefreshRequest(first.refreshToken)) }
        }
    }

    @Test
    fun `admin agrega y quita roles`() = runBlocking {
        val email = "staff2${System.nanoTime()}@frutapp.local"
        val created = adminUsers.createUser(AdminCreateUserRequest("Staff Dos", email, null, listOf("picker")))
        val after = adminUsers.setRoles(created.id, SetRolesRequest(add = listOf("repartidor"), remove = listOf("picker")))
        assertEquals(setOf("repartidor"), after.roles.toSet())
    }

    @Test
    fun `allowedActions segun permisos del rol`() {
        val admin = PermissionCache.permissionsForRoles(listOf("admin"))
        val picker = PermissionCache.permissionsForRoles(listOf("picker"))
        val cliente = PermissionCache.permissionsForRoles(listOf("cliente"))
        // desde PAGADO: admin -> EN_PICKING + CANCELADO; picker -> solo EN_PICKING; cliente -> nada
        assertEquals(setOf("EN_PICKING", "CANCELADO"), OrderStatus.allowedActions(OrderStatus.PAGADO, admin).toSet())
        assertEquals(setOf("EN_PICKING"), OrderStatus.allowedActions(OrderStatus.PAGADO, picker).toSet())
        assertTrue(OrderStatus.allowedActions(OrderStatus.PAGADO, cliente).isEmpty())
    }

    @Test
    fun `permisos se resuelven por rol`() {
        // cliente: puede crear pedidos, NO avanzar estados
        assertTrue(PermissionCache.has(listOf("cliente"), "order:create"))
        assertFalse(PermissionCache.has(listOf("cliente"), "order:transition"))
        // admin y picker: sí pueden avanzar estados
        assertTrue(PermissionCache.has(listOf("admin"), "order:transition"))
        assertTrue(PermissionCache.has(listOf("picker"), "order:transition"))
    }
}
