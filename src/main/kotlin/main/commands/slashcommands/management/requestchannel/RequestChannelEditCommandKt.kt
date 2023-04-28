package main.commands.slashcommands.management.requestchannel

import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import main.audiohandlers.RobertifyAudioManagerKt
import main.constants.ToggleKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.toMention
import main.utils.RobertifyEmbedUtilsKt.Companion.replyWithEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendWithEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.json.requestchannel.RequestChannelKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.util.concurrent.atomic.AtomicLong

class RequestChannelEditCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "requestchannel",
        description = "Configure the request channel for your server.",
        adminOnly = true,
        subcommands = listOf(
            SubCommandKt(
                name = "setup",
                description = "Setup the request channel."
            ),
            SubCommandKt(
                name = "edit",
                description = "Edit the request channel."
            )
        )
    )
) {

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        private val threadContext = newFixedThreadPoolContext(4, "Request-Channel-Worker")
        private val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) = when (event.subcommandName) {
        "setup" -> handleSetup(event)
        else -> {}
    }

    internal suspend fun handleSetup(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!

        if (RequestChannelConfigKt(guild).isChannelSet()) {
            event.replyWithEmbed(guild) {
                embed(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_ALREADY_SETUP)
            }.queue()
            return
        }

        event.deferReply().queue()
        val channel = createRequestChannel(guild).await()

        try {
            event.hook.sendWithEmbed(guild) {
                embed(
                    RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP,
                    Pair("{channel}", channel.channelId.toMention(GeneralUtilsKt.Mentioner.CHANNEL))
                )
            }.queue()
        } catch (e: InsufficientPermissionException) {
            if (e.message?.contains("MESSAGE_HISTORY") == true)
                event.hook.sendWithEmbed(guild) {
                    embed(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP_2)
                }.queue()
            else logger.error("Unexpected error", e)
        }
    }

    private suspend fun createRequestChannel(guild: Guild): Deferred<RequestChannelKt> = withContext(threadContext) {
        val job = async {
            val config = RequestChannelConfigKt(guild)

            val textChannelId = AtomicLong()
            return@async guild.createTextChannel("robertify-requests")
                .submit()
                .thenCompose { channel ->
                    val theme = ThemesConfigKt(guild).theme
                    val localeManager = LocaleManagerKt.getLocaleManager(guild)
                    val manager = channel.manager

                    manager.setPosition(0).queue()
                    config.channelTopicUpdateRequest(channel)?.queue()

                    val embedBuilder = EmbedBuilder()
                    embedBuilder.setColor(theme.color)
                        .setTitle(localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_NOTHING_PLAYING))
                        .setImage(theme.idleBanner)
                    textChannelId.set(channel.idLong)

                    return@thenCompose channel.sendMessage(localeManager.getMessage(RobertifyLocaleMessageKt.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING))
                        .setEmbeds(embedBuilder.build())
                        .submit()
                }.thenApply { message ->
                    config.setChannelAndMessage(textChannelId.get(), message.idLong)
                    config.buttonUpdateRequest(message).queue()
                    config.originalAnnouncementToggle = TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES)

                    if (RobertifyAudioManagerKt.getMusicManager(guild).player.playingTrack != null)
                        config.updateMessage()

                    return@thenApply RequestChannelKt(
                        channelId = config.messageId,
                        messageId = config.channelId,
                        config = config.config.config
                    )
                }.join()
        }

        return@withContext job
    }
}