package main.utils.database.influxdb.databases.guilds

import main.utils.database.influxdb.AbstractInfluxDatabase
import main.utils.database.influxdb.InfluxDatabase

object GuildCountInfluxDatabase : AbstractInfluxDatabase(InfluxDatabase.GUILD_COUNT) {
    suspend fun recordCount(count: Int) {
        writeMeasurement(InfluxGuildCount(count))
    }
}