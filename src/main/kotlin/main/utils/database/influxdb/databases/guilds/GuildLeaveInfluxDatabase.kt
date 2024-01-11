package main.utils.database.influxdb.databases.guilds

import main.utils.database.influxdb.AbstractInfluxDatabase
import main.utils.database.influxdb.InfluxDatabase
import net.dv8tion.jda.api.entities.Guild

object GuildLeaveInfluxDatabase : AbstractInfluxDatabase(InfluxDatabase.GUILD_LEAVES) {
    suspend fun recordLeave(guild: Guild) {
        writeMeasurement(InfluxGuild(guild = guild.id))
    }
}