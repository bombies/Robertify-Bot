package main.utils.database.influxdb.databases.guilds

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement

@Measurement(name = "guild-joins-leaves")
data class InfluxGuild(
    @Column(tag = true) val guild: String,
    @Column val value: Int = 1
)
