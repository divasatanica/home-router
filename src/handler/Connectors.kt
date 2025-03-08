package handler

import com.google.gson.Gson
import database.data.schema.Connectors
import io.muserver.MuRequest
import io.muserver.MuResponse
import io.muserver.RouteHandler
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class ConnectorItem(val appName: String, val address: String, val lastRegistered: Long)

class ConnectorsHandler: RouteHandler {
    override fun handle(req: MuRequest, res: MuResponse, p2: MutableMap<String, String>?) {
        transaction {
            val appNameMap: MutableMap<String, MutableList<ConnectorItem>> = mutableMapOf()
            Connectors.selectAll().forEach {
                val appName = it[Connectors.appName]!!
                // do map-reduce things
                if (appNameMap.containsKey(appName)) {
                    val existedConnectors = appNameMap[appName]!!
                    existedConnectors.add(ConnectorItem(appName, it[Connectors.address]!!, it[Connectors.lastRegistered]!!.toEpochMilliseconds()))
                    appNameMap[appName] = existedConnectors
                    return@forEach
                }

                appNameMap[appName] = mutableListOf(ConnectorItem(appName, it[Connectors.address]!!, it[Connectors.lastRegistered]!!.toEpochMilliseconds()))
            }

            val appNames = appNameMap.keys
            val data = appNames.map {
                val connectors = appNameMap[it]!!
                object {
                    val appName = it
                    val connectors = connectors
                }
            }
            val responseResult = object {
                val data = data
                val message = "Successfully fetched"
                val code = 0
            }

            res.write(Gson().toJson(responseResult))
        }
    }
}