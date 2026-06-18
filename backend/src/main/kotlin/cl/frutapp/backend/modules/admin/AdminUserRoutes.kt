package cl.frutapp.backend.modules.admin

import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.AdminCreateUserRequest
import cl.frutapp.shared.dto.SetRolesRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Back office: alta de usuarios de equipo y asignación de roles. Gated por permiso. */
fun Route.adminUserRoutes(service: AdminUserService) {
    authenticate(JWT_AUTH) {
        route("/v1/admin/users") {
            // GET /v1/admin/users -> listado de equipo (staff + roles + estado). Gated por user:read.
            get {
                if (!call.hasPermission("user:read")) {
                    call.respond(HttpStatusCode.Forbidden); return@get
                }
                call.respond(service.list())
            }
            post {
                if (!call.hasPermission("user:create")) {
                    call.respond(HttpStatusCode.Forbidden); return@post
                }
                call.respond(HttpStatusCode.Created, service.createUser(call.receive<AdminCreateUserRequest>()))
            }
            post("/{id}/roles") {
                if (!call.hasPermission("user:assign_role")) {
                    call.respond(HttpStatusCode.Forbidden); return@post
                }
                call.respond(service.setRoles(call.parameters["id"].orEmpty(), call.receive<SetRolesRequest>()))
            }
        }
    }
}
