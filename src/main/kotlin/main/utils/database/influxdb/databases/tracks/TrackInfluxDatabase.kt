package main.utils.database.influxdb.databases.tracks

import main.audiohandlers.models.Requester
import main.utils.database.influxdb.AbstractInfluxDatabase
import main.utils.database.influxdb.InfluxDatabase
import net.dv8tion.jda.api.entities.Guild

object TrackInfluxDatabase : AbstractInfluxDatabase(InfluxDatabase.TRACK_STATS) {

    suspend fun recordTrack(
        title: String,
        author: String,
        guild: Guild,
        requester: Requester?
    ) {
        writeMeasurement(
            InfluxTrack(
                guild = guild.id,
                requester = requester?.id ?: "unknown",
                title = title,
                author = author
            )
        )
    }
}