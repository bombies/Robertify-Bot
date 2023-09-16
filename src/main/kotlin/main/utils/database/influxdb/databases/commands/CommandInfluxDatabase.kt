package main.utils.database.influxdb.databases.commands

import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import kotlinx.coroutines.channels.Channel
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.database.influxdb.AbstractInfluxDatabase
import main.utils.database.influxdb.InfluxDatabase
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.time.Instant
import java.time.temporal.ChronoUnit

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
        executor: User? = null,
        range: LongRange? = null,
        rangeUnit: ChronoUnit = ChronoUnit.DAYS
    ): Channel<FluxRecord>? =
        query {
            var fluxRet: Flux = flux
            if (range != null)
                fluxRet = fluxRet.range(range.first, range.last, rangeUnit)

            val restrictions = mutableListOf(
                Restrictions.measurement().equal("command-execution"),
                Restrictions.tag("command").equal(command.info.name),
            )

            if (guild != null)
                restrictions.add(Restrictions.tag("guild").equal(guild.id))

            if (executor != null)
                restrictions.add(Restrictions.tag("executor").equal(executor.id))

            fluxRet
                .groupBy(listOf("_measurement", "_field"))
                .filter(Restrictions.and(*restrictions.toTypedArray()))
                .count()
                .yield("count")
        }


}