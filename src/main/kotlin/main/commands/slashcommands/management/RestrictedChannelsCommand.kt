package main.commands.slashcommands.management

import dev.minn.jda.ktx.util.SLF4J
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.BotConstants
import main.constants.Toggle
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.SlashCommand
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.json.restrictedchannels.RestrictedChannelsConfig
import main.utils.json.toggles.TogglesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.RestrictedChannelMessages
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class RestrictedChannelsCommand : AbstractSlashCommand(
    SlashCommand(
        name = "restrictedchannels",
        description = "Configure which channels Robertify can interact with.",
        subcommands = listOf(
            SubCommand(
                name = "add",
                description = "Add restricted text or voice channels",
                options = listOf(
                    CommandOption(
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
            SubCommand(
                name = "remove",
                description = "The channel to remove as a restricted channel.",
                options = listOf(
                    CommandOption(
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
            SubCommand(
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

    override fun handle(event: SlashCommandInteractionEvent) {
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
                config.addChannel(channel.idLong, RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL)
                e.replyEmbed {
                    embed(
                        RestrictedChannelMessages.RESTRICTED_CHANNEL_ADDED,
                        Pair("{channelId}", channel.id),
                        Pair("{channelType}", "text")
                    )
                }.queue()
            },
            mutateAudioChannel = { e, config, channel ->
                config.addChannel(channel.idLong, RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)
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
                config.removeChannel(channel.idLong, RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL)
                e.replyEmbed {
                    embed("You have successfully removed ${channel.asMention} as a restricted text channel!")
                }.queue()
            },
            mutateAudioChannel = { e, config, channel ->
                config.removeChannel(channel.idLong, RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)
                e.replyEmbed {
                    embed("You have successfully removed ${channel.asMention} as a restricted text channel!")
                }.queue()
            }
        )
    }

    private inline fun handleGenericMutation(
        event: SlashCommandInteractionEvent,
        mutateAudioChannel: (event: SlashCommandInteractionEvent, config: RestrictedChannelsConfig, channel: AudioChannel) -> Unit,
        mutateTextChannel: (event: SlashCommandInteractionEvent, config: RestrictedChannelsConfig, channel: TextChannel) -> Unit,
    ) {
        val channel = event.getRequiredOption("channel").asChannel
        val guild = event.guild!!
        val config = RestrictedChannelsConfig(guild)
        val togglesConfig = TogglesConfig(guild)

        try {
            if (channel.type.isAudio) {
                if (!togglesConfig.getToggle(Toggle.RESTRICTED_VOICE_CHANNELS)) {
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
                if (!togglesConfig.getToggle(Toggle.RESTRICTED_TEXT_CHANNELS)) {
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
                BotConstants.getUnexpectedErrorEmbed(guild)
            }.setEphemeral(true)
                .queue()
        }
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val config = RestrictedChannelsConfig(guild)
        val localeManager = LocaleManager[guild]

        val textChannels = config.restrictedChannelsToString(RestrictedChannelsConfig.ChannelType.TEXT_CHANNEL)
        val voiceChannels = config.restrictedChannelsToString(RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)

        val embed = RobertifyEmbedUtils.embedMessage(
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