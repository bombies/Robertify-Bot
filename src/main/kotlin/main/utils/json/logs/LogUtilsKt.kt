package main.utils.json.logs

import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.LocaleMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.internal.utils.tuple.Pair
import java.time.Instant

class LogUtilsKt(private val guild: Guild) {
    private val config: LogConfigKt = LogConfigKt(guild)

    fun sendLog(type: LogType, message: String) {
        if (!config.channelIsSet()) return
        if (!TogglesConfig.getConfig(guild).getLogToggle(type)) return
        val channel = config.getChannel()
        channel!!.sendMessageEmbeds(
            EmbedBuilder()
                .setTitle(type.emoji.formatted + " " + type.title)
                .setColor(type.color)
                .setDescription(message)
                .setTimestamp(Instant.now())
                .build()
        ).queue()
    }

    fun sendLog(type: LogType, message: LocaleMessage) {
        if (!config.channelIsSet()) return
        if (!TogglesConfig.getConfig(guild).getLogToggle(type)) return
        val localeManager = LocaleManager.getLocaleManager(guild)
        val channel = config.getChannel()
        channel!!.sendMessageEmbeds(
            EmbedBuilder()
                .setTitle(type.emoji.formatted + " " + type.title)
                .setColor(type.color)
                .setDescription(localeManager.getMessage(message))
                .setTimestamp(Instant.now())
                .build()
        ).queue()
    }

    @SafeVarargs
    fun sendLog(type: LogType, message: LocaleMessage, vararg placeholders: Pair<String, String>) {
        if (!config.channelIsSet()) return
        if (!TogglesConfig.getConfig(guild).getLogToggle(type)) return
        val localeManager = LocaleManager.getLocaleManager(guild)
        val channel = config.getChannel()
        channel!!.sendMessageEmbeds(
            EmbedBuilder()
                .setTitle(type.emoji.formatted + " " + type.title)
                .setColor(type.color)
                .setDescription(localeManager.getMessage(message, *placeholders))
                .setTimestamp(Instant.now())
                .build()
        ).queue()
    }

    fun createChannel() {
        if (config.channelIsSet()) config.removeChannel()
        guild.createTextChannel("robertify-logs")
            .addPermissionOverride(guild.publicRole, emptyList(), listOf(Permission.VIEW_CHANNEL))
            .addPermissionOverride(
                guild.selfMember,
                listOf(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_SEND),
                emptyList()
            )
            .queue { channel: TextChannel -> config.setChannel(channel.idLong) }
    }

}