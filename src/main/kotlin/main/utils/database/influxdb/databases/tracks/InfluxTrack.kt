package main.utils.database.influxdb.databases.tracks

import com.influxdb.annotations.Column
import com.influxdb.annotations.Measurement

@Measurement(name = "track-plays")
data class InfluxTrack(
    @Column(tag = true) val guild: String,
    @Column(tag = true) val requester: String,
    @Column(tag = true) val title: String,
    @Column(tag = true) val author: String,
    @Column val plays: Int = 1
)