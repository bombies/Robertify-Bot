package main.commands.slashcommands.management

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.BotConstantsKt
import main.constants.ToggleKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.json.restrictedchannels.RestrictedChannelsConfigKt
import main.utils.json.toggles.TogglesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RestrictedChannelMessages
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class RestrictedChannelsCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "restrictedchannels",
        description = "Configure which channels Robertify can interact with.",
        subcommands = listOf(
            SubCommandKt(
                name = "add",
                description = "Add restricted text or voice channels",
                options = listOf(
                    CommandOptionKt(
                        type = OptionType.CHANNEL,
                        name = "channel",
                        description = "The channel to add as a restricted channel.",
                        channelTypes = listOf(
                            ChannelType.TEXT,
                            ChannelType.VOICE,
                            ChannelType.STAGE
                        )
                    )
                )
            ),
            SubCommandKt(
                name = "remove",
                description = "The channel to remove as a restricted channel.",
                options = listOf(
                    CommandOptionKt(
                        type = OptionType.CHANNEL,
                        name = "channel",
                        description = "The channel to add as a restricted channel.",
                        channelTypes = listOf(
                            ChannelType.TEXT,
                            ChannelType.VOICE,
                            ChannelType.STAGE,
                        )
                    )
                )
            ),
            SubCommandKt(
                name = "list",
                description = "List all restricted channels."
            )
        ),
        adminOnly = true
    )
) {

    companion object {
        val logger by SLF4J
    }

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "add" -> handleAdd(event)
            "remove" -> handleRemove(event)
            "list" -> handleList(event)
        }
    }

    private fun handleAdd(event: SlashCommandInteractionEvent) {
        handleGenericMutation(
            event = event,
            mutateTextChannel = { e, config, channel ->
                config.addChannel(channel.idLong, RestrictedChannelsConfigKt.ChannelType.TEXT_CHANNEL)
                e.replyEmbed {
                    embed(
                        RestrictedChannelMessages.RESTRICTED_CHANNEL_ADDED,
                        Pair("{channelId}", channel.id),
                        Pair("{channelType}", "text")
                    )
                }.queue()
            },
            mutateAudioChannel = { e, config, channel ->
                config.addChannel(channel.idLong, RestrictedChannelsConfigKt.ChannelType.VOICE_CHANNEL)
                e.replyEmbed {
                    embed(
                        RestrictedChannelMessages.RESTRICTED_CHANNEL_ADDED,
                        Pair("{channelId}", channel.id),
                        Pair("{channelType}", "voice")
                    )
                }.queue()
            }
        )
    }

    private fun handleRemove(event: SlashCommandInteractionEvent) {
        handleGenericMutation(
            event = event,
            mutateTextChannel = { e, config, channel ->
                config.removeChannel(channel.idLong, RestrictedChannelsConfigKt.ChannelType.TEXT_CHANNEL)
                e.replyEmbed {
                    embed("You have successfully removed ${channel.asMention} as a restricted text channel!")
                }.queue()
            },
            mutateAudioChannel = { e, config, channel ->
                config.removeChannel(channel.idLong, RestrictedChannelsConfigKt.ChannelType.VOICE_CHANNEL)
                e.replyEmbed {
                    embed("You have successfully removed ${channel.asMention} as a restricted text channel!")
                }.queue()
            }
        )
    }

    private inline fun handleGenericMutation(
        event: SlashCommandInteractionEvent,
        mutateAudioChannel: (event: SlashCommandInteractionEvent, config: RestrictedChannelsConfigKt, channel: AudioChannel) -> Unit,
        mutateTextChannel: (event: SlashCommandInteractionEvent, config: RestrictedChannelsConfigKt, channel: TextChannel) -> Unit,
    ) {
        val channel = event.getRequiredOption("channel").asChannel
        val guild = event.guild!!
        val config = RestrictedChannelsConfigKt(guild)
        val togglesConfig = TogglesConfigKt(guild)

        try {
            if (channel.type.isAudio) {
                if (!togglesConfig.getToggle(ToggleKt.RESTRICTED_VOICE_CHANNELS)) {
                    event.replyEmbed {
                        embed(
                            """
                        This feature is toggled **OFF**
                        
                        *Looking to toggle this feature on? Do* `toggle restrictedvoice` 
                    """.trimIndent()
                        )
                    }.queue()
                    return
                }
                mutateAudioChannel(event, config, channel.asAudioChannel())
            } else {
                if (!togglesConfig.getToggle(ToggleKt.RESTRICTED_TEXT_CHANNELS)) {
                    event.replyEmbed {
                        embed(
                            """
                        This feature is toggled **OFF**
                        
                        *Looking to toggle this feature on? Do* `toggle restrictedtext` 
                    """.trimIndent()
                        )
                    }.queue()
                    return
                }
                mutateTextChannel(event, config, channel.asTextChannel())
            }
        } catch (e: IllegalStateException) {
            event.replyEmbed {
                embed(e.message!!)
            }.setEphemeral(true)
                .queue()
        } catch (e: NullPointerException) {
            event.replyEmbed {
                embed(e.message!!)
            }.setEphemeral(true)
                .queue()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            event.replyEmbed {
                BotConstantsKt.getUnexpectedErrorEmbed(guild)
            }.setEphemeral(true)
                .queue()
        }
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val config = RestrictedChannelsConfigKt(guild)
        val localeManager = LocaleManagerKt[guild]

        val textChannels = config.restrictedChannelsToString(RestrictedChannelsConfigKt.ChannelType.TEXT_CHANNEL)
        val voiceChannels = config.restrictedChannelsToString(RestrictedChannelsConfigKt.ChannelType.VOICE_CHANNEL)

        val embed = RobertifyEmbedUtilsKt.embedMessage(
            guild,
            RestrictedChannelMessages.LISTING_RESTRICTED_CHANNELS
        )
            .addField(
                localeManager.getMessage(RestrictedChannelMessages.RESTRICTED_CHANNELS_TC_EMBED_FIELD),
                textChannels.ifEmpty { localeManager.getMessage(RestrictedChannelMessages.NO_CHANNELS) },
                false
            )
            .addField(
                localeManager.getMessage(RestrictedChannelMessages.RESTRICTED_CHANNELS_VC_EMBED_FIELD),
                voiceChannels.ifEmpty { localeManager.getMessage(RestrictedChannelMessages.NO_CHANNELS) },
                false
            ).build()

        event.replyEmbed { embed }.queue()
    }

    override val help: String
        get() = "Restrict the bot to join voice/text channels that you set."
}