package coma.personal.router.handler

import com.google.gson.Gson
import coma.personal.router.RegistrationLoader
import io.muserver.MuRequest
import io.muserver.MuResponse
import io.muserver.RouteHandler

data class PingPayload(val connectorId: String)

class PingHandler(
    private val registrationLoader: RegistrationLoader
): RouteHandler {
    override fun handle(request: MuRequest, response: MuResponse, p2: Map<String?, String?>) {
        val body = request.readBodyAsString()
        val result = Gson().fromJson(body, PingPayload::class.java)

        registrationLoader.pingWithConnectorId(result.connectorId)

        response.write("OK")
    }
}