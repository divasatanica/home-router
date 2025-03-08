package handler

import database.data.schema.Connectors
import io.muserver.MuRequest
import io.muserver.MuResponse
import io.muserver.RouteHandler
import org.jetbrains.exposed.sql.insertAndGetId
import com.google.gson.Gson
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.transactions.transaction

data class RegistrationPayload(val appName: String, val address: String)

class RegisterHandler: RouteHandler {
    override fun handle(request: MuRequest, response: MuResponse, paramMap: MutableMap<String, String>?) {
        try {
            val body = request.readBodyAsString()
            val result = Gson().fromJson(body, RegistrationPayload::class.java)
            transaction {
                val id = Connectors.insertAndGetId {
                    it[appName] = result.appName
                    it[address] = result.address
                    it[lastRegistered] = Instant.fromEpochMilliseconds(System.currentTimeMillis())
                }

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
            val responseResult = object {
                val message = e.message
                val code = -1
            }
            response.write(Gson().toJson(responseResult))
        }
    }
}