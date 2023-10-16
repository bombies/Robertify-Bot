package main.utils.managers

import main.constants.Toggle
import main.main.Config
import main.utils.RobertifyEmbedUtils
import main.utils.database.mongodb.cache.BotDBCache
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.RandomMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.time.Instant
import kotlin.random.Random

class RandomMessageManager {

    companion object {
        var chance: Double = Config.RANDOM_MESSAGE_CHANCE
    }

    val messages: List<String>
        get() = BotDBCache.instance.getRandomMessages()

    val hasMessages: Boolean
        get() = messages.isEmpty()

    fun getMessage(guild: Guild): MessageEmbed {
        val localeManager = LocaleManager[guild]
        val messages = BotDBCache.instance.getRandomMessages()

        if (messages.isEmpty())
            throw NullPointerException(localeManager.getMessage(RandomMessages.NO_RANDOM_MESSAGES))

        return RobertifyEmbedUtils.embedMessage(guild, messages.get(Random.nextInt(messages.size)))
            .setTitle(localeManager.getMessage(RandomMessages.TIP_TITLE))
            .setFooter(localeManager.getMessage(RandomMessages.TIP_FOOTER))
            .setTimestamp(Instant.now())
            .build()
    }

    operator fun plus(message: String) = BotDBCache.instance.addRandomMessage(message)

    fun addMessage(message: String) = plus(message)

    operator fun minus(id: Int) = BotDBCache.instance.removeMessage(id)

    fun removeMessage(id: Int): String = minus(id)

    operator fun unaryMinus() = BotDBCache.instance.clearMessages()

    fun clearMessages() = unaryMinus()

    fun randomlySendMessage(channel: GuildMessageChannel) {
        val guild = channel.guild
        if (!TogglesConfig(guild).getToggle(Toggle.TIPS))
            return

        val requestChannelConfig = RequestChannelConfig(guild)
        if (requestChannelConfig.isChannelSet() && requestChannelConfig.getChannelId() == channel.idLong)
            return

        if (!hasMessages)
            return

        if (Random.nextDouble() <= chance)
            channel.sendMessageEmbeds(getMessage(guild)).queue()
    }


}