package main.commands.slashcommands.management.requestchannel

import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.interactions.components.secondary
import dev.minn.jda.ktx.messages.send
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.loaders.MainAudioLoaderKt.Companion.queueThenDelete
import main.constants.RobertifyEmojiKt
import main.constants.ToggleKt
import main.main.RobertifyKt
import main.utils.GeneralUtilsKt
import main.utils.GeneralUtilsKt.toMention
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.requestchannel.RequestChannelButtonKt
import main.utils.json.requestchannel.RequestChannelConfigKt
import main.utils.json.requestchannel.RequestChannelKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.DedicatedChannelMessages
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.sharding.ShardManager
import java.io.IOException
import java.util.concurrent.TimeUnit
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

        private const val EDIT_BUTTON_ID = "togglerqchannel:%s:%s"
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) = when (event.subcommandName) {
        "setup" -> handleSetup(event)
        else -> handleEdit(event)
    }

    internal suspend fun handleSetup(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!

        if (RequestChannelConfigKt(guild).isChannelSet()) {
            event.replyEmbed(guild) {
                embed(DedicatedChannelMessages.DEDICATED_CHANNEL_ALREADY_SETUP)
            }.queue()
            return
        }

        event.deferReply().queue()
        val channel = createRequestChannel(guild).await()

        try {
            event.hook.sendEmbed(guild) {
                embed(
                    DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP,
                    Pair("{channel}", channel.channelId.toMention(GeneralUtilsKt.Mentioner.CHANNEL))
                )
            }.queue()
        } catch (e: InsufficientPermissionException) {
            if (e.message?.contains("MESSAGE_HISTORY") == true)
                event.hook.sendEmbed(guild) {
                    embed(DedicatedChannelMessages.DEDICATED_CHANNEL_SETUP_2)
                }.queue()
            else logger.error("Unexpected error", e)
        }
    }

    suspend fun createRequestChannel(guild: Guild, shardManager: ShardManager = RobertifyKt.shardManager): Deferred<RequestChannelKt> = withContext(threadContext) {
        val job = async {
            val config = RequestChannelConfigKt(guild, shardManager)

            val textChannelId = AtomicLong()
            return@async guild.createTextChannel("robertify-requests")
                .submit()
                .thenCompose { channel ->
                    val theme = ThemesConfigKt(guild).theme
                    val localeManager = LocaleManagerKt[guild]
                    val manager = channel.manager

                    manager.setPosition(0).queue()
                    config.channelTopicUpdateRequest(channel)?.queue()

                    val embedBuilder = EmbedBuilder()
                    embedBuilder.setColor(theme.color)
                        .setTitle(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_NOTHING_PLAYING))
                        .setImage(theme.idleBanner)
                    textChannelId.set(channel.idLong)

                    return@thenCompose channel.sendMessage(localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING))
                        .setEmbeds(embedBuilder.build())
                        .submit()
                }.thenApply { message ->
                    config.setChannelAndMessage(textChannelId.get(), message.idLong)
                    config.buttonUpdateRequest(message).queue()
                    config.originalAnnouncementToggle = TogglesConfigKt(guild).getToggle(ToggleKt.ANNOUNCE_MESSAGES)

                    try {
                        if (RobertifyAudioManagerKt[guild].player.playingTrack != null)
                            config.updateMessage()
                    }
                    catch (_ : UninitializedPropertyAccessException) {}

                    return@thenApply RequestChannelKt(
                        channelId = config.channelId,
                        messageId = config.messageId,
                        config = config.config.config
                    )
                }.join()
        }

        return@withContext job
    }

    fun deleteRequestChannel(guild: Guild, shardManager: ShardManager = RobertifyKt.shardManager) {
        RequestChannelConfigKt(guild, shardManager).removeChannel()
    }

    private fun handleEdit(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val config = RequestChannelConfigKt(guild)

        if (!config.isChannelSet()) {
            event.replyEmbed(guild) {
                embed(DedicatedChannelMessages.DEDICATED_CHANNEL_NOT_SET)
            }.queue()
            return
        }

        event.deferReply().queue()
        val localeManager = LocaleManagerKt[guild]
        val user = event.user

        event.hook.send(
            embeds = listOf(
                RobertifyEmbedUtilsKt.embedMessage(
                    guild,
                    DedicatedChannelMessages.DEDICATED_CHANNEL_EDIT_EMBED
                ).build()
            ),
            components = listOf(
                ActionRow.of(
                    primary(
                        id = EDIT_BUTTON_ID.format("previous", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PREVIOUS),
                        emoji = RobertifyEmojiKt.PREVIOUS_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("rewind", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_REWIND),
                        emoji = RobertifyEmojiKt.REWIND_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("pnp", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PLAY_AND_PAUSE),
                        emoji = RobertifyEmojiKt.PLAY_AND_PAUSE_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("stop", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_STOP),
                        emoji = RobertifyEmojiKt.STOP_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("skip", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SKIP),
                        emoji = RobertifyEmojiKt.END_EMOJI.emoji
                    ),
                ),
                ActionRow.of(
                    secondary(
                        id = EDIT_BUTTON_ID.format("favourite", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FAVOURITE),
                        emoji = RobertifyEmojiKt.STAR_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("loop", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_LOOP),
                        emoji = RobertifyEmojiKt.LOOP_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("shuffle", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SHUFFLE),
                        emoji = RobertifyEmojiKt.SHUFFLE_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("disconnect", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_DISCONNECT),
                        emoji = RobertifyEmojiKt.QUIT_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("filters", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FILTERS),
                        emoji = RobertifyEmojiKt.STAR_EMOJI.emoji
                    ),
                )
            ),
            ephemeral = true
        ).queue()
    }

    override suspend fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.button.id?.startsWith(EDIT_BUTTON_ID.split(":")[0]) != true) return

        val guild = event.guild!!
        val (_, buttonName, allowedUserId) = event.button.id!!.split(":")
        if (allowedUserId != event.user.id) {
            event.replyEmbed(guild) {
                embed(GeneralMessages.NO_PERMS_BUTTON)
            }
                .setEphemeral(true)
                .queue()
            return
        }

        event.deferReply().queue()

        val localeManager = LocaleManagerKt[guild]
        val config = RequestChannelConfigKt(guild)
        val subConfig = config.config
        val field: RequestChannelButtonKt
        val button: String

        when (buttonName) {
            "previous" -> {
                field = RequestChannelButtonKt.PREVIOUS
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PREVIOUS)
            }

            "rewind" -> {
                field = RequestChannelButtonKt.REWIND
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_REWIND)
            }

            "pnp", "play_and_pause" -> {
                field = RequestChannelButtonKt.PLAY_PAUSE
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PLAY_AND_PAUSE)
            }

            "stop" -> {
                field = RequestChannelButtonKt.STOP
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_STOP)
            }

            "skip" -> {
                field = RequestChannelButtonKt.SKIP
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SKIP)
            }

            "favourite" -> {
                field = RequestChannelButtonKt.FAVOURITE
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FAVOURITE)
            }

            "loop" -> {
                field = RequestChannelButtonKt.LOOP
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_LOOP)
            }

            "shuffle" -> {
                field = RequestChannelButtonKt.SHUFFLE
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SHUFFLE)
            }

            "disconnect" -> {
                field = RequestChannelButtonKt.DISCONNECT
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_DISCONNECT)
            }

            "filters" -> {
                field = RequestChannelButtonKt.FILTERS
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FILTERS)
            }

            else -> throw IllegalArgumentException("The button ID \"${event.button.id!!}\" doesn't map to a case to be handled!")
        }

        if (subConfig.getState(field)) {
            subConfig.setState(field, false)
            event.hook.sendEmbed(guild) {
                embed(
                    DedicatedChannelMessages.DEDICATED_CHANNEL_BUTTON_TOGGLE,
                    Pair("{button}", button),
                    Pair("{status}", localeManager.getMessage(GeneralMessages.OFF_STATUS))
                )
            }
                .queueThenDelete(15, TimeUnit.SECONDS)
        } else {
            subConfig.setState(field, true)
            event.hook.sendEmbed(guild) {
                embed(
                    DedicatedChannelMessages.DEDICATED_CHANNEL_BUTTON_TOGGLE,
                    Pair("{button}", button),
                    Pair("{status}", localeManager.getMessage(GeneralMessages.ON_STATUS))
                )
            }.queueThenDelete(15, TimeUnit.SECONDS)
        }

        config.updateButtons()
    }

}