package main.utils.managers

import main.constants.ENVKt
import main.constants.ToggleKt
import main.main.ConfigKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.database.mongodb.cache.BotDBCacheKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.time.Instant
import kotlin.random.Random

class RandomMessageManagerKt {
    var chance: Double = ConfigKt.RANDOM_MESSAGE_CHANCE
    val messages: List<String>
        get() = BotDBCacheKt.instance.getRandomMessages()

    val hasMessages: Boolean
        get() = messages.isEmpty()

    fun getMessage(guild: Guild): MessageEmbed {
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        val messages = BotDBCacheKt.instance.getRandomMessages()

        if (messages.isEmpty())
            throw NullPointerException(localeManager.getMessage(RobertifyLocaleMessageKt.RandomMessages.NO_RANDOM_MESSAGES))

        return RobertifyEmbedUtilsKt.embedMessage(guild, messages.get(Random.nextInt(messages.size)))
            .setTitle(localeManager.getMessage(RobertifyLocaleMessageKt.RandomMessages.TIP_TITLE))
            .setFooter(localeManager.getMessage(RobertifyLocaleMessageKt.RandomMessages.TIP_FOOTER))
            .setTimestamp(Instant.now())
            .build()
    }

    fun addMessage(message: String) =
        BotDBCacheKt.instance.addRandomMessage(message)

    fun removeMessage(id: Int) =
        BotDBCacheKt.instance.removeMessage(id)

    fun clearMessages() =
        BotDBCacheKt.instance.clearMessages()

    fun randomlySendMessage(channel: GuildMessageChannel) {
        val guild = channel.guild
        if (!TogglesConfigKt(guild).getToggle(ToggleKt.TIPS))
            return

        val requestChannelConfig = RequestChannelConfigKt(guild)
        if (requestChannelConfig.isChannelSet() && requestChannelConfig.channelId == channel.idLong)
            return

        if (!hasMessages)
            return

        if (Random.nextDouble() <= chance)
            channel.sendMessageEmbeds(getMessage(guild)).queue()
    }


}