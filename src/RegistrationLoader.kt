import handler.AppWithConnectors
import handler.ConnectorsHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledThreadPoolExecutor

class RegistrationLoader {
    private val registrationData: MutableList<AppWithConnectors> = mutableListOf()
    private val executor = ScheduledThreadPoolExecutor(1)
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    fun start() {
        logger.info("Scheduler started")
        loadRegistrationToMemory()
        executor.scheduleWithFixedDelay({
            logger.info("Loading registration data to memory")
            loadRegistrationToMemory()
        }, 0, 20, java.util.concurrent.TimeUnit.SECONDS)
    }

    fun getRegistration(): List<AppWithConnectors> {
        return registrationData
    }

    fun findAddress(appName: String): List<String> {
        val data = registrationData.find { it.appName == appName }
        return data?.connectors?.map { it.address } ?: listOf()
    }

    private fun loadRegistrationToMemory() {
        val data = ConnectorsHandler.aggregateConnectors()

        registrationData.removeAll { true }
        registrationData.addAll(data)
    }
}