package main.utils.database.influxdb.databases.guilds

import main.utils.database.influxdb.AbstractInfluxDatabase
import main.utils.database.influxdb.InfluxDatabase
import net.dv8tion.jda.api.entities.Guild

object GuildJoinInfluxDatabase : AbstractInfluxDatabase(InfluxDatabase.GUILD_JOINS) {
    suspend fun recordJoin(guild: Guild) {
        writeMeasurement(InfluxGuild(guild = guild.id))
    }
}