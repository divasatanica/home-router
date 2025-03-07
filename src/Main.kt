import io.muserver.MuServerBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class App {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    fun start() {
        val env = Env.make("development")
        println(env.speak())

        val server = MuServerBuilder.muServer()
        val listener = server
            .withHttpPort(8088)
            .start()

        val port = listener.address().port
        logger.info("Server started on http://localhost:{}", port)

        server
            .addHandler(TraceContext())
            .addHandler(LogMiddleware())
            .addHandler(
                { req, res ->
                    res.write("Hello world")
                    true
                }
            )

    }
}

fun main() {
    val app = App()
    app.start()
}