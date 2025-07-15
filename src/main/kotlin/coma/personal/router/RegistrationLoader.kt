package coma.personal.router

import coma.personal.router.database.data.schema.Connectors
import coma.personal.router.handler.AppWithConnectors
import coma.personal.router.handler.ConnectorsHandler
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledThreadPoolExecutor
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.update
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

        // Check and refresh health of connectors every 60 seconds
        executor.scheduleWithFixedDelay({
            try {
                loadRegistrationToMemory()
            } catch (e: Exception) {
                logger.error("Failed to load registration data", e)
            }
        }, 60, 60, java.util.concurrent.TimeUnit.SECONDS)
        // Store latest activity of connectors every 2 minutes
        executor.scheduleWithFixedDelay({
            try {
                persistConnectorsActivity()
            } catch (e: Exception) {
                logger.error("Failed to persist connectors activity", e)
            }
        }, 2 * 60, 2 * 60, java.util.concurrent.TimeUnit.SECONDS)
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

    private fun persistConnectorsActivity() {
        var updatedCount = 0;
        var totalCount = 0;
        transaction {
            heartBeatMap.forEach { (id, lastActive) ->
                totalCount += 1;
                val updated = Connectors.update ({ Connectors.id eq UUID.fromString(id) }) { it ->
                    it[Connectors.lastActive] = Instant.fromEpochMilliseconds(lastActive)
                }
                if (updated == 0) {
                    heartBeatMap.remove(id)
                }
                updatedCount += updated
            }
        }

        logger.info("Persisted $updatedCount out of $totalCount connectors' activity")
    }

    private fun checkHeartBeatSignal() {
        val connectorIds = heartBeatMap.keys
        val unhealthyConnector = connectorIds.filter { System.currentTimeMillis() - heartBeatMap[it]!! > 5 * 60 * 1000 }.map{ UUID.fromString(it) }

        transaction {
            val rows = Connectors.deleteWhere { id.inList(unhealthyConnector) }

            logger.info("Removed $rows unhealthy connectors")
        }
    }

    private fun loadRegistrationToMemory() {
        val data = ConnectorsHandler.aggregateConnectors()
        data.forEach {
            it.connectors.forEach { connector ->
                heartBeatMap[connector.id] = connector.lastActive
            }
        }
        checkHeartBeatSignal()

        registrationData.removeAll { true }
        registrationData.addAll(data)
    }
}