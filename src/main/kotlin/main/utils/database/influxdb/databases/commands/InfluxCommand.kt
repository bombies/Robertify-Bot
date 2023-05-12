package main.utils.database.influxdb.databases.commands

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement
import java.time.Instant

// TODO: Implement a better structure
@Measurement(name = "command-execution")
data class InfluxCommand(
    @Column(tag = true) val command: String,
    @Column val guild: String,
    @Column val executor: String,
    @Column(timestamp = true) val time: Instant
)
