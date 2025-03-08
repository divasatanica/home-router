package coma.personal.router

import java.io.File

class Env(
    private val env: String,
    val secretDir: String
) {
    companion object {
        fun make(env: String): Env {
            when (env) {
                "development" -> {
                    return Env(
                        env,
                        secretDir = "/Users/mac/DockerShared/secret"
                    )
                }

                "production" -> {
                    return Env(
                        env,
                        secretDir = "/data/secret"
                    )
                }

                else -> {
                    throw RuntimeException("Unknown env parameter")
                }
            }
        }
    }

    fun speak(): String {
        return env
    }

    fun loadFromSecret(fileName: String): String {
        val file = File("$secretDir/$fileName").bufferedReader()

        val content = file.use {
            it.readText()
        }

        return content
    }
}
