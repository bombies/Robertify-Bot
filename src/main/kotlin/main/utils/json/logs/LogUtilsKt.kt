package main.utils.json.logs

import dev.minn.jda.ktx.coroutines.await
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.LocaleMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.time.Instant

class LogUtilsKt(private val guild: Guild) {
    private val config: LogConfig = LogConfig(guild)

    suspend fun sendLog(type: LogType, message: String) {
        if (!config.channelIsSet()) return
        if (!TogglesConfig(guild).getLogToggle(type)) return
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

    suspend fun sendLog(type: LogType, message: LocaleMessage) {
        if (!config.channelIsSet()) return
        if (!TogglesConfig(guild).getLogToggle(type)) return
        val localeManager = LocaleManager[guild]
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
    suspend fun sendLog(type: LogType, message: LocaleMessage, vararg placeholders: Pair<String, String>) {
        if (!config.channelIsSet()) return
        if (!TogglesConfig(guild).getLogToggle(type)) return
        val localeManager = LocaleManager[guild]
        val channel = config.getChannel()
        try {
            channel!!.sendMessageEmbeds(
                EmbedBuilder()
                    .setTitle(type.emoji.formatted + " " + type.title)
                    .setColor(type.color)
                    .setDescription(localeManager.getMessage(message, *placeholders))
                    .setTimestamp(Instant.now())
                    .build()
            ).queue()
        } catch (_: InsufficientPermissionException) {
        }
    }

    suspend fun createChannel() {
        if (config.channelIsSet()) config.removeChannel()
        val channel = guild.createTextChannel("robertify-logs")
            .addPermissionOverride(guild.publicRole, emptyList(), listOf(Permission.VIEW_CHANNEL))
            .addPermissionOverride(
                guild.selfMember,
                listOf(Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_SEND),
                emptyList()
            )
            .await()
        config.setChannelId(channel.idLong)
    }

}