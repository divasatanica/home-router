package coma.personal.router.handler

import com.google.gson.Gson
import coma.personal.router.database.data.schema.Connectors
import io.muserver.MuRequest
import io.muserver.MuResponse
import io.muserver.RouteHandler
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

data class ConnectorItem(val appName: String, val address: String, val lastRegistered: Long)
data class AppWithConnectors(val appName: String, val connectors: List<ConnectorItem>)

class ConnectorsHandler: RouteHandler {
    override fun handle(req: MuRequest, res: MuResponse, p2: MutableMap<String, String>?) {
        val data = aggregateConnectors()
        val responseResult = object {
            val data = data
            val message = "Successfully fetched"
            val code = 0
        }

        res.write(Gson().toJson(responseResult))
    }

    companion object {
        fun aggregateConnectors(): List<AppWithConnectors> {
            val appNameMap: MutableMap<String, MutableList<ConnectorItem>> = mutableMapOf()
            transaction {
                Connectors.selectAll().forEach {
                    val appName = it[Connectors.appName]!!
                    // do map-reduce things
                    if (appNameMap.containsKey(appName)) {
                        val existedConnectors = appNameMap[appName]!!
                        existedConnectors.add(
                            ConnectorItem(
                                appName,
                                it[Connectors.address]!!,
                                it[Connectors.lastRegistered]!!.toEpochMilliseconds()
                            )
                        )
                        appNameMap[appName] = existedConnectors
                        return@forEach
                    }

                    appNameMap[appName] = mutableListOf(
                        ConnectorItem(
                            appName,
                            it[Connectors.address]!!,
                            it[Connectors.lastRegistered]!!.toEpochMilliseconds()
                        )
                    )
                }

            }

            val appNames = appNameMap.keys
            val data = appNames.map {
                val connectorsList = appNameMap[it]!!
                AppWithConnectors(appName = it, connectors = connectorsList)
            }

            return data
        }
    }
}