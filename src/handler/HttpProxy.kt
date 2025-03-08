package handler

import RegistrationLoader
import io.muserver.MuHandler
import io.muserver.MuRequest
import io.muserver.MuResponse
import jakarta.ws.rs.NotFoundException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class HttpProxyHandler(
    private val registrationLoader: RegistrationLoader,
    private val httpClient: OkHttpClient
) : MuHandler {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    override fun handle(req: MuRequest, res: MuResponse): Boolean {
        val uri = req.relativePath().toString()
        val appName = uri.split("/")[1]

        val addressList = registrationLoader.findAddress(appName)

        if (addressList.isEmpty()) {
            throw NotFoundException()
        }

        val selectedAddress = addressList.random()

        val targetUri = URI.create("$selectedAddress$uri")
        logger.info("Attempt to proxy request to $targetUri")
        val contentType = (req.headers().get("content-type") ?: "application/octet-stream")
        val asyncHandle = req.handleAsync()
        val requestBody = req.readBodyAsString()
        var isClientOk = true
        httpClient.newCall(
            Request.Builder().method(
                req.method().toString(),
                if (requestBody == "") null else requestBody.toRequestBody(contentType.toMediaTypeOrNull())
            ).url(targetUri.toString()).build()
        ).enqueue(object: Callback {
            override fun onResponse(call: Call, response: Response) {
                res.headers().set("content-type", response.headers["content-type"])
                // Use output stream to respond in body
                // in case some requests is for resources download
                response.body?.use { targetBody ->
                    targetBody.byteStream().use { targetBodyStream ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (targetBodyStream.read(buffer).also { read = it } > -1) {
                            try {
                                if (read > 0) {
                                    asyncHandle.write(ByteBuffer.wrap(buffer, 0, read)).get(10, TimeUnit.SECONDS)
                                }
                            } catch (e: Exception) {
                                logger.error("Error while writing to client ${e.message}")
                                isClientOk = false
                                asyncHandle.complete(e)
                                break
                            }
                        }

                        if (isClientOk) {
                            asyncHandle.complete()
                        }
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                logger.error("Error while proxying request $uri to $targetUri, message: ${e.message}")
                asyncHandle.complete(e)
            }
        })
        return true
    }
}