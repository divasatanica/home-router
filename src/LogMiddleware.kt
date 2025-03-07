import io.muserver.MuHandler
import io.muserver.MuRequest
import io.muserver.MuResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LogMiddleware: MuHandler {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    override fun handle(req: MuRequest, res: MuResponse): Boolean {
        logger.info("Incoming request to ${req.method()} ${req.uri()}")
        
        return false
    }
}