package main.commands.slashcommands.management.requestchannel

import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.interactions.components.secondary
import dev.minn.jda.ktx.messages.send
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.loaders.MainAudioLoader.Companion.queueThenDelete
import main.constants.RobertifyEmoji
import main.constants.Toggle
import main.main.Robertify
import main.utils.GeneralUtils
import main.utils.GeneralUtils.toMention
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.json.requestchannel.RequestChannelButton
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.json.requestchannel.RequestChannel
import main.utils.json.themes.ThemesConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.DedicatedChannelMessages
import main.utils.locale.messages.GeneralMessages
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class RequestChannelEditCommand : AbstractSlashCommand(
    Command(
        name = "requestchannel",
        description = "Configure the request channel for your server.",
        adminOnly = true,
        subcommands = listOf(
            SubCommand(
                name = "setup",
                description = "Setup the request channel."
            ),
            SubCommand(
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

        if (RequestChannelConfig(guild).isChannelSet()) {
            event.replyEmbed {
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
                    Pair("{channel}", channel.channelId.toMention(GeneralUtils.Mentioner.CHANNEL))
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

    suspend fun createRequestChannel(
        guild: Guild,
        shardManager: ShardManager = Robertify.shardManager
    ): Deferred<RequestChannel> = withContext(threadContext) {
        val job = async {
            val config = RequestChannelConfig(guild, shardManager)

            val textChannelId = AtomicLong()
            return@async guild.createTextChannel("robertify-requests")
                .submit()
                .thenCompose { channel ->
                    val theme = ThemesConfig(guild).theme
                    val localeManager = LocaleManager[guild]
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
                    config.originalAnnouncementToggle = TogglesConfig(guild).getToggle(Toggle.ANNOUNCE_MESSAGES)

                    try {
                        if (RobertifyAudioManager[guild].player.playingTrack != null)
                            config.updateMessage()
                    } catch (_: UninitializedPropertyAccessException) {
                    }

                    return@thenApply RequestChannel(
                        channelId = config.channelId,
                        messageId = config.messageId,
                        config = config.config.config
                    )
                }.join()
        }

        return@withContext job
    }

    fun deleteRequestChannel(guild: Guild, shardManager: ShardManager = Robertify.shardManager) {
        RequestChannelConfig(guild, shardManager).removeChannel()
    }

    private fun handleEdit(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val config = RequestChannelConfig(guild)

        if (!config.isChannelSet()) {
            event.replyEmbed {
                embed(DedicatedChannelMessages.DEDICATED_CHANNEL_NOT_SET)
            }.queue()
            return
        }

        event.deferReply().queue()
        val localeManager = LocaleManager[guild]
        val user = event.user

        event.hook.send(
            embeds = listOf(
                RobertifyEmbedUtils.embedMessage(
                    guild,
                    DedicatedChannelMessages.DEDICATED_CHANNEL_EDIT_EMBED
                ).build()
            ),
            components = listOf(
                ActionRow.of(
                    primary(
                        id = EDIT_BUTTON_ID.format("previous", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PREVIOUS),
                        emoji = RobertifyEmoji.PREVIOUS_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("rewind", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_REWIND),
                        emoji = RobertifyEmoji.REWIND_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("pnp", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PLAY_AND_PAUSE),
                        emoji = RobertifyEmoji.PLAY_AND_PAUSE_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("stop", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_STOP),
                        emoji = RobertifyEmoji.STOP_EMOJI.emoji
                    ),
                    primary(
                        id = EDIT_BUTTON_ID.format("skip", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SKIP),
                        emoji = RobertifyEmoji.END_EMOJI.emoji
                    ),
                ),
                ActionRow.of(
                    secondary(
                        id = EDIT_BUTTON_ID.format("favourite", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FAVOURITE),
                        emoji = RobertifyEmoji.STAR_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("loop", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_LOOP),
                        emoji = RobertifyEmoji.LOOP_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("shuffle", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SHUFFLE),
                        emoji = RobertifyEmoji.SHUFFLE_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("disconnect", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_DISCONNECT),
                        emoji = RobertifyEmoji.QUIT_EMOJI.emoji
                    ),
                    secondary(
                        id = EDIT_BUTTON_ID.format("filters", user.id),
                        label = localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FILTERS),
                        emoji = RobertifyEmoji.STAR_EMOJI.emoji
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
            event.replyEmbed {
                embed(GeneralMessages.NO_PERMS_BUTTON)
            }
                .setEphemeral(true)
                .queue()
            return
        }

        event.deferReply().queue()
        handleChannelButtonToggle(guild, buttonName, event)
    }

    fun handleChannelButtonToggle(
        guild: Guild,
        buttonName: String,
        event: ButtonInteractionEvent? = null,
        shardManager: ShardManager = Robertify.shardManager
    ): CompletableFuture<Message>? {
        val localeManager = LocaleManager[guild]
        val config = RequestChannelConfig(guild, shardManager)
        val subConfig = config.config
        val field: RequestChannelButton
        val button: String

        when (buttonName) {
            "previous" -> {
                field = RequestChannelButton.PREVIOUS
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PREVIOUS)
            }

            "rewind" -> {
                field = RequestChannelButton.REWIND
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_REWIND)
            }

            "pnp", "play_and_pause" -> {
                field = RequestChannelButton.PLAY_PAUSE
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_PLAY_AND_PAUSE)
            }

            "stop" -> {
                field = RequestChannelButton.STOP
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_STOP)
            }

            "skip" -> {
                field = RequestChannelButton.SKIP
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SKIP)
            }

            "favourite" -> {
                field = RequestChannelButton.FAVOURITE
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FAVOURITE)
            }

            "loop" -> {
                field = RequestChannelButton.LOOP
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_LOOP)
            }

            "shuffle" -> {
                field = RequestChannelButton.SHUFFLE
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_SHUFFLE)
            }

            "disconnect" -> {
                field = RequestChannelButton.DISCONNECT
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_DISCONNECT)
            }

            "filters" -> {
                field = RequestChannelButton.FILTERS
                button =
                    localeManager.getMessage(DedicatedChannelMessages.DEDICATED_CHANNEL_FILTERS)
            }

            else -> throw IllegalArgumentException("The button ID \"${event?.button?.id!!}\" doesn't map to a case to be handled!")
        }

        if (subConfig.getState(field)) {
            subConfig.setState(field, false)
            event?.hook?.sendEmbed(guild) {
                embed(
                    DedicatedChannelMessages.DEDICATED_CHANNEL_BUTTON_TOGGLE,
                    Pair("{button}", button),
                    Pair("{status}", localeManager.getMessage(GeneralMessages.OFF_STATUS))
                )
            }?.queueThenDelete(15, TimeUnit.SECONDS)
        } else {
            subConfig.setState(field, true)
            event?.hook?.sendEmbed(guild) {
                embed(
                    DedicatedChannelMessages.DEDICATED_CHANNEL_BUTTON_TOGGLE,
                    Pair("{button}", button),
                    Pair("{status}", localeManager.getMessage(GeneralMessages.ON_STATUS))
                )
            }?.queueThenDelete(15, TimeUnit.SECONDS)
        }

        return config.updateButtons()
    }

}