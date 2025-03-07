import io.muserver.MuHandler
import io.muserver.MuRequest
import io.muserver.MuResponse
import kotlin.uuid.ExperimentalUuidApi

const val INVALID_TRACE_ID_FORMAT = "00000000000000000000000000000000"
const val INVALID_PARENT_ID_FORMAT = "0000000000000000"

class TraceContext: MuHandler {

    override fun handle(req: MuRequest, res: MuResponse): Boolean {
        val version = "00"
        var traceId = generateBytes(16)

        while (traceId == INVALID_TRACE_ID_FORMAT) {
            traceId = generateBytes(16)
        }

        var parentId = generateBytes(8)

        while (parentId == INVALID_PARENT_ID_FORMAT) {
            parentId = generateBytes(8)
        }

        val flags = "01"

        req.headers().set("transparent", "$version-$traceId-$parentId-$flags")
        res.headers().set("transparent", "$version-$traceId-$parentId-$flags")

        return false
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateBytes(bytes: Int = 16): String {
        return kotlin.uuid.Uuid.random().toString().split("-").joinToString("").slice(IntRange(0, bytes * 2 - 1))
    }
}