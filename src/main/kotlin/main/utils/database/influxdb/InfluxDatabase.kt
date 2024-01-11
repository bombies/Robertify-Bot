package main.utils.database.influxdb

import main.main.Config

enum class InfluxDatabase {
    COMMAND_STATS,
    TRACK_STATS,
    PLAYER_STATS,
    GUILD_JOINS,
    GUILD_LEAVES;

    override fun toString(): String = "${name.lowercase()}-${Config.ENVIRONMENT}"
}