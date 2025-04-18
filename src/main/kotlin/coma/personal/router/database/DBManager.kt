package coma.personal.router.database

import coma.personal.router.Env
import coma.personal.router.database.data.schema.Connectors
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DBManager {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger("DBManager")
        fun init(env: Env): String {
            val url = connect(env)
            transaction {
                SchemaUtils.create(Connectors)
                logger.info("Database initialized")
            }
            return url
        }

        private fun connect(env: Env): String {
            val userNameAndSecretCombination = env.loadFromSecret("postgres")
            val contentList = userNameAndSecretCombination.replace("\n", "").split("@")
            val databaseHost = if (env.speak() == "development") "0.0.0.0:5432" else "host.docker.internal:5432"

            val instance = Database.connect("jdbc:postgresql://$databaseHost/registration", driver = "org.postgresql.Driver", contentList[0], contentList[1])

            logger.info("Connected to ${instance.url}")
            return instance.url
        }
    }

}