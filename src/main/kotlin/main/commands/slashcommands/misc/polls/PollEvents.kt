package main.commands.slashcommands.misc.polls

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.misc.polls.PollCommand.Companion.pollCache
import main.events.AbstractEventController
import main.utils.GeneralUtils
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent

class PollEvents : AbstractEventController() {
    companion object {
        private val logger by SLF4J
    }

    private val handleReactionAdd = onEvent<MessageReactionAddEvent> { event ->
        if (!pollCache.containsKey(event.messageIdLong)) return@onEvent
        event.retrieveUser().queue { user ->
            if (user.isBot) return@queue

            val emoji = event.reaction.emoji.name
            val countMap = pollCache[event.messageIdLong]!!
            var oldVal = countMap[GeneralUtils.parseNumEmoji(emoji) - 1]!!
            countMap[GeneralUtils.parseNumEmoji(emoji) - 1] = ++oldVal
        }
    }

    private val handleReactionRemove = onEvent<MessageReactionRemoveEvent> { event ->
        if (!pollCache.containsKey(event.messageIdLong)) return@onEvent
        event.retrieveUser().queue { user ->
            if (user.isBot) return@queue

            val emoji = event.reaction.emoji.name
            val countMap = pollCache[event.messageIdLong]!!
            var oldVal = countMap[GeneralUtils.parseNumEmoji(emoji)]!!
            countMap[GeneralUtils.parseNumEmoji(emoji)] = --oldVal
        }
    }
}