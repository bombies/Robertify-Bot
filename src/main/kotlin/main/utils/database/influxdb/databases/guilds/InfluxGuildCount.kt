package main.utils.database.influxdb.databases.guilds

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement

@Measurement(name = "guild-count")
data class InfluxGuildCount(
    @Column val count: Int
)
