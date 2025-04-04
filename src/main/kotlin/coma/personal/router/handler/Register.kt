package coma.personal.router.handler

import coma.personal.router.RegistrationLoader
import coma.personal.router.database.data.schema.Connectors
import io.muserver.MuRequest
import io.muserver.MuResponse
import io.muserver.RouteHandler
import org.jetbrains.exposed.sql.insertAndGetId
import com.google.gson.Gson
import jakarta.ws.rs.core.Response
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class RegistrationPayload(val appName: String, val address: String)

class RegisterHandler(
    private val registrationLoader: RegistrationLoader

): RouteHandler {
    private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
    override fun handle(request: MuRequest, response: MuResponse, paramMap: MutableMap<String, String>?) {
        try {
            val body = request.readBodyAsString()
            val result = Gson().fromJson(body, RegistrationPayload::class.java)
            val resultAppName = result.appName
            val resultAddress = result.address
            val fiveMinutesAgo = Instant.fromEpochMilliseconds(System.currentTimeMillis() - 300 * 1000)

            transaction {
                // Found existing connector registered within 5 minutes (MAX heartbeat interval), reuse it
                val existingList = Connectors.select(Connectors.fields)
                    .where {
                        Connectors.appName eq resultAppName
                    }.andWhere {
                        Connectors.address eq resultAddress
                    }
                    .limit(1)
                var id: EntityID<UUID>
                if (existingList.count() > 0 && existingList.first()[Connectors.lastActive]!! >= fiveMinutesAgo) {
                    id = existingList.first()[Connectors.id]
                    logger.info("Found existing connector with id: $id, reuse this connector")
                } else {
                    id = Connectors.insertAndGetId {
                        it[appName] = result.appName
                        it[address] = result.address
                        it[lastActive] = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                    }
                }

                registrationLoader.refresh()

                val responseResult = object {
                    val data = object {
                        val id = id.value
                    }
                    val message = "Successfully registered"
                    val code = 0
                }

                response.write(Gson().toJson(responseResult))
            }
        } catch (e: Exception) {
            response.status(Response.Status.INTERNAL_SERVER_ERROR.statusCode)
            response.write(e.message)
        }
    }
}