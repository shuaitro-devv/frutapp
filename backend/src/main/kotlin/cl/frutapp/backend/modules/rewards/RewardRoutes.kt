package cl.frutapp.backend.modules.rewards

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.CanjearFrutCoinsRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

/**
 * Rutas de FrutCoins canje.
 *
 *  - POST /v1/frutcoins/redeem          (auth)   canjear monto → cupon
 *  - GET  /v1/frutcoins/cupones         (auth)   listar mis cupones
 *  - POST /v1/frutcoins/cupones/{id}/usar (auth) marcar usado
 *
 * El POST recibe en body { monto, recompensa, idempotencyKey } — el
 * idempotencyKey lo genera el cliente con UUID v4 random al abrir el
 * dialogo de canje y lo reenvia en cada reintento (red mala etc).
 */
fun Route.rewardRoutes(service: RewardService) {

    authenticate(JWT_AUTH) {

        post("/v1/frutcoins/redeem") {
            val uid = call.userId()
            val body = call.receive<CanjearFrutCoinsRequest>()
            val dto = service.canjear(
                userId = uid,
                monto = body.monto,
                recompensa = body.recompensa,
                idempotencyKey = body.idempotencyKey,
            )
            call.respond(HttpStatusCode.Created, dto)
        }

        get("/v1/frutcoins/cupones") {
            val uid = call.userId()
            call.respond(service.listar(uid))
        }

        post("/v1/frutcoins/cupones/{id}/usar") {
            val uid = call.userId()
            val cuponId = call.parameters["id"]
                ?: throw ValidationException("Falta el id del cupon.")
            service.usar(uid, cuponId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
