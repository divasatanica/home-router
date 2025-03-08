package coma.personal.router.handler

import io.muserver.MuHandler
import io.muserver.MuRequest
import io.muserver.MuResponse

class HelloWorld: MuHandler {
    override fun handle(request: MuRequest, response: MuResponse): Boolean {
        if (request.uri().equals("/hello")) {
            response.write("Hello world")
            return true
        }

        return false
    }
}