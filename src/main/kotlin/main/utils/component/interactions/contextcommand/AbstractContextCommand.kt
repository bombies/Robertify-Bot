package main.utils.component.interactions.contextcommand

import main.utils.component.GenericInteraction
import main.utils.component.interactions.contextcommand.models.ContextCommand
import net.dv8tion.jda.api.entities.Guild

abstract class AbstractContextCommand protected constructor(val info: ContextCommand) : GenericInteraction {

    fun loadCommand(guild: Guild) {
        guild.upsertCommand(info.getCommandData())
            .queue()
    }
}