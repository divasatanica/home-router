package coma.personal.router

import coma.personal.router.database.data.schema.Connectors
import coma.personal.router.handler.AppWithConnectors
import coma.personal.router.handler.ConnectorsHandler
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledThreadPoolExecutor
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.util.UUID

class RegistrationLoader {
    private val registrationData: MutableList<AppWithConnectors> = mutableListOf()
    private val heartBeatMap: MutableMap<String, Long> = mutableMapOf()
    private val executor = ScheduledThreadPoolExecutor(1)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    fun start() {
        logger.info("Scheduler started")
        loadRegistrationToMemory()
        registrationData.forEach { it ->
            it.connectors.forEach {
                heartBeatMap[it.id] = System.currentTimeMillis()
            }
        }

        executor.scheduleWithFixedDelay({
            loadRegistrationToMemory()
        }, 0, 60, java.util.concurrent.TimeUnit.SECONDS)
        executor.scheduleWithFixedDelay({
            checkHeartBeatSignal()
        }, 0, 60, java.util.concurrent.TimeUnit.SECONDS)
    }

    fun findAddress(appName: String): List<String> {
        val data = registrationData.find { it.appName == appName }
        return data?.connectors?.map { it.address } ?: listOf()
    }

    fun refresh() {
        loadRegistrationToMemory()
    }

    fun pingWithConnectorId(id: String) {
        heartBeatMap[id] = System.currentTimeMillis()
    }

    private fun checkHeartBeatSignal() {
        val connectorIds = heartBeatMap.keys
        val unhealthyConnector = connectorIds.filter { System.currentTimeMillis() - heartBeatMap[it]!! > 2 * 60 * 1000 }.map{ UUID.fromString(it) }

        transaction {
            val rows = Connectors.deleteWhere { id.inList(unhealthyConnector) }

            logger.info("Removed $rows unhealthy connectors")
        }
    }

    private fun loadRegistrationToMemory() {
        val data = ConnectorsHandler.aggregateConnectors()

        registrationData.removeAll { true }
        registrationData.addAll(data)
    }
}