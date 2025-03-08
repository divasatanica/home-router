package coma.personal.router.handler

import coma.personal.router.RegistrationLoader
import com.google.gson.Gson
import coma.personal.router.database.data.schema.Connectors
import io.muserver.MuRequest
import io.muserver.MuResponse
import io.muserver.RouteHandler
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.InternalServerErrorException
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class UnregisterPayload(val id: String)

class UnregisterHandler(
    private val registrationLoader: RegistrationLoader
): RouteHandler {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    override fun handle(req: MuRequest, res: MuResponse, p2: MutableMap<String, String>?) {
        val body = Gson().fromJson(req.readBodyAsString(), UnregisterPayload::class.java)

        if (body.id.isEmpty()) {
            throw BadRequestException("Lack 'id' parameter in request body")
        }

        transaction {
            val deletedRow = Connectors.deleteWhere { Connectors.id.eq(UUID.fromString(body.id)) }
            logger.info("Deleted row $deletedRow for id: ${body.id}")

            if (deletedRow == 0) {
                throw InternalServerErrorException("No record was deleted")
            }

            registrationLoader.refresh()

            res.write(Gson().toJson(object {
                val message = "Successfully unregistered"
                val code = 0
            }))
        }
    }
}