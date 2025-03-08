import coma.personal.router.database.DBManager
import coma.personal.router.handler.*
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
        val client = okhttp3.OkHttpClient().newBuilder().readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build()

        logger.info("Classpath: ${System.getProperty("java.class.path")}")
        DBManager.init(env)
        val registrationLoader = RegistrationLoader()
        registrationLoader.start()
        server
            .addHandler(TraceContext())
            .addHandler(LogMiddleware())
            .addHandler(HelloWorld())
            .addHandler(Method.POST, "/api/v1/register", RegisterHandler(registrationLoader))
            .addHandler(Method.POST, "/api/v1/unregister", UnregisterHandler(registrationLoader))
            .addHandler(Method.GET, "/api/v1/connectors", ConnectorsHandler())
            .addHandler(HttpProxyHandler(registrationLoader, client))

        logger.info("Server started on http://localhost:{}", port)
    }
}

fun main() {
    val app = App()
    app.start()
}