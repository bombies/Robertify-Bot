package main.commands.slashcommands.util.suggestions

import main.events.AbstractEventController
import main.utils.database.mongodb.cache.BotDBCache
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent

class SuggestionChannelEvents : AbstractEventController() {

    private val handleCategoryDeletion =
        onEvent<ChannelDeleteEvent> { event ->
            if (event.channelType != ChannelType.CATEGORY) return@onEvent
            val category = event.channel.asCategory()
            val config = BotDBCache.instance

            if (category.idLong != config.suggestionsCategoryId) return@onEvent

            val acceptedId = config.suggestionsAcceptedChannelId
            val deniedId = config.suggestionsDeniedChannelId
            val pendingId = config.suggestionsPendingChannelId

            category.channels.filter { channel ->
                channel.idLong == acceptedId ||
                        channel.idLong == deniedId ||
                        channel.idLong == pendingId
            }
                .forEach { channel -> channel.delete().queue() }

            config.resetSuggestionsConfig()
        }

    private val handleChannelDeletion =
        onEvent<ChannelDeleteEvent> { event ->
            if (event.channelType != ChannelType.TEXT) return@onEvent
            val channel = event.channel.asTextChannel()
            val config = BotDBCache.instance

            when (channel.idLong) {
                config.suggestionsAcceptedChannelId -> config.suggestionsAcceptedChannelId = -1L
                config.suggestionsDeniedChannelId -> config.suggestionsDeniedChannelId = -1L
                config.suggestionsPendingChannelId -> config.suggestionsPendingChannelId = -1L
            }
        }
}