class Env(
    private val env: String
) {
    companion object {
        fun make(env: String): Env {
            when (env) {
                "development" -> {
                    return Env(
                        env
                    )
                }

                "production" -> {
                    return Env(
                        env
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
}
