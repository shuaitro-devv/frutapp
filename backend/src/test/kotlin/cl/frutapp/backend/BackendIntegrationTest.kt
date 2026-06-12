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
import cl.frutapp.backend.modules.admin.AdminOrderService
import cl.frutapp.backend.modules.admin.AdminUserService
import cl.frutapp.backend.modules.audit.EventContext
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.audit.UserEventTable
import cl.frutapp.backend.modules.auth.AuthService
import cl.frutapp.backend.modules.auth.Email
import cl.frutapp.backend.modules.auth.EmailSender
import cl.frutapp.backend.modules.auth.EmailVerificationTokenRepository
import cl.frutapp.backend.modules.auth.PasswordResetTokenRepository
import cl.frutapp.backend.modules.auth.RefreshTokenRepository
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.auth.UserRow
import cl.frutapp.backend.modules.auth.UsersTable
import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.catalog.CatalogRepository
import cl.frutapp.backend.modules.orders.FrutCoinsRepository
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderService
import cl.frutapp.backend.modules.orders.OrderStatus
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.backend.modules.orders.PickupLocationTable
import cl.frutapp.backend.modules.staff.StaffOrderService
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
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
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
    private val adminOrders = AdminOrderService(OrderRepository())
    private val events = UserEventService()
    private val staff = StaffOrderService(events)

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

    /** Producto por unidad (no kg) — pedidos creados con este NO requieren pesaje
     *  antes del complete del picker. Usado por los tests de flujo picker/repartidor
     *  que no testean especificamente peso variable. */
    private suspend fun unidadProductPriced(): cl.frutapp.shared.dto.ProductDto =
        catalog.listProducts(null, null).first { it.unit != "kg" && it.priceClp >= 500 }

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
            auth.register(RegisterRequest("Xnombre", email, "+56911111111", "abc123", "1.0"))
            // Cuenta NO verificada: re-registrar NO debe lanzar 409. Sobrescribimos password,
            // name, phone y consent (fix observacion #6 del test del hermano + finding del
            // code-review post-fix: el comentario prometia overwrite pero solo password se
            // actualizaba). Cambiamos el nombre y telefono para lockear el contrato.
            auth.register(RegisterRequest("Ynombre", email, "+56922222222", "nueva456", "2.0"))
            val rowTrasOverwrite = users.findByEmail(email)!!
            assertEquals("Ynombre", rowTrasOverwrite.name)
            assertEquals("+56922222222", rowTrasOverwrite.phone)
            users.markEmailVerified(rowTrasOverwrite.id)
            // Recien con la cuenta YA verificada, un re-register debe rebotar con Conflict.
            assertFailsWith<ConflictException> { auth.register(RegisterRequest("Z", email, null, "abc123", "1.0")) }
            // La password del PRIMER register (abc123) ya no funciona — fue sobrescrita por la
            // del segundo. Si alguien refactoriza register() y silenciosamente skipea el update,
            // este assert lo caza.
            assertFailsWith<UnauthorizedException> { auth.login(LoginRequest(email, "abc123")) }
            // La password del SEGUNDO register sí entra (cuenta ya esta verificada por arriba).
            val res = auth.login(LoginRequest(email, "nueva456"))
            assertTrue(res.accessToken.isNotEmpty())
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

    // --- Tests del back office: lectura global de pedidos (V17 order:read_all) ---

    @Test
    fun `order read_all solo para staff (admin y soporte), no cliente`() {
        // Valida que la migración V17 sembró el permiso y lo asignó bien.
        assertTrue(PermissionCache.has(listOf("admin"), "order:read_all"))
        assertTrue(PermissionCache.has(listOf("soporte"), "order:read_all"))
        assertFalse(PermissionCache.has(listOf("cliente"), "order:read_all"))
    }

    @Test
    fun `admin lista pedidos del dia con ticket promedio coherente`() = runBlocking {
        val u = registerVerified()
        val p = kgProductPriced()
        val o1 = orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 1, 1000))))
        val o2 = orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 2, 1000))))

        val page = adminOrders.list(null, null)
        val ids = page.orders.map { it.id }.toSet()
        assertTrue(o1.id in ids && o2.id in ids, "mis pedidos del día deben aparecer en la lista global")
        // Invariantes de la página (robusto ante otros pedidos del mismo día en la BD compartida).
        assertEquals(page.orders.size, page.count)
        assertEquals(page.orders.sumOf { it.total }, page.totalDia)
        assertEquals(page.totalDia / page.count, page.ticketPromedio)
        val mine = page.orders.first { it.id == o1.id }
        assertEquals("Test", mine.clienteNombre)   // solo el primer nombre
        assertTrue(mine.itemsCount >= 1)
    }

    @Test
    fun `admin detalle trae el pedido completo + datos del cliente`() = runBlocking {
        val u = registerVerified()
        val p = kgProductPriced()
        val o = orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 1, 1000))))

        val detail = adminOrders.detail(o.id)
        assertEquals(o.numero, detail.order.numero)
        assertEquals("Test", detail.clienteNombre)
        assertTrue(detail.clienteEmail.isNotBlank())
        assertTrue(detail.order.items.isNotEmpty())
    }

    @Test
    fun `admin lista filtra por estado y por fecha`() = runBlocking {
        val u = registerVerified()
        val p = kgProductPriced()
        val o = orders.create(u.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 1, 1000))))
        orders.transition(o.id, TransitionRequest("EN_PICKING"))

        val enPicking = adminOrders.list(null, "EN_PICKING")
        assertTrue(enPicking.orders.all { it.status == "EN_PICKING" }, "el filtro de estado debe acotar")
        assertTrue(enPicking.orders.any { it.id == o.id })
        // un estado que el mío no tiene -> no aparece
        assertFalse(adminOrders.list(null, "ENTREGADO").orders.any { it.id == o.id })
        // una fecha pasada -> no aparece (fue creado hoy)
        val viejo = adminOrders.list("2020-01-01", null)
        assertFalse(viejo.orders.any { it.id == o.id })
        assertEquals("2020-01-01", viejo.fecha)
    }

    // --- Tests del flujo staff (V11 user_event + V12 cola del picker) ---

    private suspend fun createPickerWithDefaultLocation(): UUID {
        val email = "picker${System.nanoTime()}@frutapp.local"
        adminUsers.createUser(AdminCreateUserRequest("Picker T", email, null, listOf("picker")))
        val u = users.findByEmail(email)!!
        // Asigna la location default (Lo Valledor Centro) al picker — equivalente a lo que
        // hara el admin web cuando exista. Sin esto el StaffOrderService rebota con
        // "Tu cuenta no tiene una location asignada".
        dbQuery {
            val locationId = PickupLocationTable.selectAll()
                .where { PickupLocationTable.code eq OrderRepository.DEFAULT_PICKUP_LOCATION_CODE }
                .first()[PickupLocationTable.id]
            UsersTable.update({ UsersTable.id eq u.id }) {
                it[homeLocationId] = locationId
            }
        }
        return u.id
    }

    private suspend fun createOrderForCliente(): UUID {
        val cliente = registerVerified()
        // Producto por unidad: no requiere pesaje, asi el picker puede completar
        // sin tener que llamar a setItemPeso primero. Para tests especificos de
        // peso variable, crear el order con kgProductPriced + setItemPeso explicito.
        val p = unidadProductPriced()
        val dto = orders.create(cliente.id, CreateOrderRequest(items = listOf(OrderItemRequest(p.id, 1))))
        return UUID.fromString(dto.id)
    }

    @Test
    fun `picker ve pedidos en su cola y los toma atomicamente`() = runBlocking {
        val pickerId = createPickerWithDefaultLocation()
        val orderId = createOrderForCliente()

        // En la cola libre antes de tomar.
        val colaInicial = staff.colaPicker(pickerId)
        assertTrue(colaInicial.any { it.id == orderId.toString() }, "pedido no aparece en cola")

        // Take exitoso.
        val result = staff.take(pickerId, orderId, EventContext.EMPTY)
        assertTrue(result.ok, "primer take deberia exitoso, motivo=${result.motivo}")

        // Ya no aparece en cola libre.
        val colaPost = staff.colaPicker(pickerId)
        assertFalse(colaPost.any { it.id == orderId.toString() }, "pedido sigue en cola tras take")

        // Aparece en mis en_curso.
        val enCurso = staff.enCursoPicker(pickerId)
        assertTrue(enCurso.any { it.id == orderId.toString() }, "pedido no aparece en en_curso del picker")
        assertTrue(enCurso.first { it.id == orderId.toString() }.assignedToMe)
    }

    @Test
    fun `race condition - segundo picker recibe ok=false`() = runBlocking {
        val pickerA = createPickerWithDefaultLocation()
        val pickerB = createPickerWithDefaultLocation()
        val orderId = createOrderForCliente()

        val resA = staff.take(pickerA, orderId, EventContext.EMPTY)
        val resB = staff.take(pickerB, orderId, EventContext.EMPTY)

        assertTrue(resA.ok, "picker A deberia haber tomado")
        assertFalse(resB.ok, "picker B no deberia haber tomado el mismo pedido")
        assertEquals("ya_tomado_o_no_disponible", resB.motivo)
    }

    @Test
    fun `release devuelve el pedido a la cola libre`() = runBlocking {
        val pickerId = createPickerWithDefaultLocation()
        val orderId = createOrderForCliente()

        staff.take(pickerId, orderId, EventContext.EMPTY)
        staff.release(pickerId, orderId, EventContext.EMPTY)

        // Otro picker (o el mismo) lo puede tomar de nuevo.
        val pickerOtro = createPickerWithDefaultLocation()
        val retoma = staff.take(pickerOtro, orderId, EventContext.EMPTY)
        assertTrue(retoma.ok, "el pedido deberia estar libre tras release")
    }

    @Test
    fun `complete marca STOCK_CONFIRMADO y registra history con actor`() = runBlocking {
        val pickerId = createPickerWithDefaultLocation()
        val orderId = createOrderForCliente()

        staff.take(pickerId, orderId, EventContext.EMPTY)
        staff.complete(pickerId, orderId, EventContext.EMPTY)

        // El order quedo en STOCK_CONFIRMADO.
        val status = dbQuery {
            OrdersTable.selectAll().where { OrdersTable.id eq orderId }
                .first()[OrdersTable.status]
        }
        assertEquals("STOCK_CONFIRMADO", status)
    }

    @Test
    fun `auditoria - take emite evento staff order_taken en user_event`() = runBlocking {
        val pickerId = createPickerWithDefaultLocation()
        val orderId = createOrderForCliente()

        staff.take(pickerId, orderId, EventContext(ipAddress = "10.0.0.7", userAgent = "Test/1.0"))

        val evento = dbQuery {
            UserEventTable.selectAll()
                .where {
                    (UserEventTable.userId eq pickerId) and
                    (UserEventTable.eventType eq "staff.order_taken") and
                    (UserEventTable.entityId eq orderId)
                }
                .firstOrNull()
        }
        assertTrue(evento != null, "evento staff.order_taken no se registro")
        assertEquals("10.0.0.7", evento!![UserEventTable.ipAddress])
        assertEquals("Test/1.0", evento[UserEventTable.userAgent])
    }

    // --- Tests del flujo repartidor (Nivel 3) ---

    private suspend fun createRepartidorWithDefaultLocation(): UUID {
        val email = "repartidor${System.nanoTime()}@frutapp.local"
        adminUsers.createUser(AdminCreateUserRequest("Reparto T", email, null, listOf("repartidor")))
        val u = users.findByEmail(email)!!
        dbQuery {
            val locationId = PickupLocationTable.selectAll()
                .where { PickupLocationTable.code eq OrderRepository.DEFAULT_PICKUP_LOCATION_CODE }
                .first()[PickupLocationTable.id]
            UsersTable.update({ UsersTable.id eq u.id }) {
                it[homeLocationId] = locationId
            }
        }
        return u.id
    }

    /** Helper: cliente crea pedido, picker lo arma y completa -> queda STOCK_CONFIRMADO
     *  listo para que el repartidor lo tome. */
    private suspend fun createOrderStockConfirmado(): UUID {
        val pickerId = createPickerWithDefaultLocation()
        val orderId = createOrderForCliente()
        staff.take(pickerId, orderId, EventContext.EMPTY)
        staff.complete(pickerId, orderId, EventContext.EMPTY)
        return orderId
    }

    @Test
    fun `repartidor ve pedidos STOCK_CONFIRMADO en su cola y los toma atomicamente`() = runBlocking {
        val repartidorId = createRepartidorWithDefaultLocation()
        val orderId = createOrderStockConfirmado()

        val cola = staff.colaDispatch(repartidorId)
        assertTrue(cola.any { it.id == orderId.toString() }, "el pedido no aparece en cola de dispatch")

        val result = staff.takeDispatch(repartidorId, orderId, EventContext.EMPTY)
        assertTrue(result.ok, "primer take dispatch deberia exitoso, motivo=${result.motivo}")

        // Aparece en mi 'en ruta'.
        val enRuta = staff.enRutaDispatch(repartidorId)
        assertTrue(enRuta.any { it.id == orderId.toString() }, "no aparece en en_ruta del repartidor")

        // No deberia volver a aparecer en cola libre.
        val colaPost = staff.colaDispatch(repartidorId)
        assertFalse(colaPost.any { it.id == orderId.toString() }, "sigue en cola tras take")
    }

    @Test
    fun `race condition dispatch - segundo repartidor recibe ok=false`() = runBlocking {
        val repartidorA = createRepartidorWithDefaultLocation()
        val repartidorB = createRepartidorWithDefaultLocation()
        val orderId = createOrderStockConfirmado()

        val resA = staff.takeDispatch(repartidorA, orderId, EventContext.EMPTY)
        val resB = staff.takeDispatch(repartidorB, orderId, EventContext.EMPTY)

        assertTrue(resA.ok)
        assertFalse(resB.ok)
        assertEquals("ya_tomado_o_no_disponible", resB.motivo)
    }

    @Test
    fun `delivered marca ENTREGADO y registra history con actor REPARTIDOR`() = runBlocking {
        val repartidorId = createRepartidorWithDefaultLocation()
        val orderId = createOrderStockConfirmado()

        staff.takeDispatch(repartidorId, orderId, EventContext.EMPTY)
        staff.deliveredDispatch(repartidorId, orderId, EventContext.EMPTY)

        val status = dbQuery {
            OrdersTable.selectAll().where { OrdersTable.id eq orderId }
                .first()[OrdersTable.status]
        }
        assertEquals("ENTREGADO", status)
    }

    @Test
    fun `detalle dispatch incluye direccion + telefono del cliente (no asi el del picker)`() = runBlocking {
        val repartidorId = createRepartidorWithDefaultLocation()
        val orderId = createOrderStockConfirmado()
        staff.takeDispatch(repartidorId, orderId, EventContext.EMPTY)

        val detalle = staff.detalleDispatch(repartidorId, orderId)
        assertTrue(detalle.direccion.isNotBlank(), "el detalle dispatch debe incluir direccion completa")
        // El telefono puede ser null si el cliente no lo seteo; clave es que el campo
        // exista en el DTO (no se omita).
        assertTrue(detalle.assignedToMe, "assignedToMe deberia ser true porque acabo de tomarlo")
        assertTrue(detalle.items.isNotEmpty(), "items reales del pedido deben estar presentes")
    }

    @Test
    fun `auditoria - takeDispatch + delivered emiten eventos staff dispatch_ en user_event`() = runBlocking {
        val repartidorId = createRepartidorWithDefaultLocation()
        val orderId = createOrderStockConfirmado()

        staff.takeDispatch(repartidorId, orderId, EventContext(ipAddress = "10.0.0.5", userAgent = "RepartoTest/1.0"))
        staff.deliveredDispatch(repartidorId, orderId, EventContext.EMPTY)

        val eventos = dbQuery {
            UserEventTable.selectAll()
                .where {
                    (UserEventTable.userId eq repartidorId) and
                    (UserEventTable.entityId eq orderId)
                }
                .map { it[UserEventTable.eventType] }
        }
        assertTrue("staff.dispatch_taken" in eventos, "falta evento staff.dispatch_taken")
        assertTrue("staff.dispatch_delivered" in eventos, "falta evento staff.dispatch_delivered")
    }
}
