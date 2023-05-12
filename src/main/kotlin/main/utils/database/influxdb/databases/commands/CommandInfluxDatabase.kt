package main.utils.database.influxdb.databases.commands

import com.influxdb.query.FluxRecord
import kotlinx.coroutines.channels.Channel
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.database.influxdb.AbstractInfluxDatabase
import main.utils.database.influxdb.InfluxDatabase
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.time.Instant

object CommandInfluxDatabase : AbstractInfluxDatabase(InfluxDatabase.COMMAND_STATS) {

    suspend fun recordCommand(
        guild: Guild?,
        command: AbstractSlashCommand,
        executor: User
    ) {
        writeMeasurement(
            InfluxCommand(
                command = command.info.name,
                guild = guild?.id ?: "none",
                executor = executor.id,
                time = Instant.now()
            )
        )
    }

    suspend fun getRecordedCommands(
        command: AbstractSlashCommand,
        guild: Guild? = null,
        executor: User? = null
    ): Channel<FluxRecord>? {
        // TODO: Fix these queries

        val queries = mutableListOf<String>()
        if (guild != null && executor != null)
            queries += "filter(fn: (r) => r.command == \"${command.info.name}\" and r.guild == \"${guild.id}\" and r.executor == \"${executor.id}\")"
        else if (guild != null) {
            queries += "filter(fn: (r) => r.command == \"${command.info.name}\" and r.guild == \"${guild.id}\")"
        } else if (executor != null) {
            queries += "filter(fn: (r) => r.command == \"${command.info.name}\" and r.executor == \"${executor.id}\")"
        }
        return query(queries)
    }

    suspend fun getRecordedCommands(
        guild: Guild? = null,
        executor: User? = null
    ): Channel<FluxRecord>? {
        // TODO: Fix these queries

        val queries = mutableListOf("group(columns: ['command'])")
        if (guild != null && executor != null)
            queries += "filter(fn: (r) => r.guild == \"${guild.id}\" and r.executor == \"${executor.id}\")"
        else if (guild != null) {
            queries += "filter(fn: (r) => r.guild == \"${guild.id}\")"
        } else if (executor != null) {
            queries += "filter(fn: (r) => r.executor == \"${executor.id}\")"
        }

        return query(queries)
    }


}