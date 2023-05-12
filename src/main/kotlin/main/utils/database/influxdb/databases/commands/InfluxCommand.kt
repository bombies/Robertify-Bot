package main.utils.database.influxdb.databases.commands

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import java.time.Instant

@Measurement(name = "command-execution")
data class InfluxCommand(
    @Column(tag = true) val command: String,
    @Column(tag = true) val guild: String,
    @Column(tag = true) val executor: String,
    @Column val usage: Float = 1.0F,
    @Column(timestamp = true) val time: Instant,
)
