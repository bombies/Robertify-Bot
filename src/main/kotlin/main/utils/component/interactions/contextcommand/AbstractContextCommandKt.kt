package main.utils.component.interactions.contextcommand

import main.utils.component.AbstractInteractionKt
import main.utils.component.interactions.contextcommand.models.ContextCommandKt
import net.dv8tion.jda.api.entities.Guild

abstract class AbstractContextCommandKt protected constructor(val info: ContextCommandKt) : AbstractInteractionKt() {

    fun loadCommand(guild: Guild) {
        guild.upsertCommand(info.getCommandData())
            .queue()
    }
}