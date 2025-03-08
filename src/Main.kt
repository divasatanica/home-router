import database.DBManager
import handler.*
import io.muserver.Method
import io.muserver.MuServerBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class App {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    fun start(presetEnv: String = "production") {
        val env = Env.make(presetEnv)
        logger.info("Application now running on ${env.speak()}")

        val server = MuServerBuilder.muServer()
        val listener = server
            .withHttpPort(8088)
            .start()

        val port = listener.address().port

        DBManager.init(env)
        server
            .addHandler(TraceContext())
            .addHandler(LogMiddleware())
            .addHandler(HelloWorld())
            .addHandler(Method.POST, "/api/v1/register", RegisterHandler())
            .addHandler(Method.GET, "/api/v1/connectors", ConnectorsHandler())

        logger.info("Server started on http://localhost:{}", port)
    }
}

fun main() {
    val app = App()
    app.start()
}