package main.utils.json.logs

import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.LocaleMessage
import main.utils.locale.LocaleMessageKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.time.Instant

class LogUtilsKt(private val guild: Guild) {
    private val config: LogConfigKt = LogConfigKt(guild)

    fun sendLog(type: LogTypeKt, message: String) {
        if (!config.channelIsSet()) return
        if (!TogglesConfigKt(guild).getLogToggle(type)) return
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

    fun sendLog(type: LogTypeKt, message: LocaleMessageKt) {
        if (!config.channelIsSet()) return
        if (!TogglesConfigKt(guild).getLogToggle(type)) return
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
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
    fun sendLog(type: LogTypeKt, message: LocaleMessageKt, vararg placeholders: Pair<String, String>) {
        if (!config.channelIsSet()) return
        if (!TogglesConfigKt(guild).getLogToggle(type)) return
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
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