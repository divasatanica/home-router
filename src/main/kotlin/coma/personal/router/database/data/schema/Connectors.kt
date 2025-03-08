package coma.personal.router.database.data.schema

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

const val MAX_CHAR_LENGTH = 50

object Connectors: UUIDTable() {
    val appName = varchar("app_name", MAX_CHAR_LENGTH).nullable()
    val address = varchar("address", MAX_CHAR_LENGTH).nullable().uniqueIndex()
    val lastRegistered = timestamp("last_registered").nullable()

    init {
        index("service_under_app", false, appName, address)
    }
}