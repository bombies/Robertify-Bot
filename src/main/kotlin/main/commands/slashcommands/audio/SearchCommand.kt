package main.commands.slashcommands.audio

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import main.audiohandlers.RobertifyAudioManager
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.json.requestchannel.RequestChannelConfig
import main.utils.locale.messages.FavouriteTracksMessages
import main.utils.locale.messages.GeneralMessages
import main.utils.locale.messages.SearchMessages
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook

class SearchCommand : AbstractSlashCommand(
    Command(
        name = "search",
        description = "Search and browse for a specific track.",
        options = listOf(
            CommandOption(
                name = "query",
                description = "What would you like to search for?"
            )
        )
    )
) {

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild!!
        val requestChannelConfig = RequestChannelConfig(guild)
        if (requestChannelConfig.isRequestChannel(event.channel.asGuildMessageChannel())) {
            event.replyEmbed {
                embed(GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
            }.queue()
            return
        }

        val query = event.getRequiredOption("query").asString

        event.replyEmbed {
            embed(
                SearchMessages.LOOKING_FOR,
                Pair("{query}", query)
            )
        }.queue { addingMsg ->
            getSearchResult(
                guild,
                event.user,
                addingMsg,
                SpotifySourceManager.SEARCH_PREFIX + query
            )
        }
    }

    private fun getSearchResult(
        guild: Guild,
        requester: User,
        botMSg: InteractionHook,
        query: String
    ) {
        val musicManager = RobertifyAudioManager[guild]
        RobertifyAudioManager.loadSearchResults(musicManager, requester, botMSg, query)
    }

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (!event.componentId.startsWith("searchresult:")) return

        val split = event.componentId.split(":")
        val id = split[1]
        val guild = event.guild!!

        if (event.user.id != id) {
            event.replyEmbed {
                embed(GeneralMessages.NO_MENU_PERMS)
            }.setEphemeral(true)
                .queue()
            return
        }

        val selfVoiceState = guild.selfMember.voiceState!!
        val memberVoiceState = event.member!!.voiceState!!

        val audioChannelCheck = audioChannelChecks(
            memberVoiceState,
            selfVoiceState,
            selfChannelNeeded = false
        )

        if (audioChannelCheck != null) {
            event.replyEmbeds(audioChannelCheck)
                .setEphemeral(true)
                .queue()
            return
        }

        val trackQuery = event.selectedOptions.first().value
        event.replyEmbed {
            embed(FavouriteTracksMessages.FT_ADDING_TO_QUEUE)
        }.setEphemeral(true)
            .queue()

        RobertifyAudioManager.loadAndPlay(
            trackUrl = trackQuery,
            memberVoiceState = memberVoiceState,
            botMessage = event.message
        )
    }

    override suspend fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.button.id?.startsWith("searchresult:") == false) return

        val split = event.button.id!!.split(":")
        val id = split[1]
        val searcherId = split[2]
        val guild = event.guild!!

        when (id.lowercase()) {
            "end" -> {
                if (event.user.id != searcherId)
                    event.replyEmbed {
                        embed(GeneralMessages.NO_PERMS_END_INTERACTION)
                    }.setEphemeral(true)
                        .queue()
                else event.message.delete().queue()
            }
            else -> throw IllegalArgumentException("The gods have blessed us with an impossible exception. (ID=$id)")
        }
    }

    override val help: String
        get() = "Search for a specific song! You will be provided a list of maximum 10" +
                " results from our library for you to choose from. It's as easy as selecting" +
                " one of them from the selection menu and it'll be added to the queue!\n\n"
}