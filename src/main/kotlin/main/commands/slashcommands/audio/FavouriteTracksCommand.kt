package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManager
import main.audiohandlers.utils.author
import main.audiohandlers.utils.source
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManager.getRequiredOption
import main.constants.RobertifyTheme
import main.constants.TrackSource
import main.utils.RobertifyEmbedUtils
import main.utils.RobertifyEmbedUtils.Companion.replyEmbed
import main.utils.RobertifyEmbedUtils.Companion.sendEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.slashcommand.AbstractSlashCommand
import main.utils.component.interactions.slashcommand.models.Command
import main.utils.component.interactions.slashcommand.models.CommandOption
import main.utils.component.interactions.slashcommand.models.SubCommand
import main.utils.database.mongodb.cache.FavouriteTracksCache
import main.utils.json.themes.ThemesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.FavouriteTracksMessages
import main.utils.locale.messages.GeneralMessages
import main.utils.pagination.PaginationHandler
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class FavouriteTracksCommand : AbstractSlashCommand(
    Command(
        name = "favouritetracks",
        description = "Interact with your favourite tracks using this command.",
        subcommands = listOf(
            SubCommand(
                name = "view",
                description = "View all of your favourite tracks."
            ),
            SubCommand(
                name = "add",
                description = "Add the currently playing song as one of your favourites."
            ),
            SubCommand(
                name = "remove",
                description = "Remove a specific track as a favourite track.",
                listOf(
                    CommandOption(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The ID of the song to remove."
                    )
                )
            ),
            SubCommand(
                name = "Clear",
                description = "Clear all of your favourite tracks"
            )
        ),
        isPremium = true
    )
) {

    companion object {
        private val logger by SLF4J
    }

    override val help: String
        get() = "This command allows you to interact with tracks you may really like when" +
                " using the bot! Want to save some really good songs for later? No problem! " +
                "We'll store it for you.\n\n" +
                """
                **__Usages__**
                `/favouritetracks add` *(Add the current song as a favourite track)*
                `/favouritetracks remove <id>` *(Remove a specified song as a favourite track)*
                `/favouritetracks` *(View all your favourite tracks)*
                """

    override suspend fun handle(event: SlashCommandInteractionEvent) {
        when (event.subcommandName) {
            "view" -> handleList(event)
            "add" -> event.replyEmbed { handleAdd(event.guild!!, event.member!!) }.setEphemeral(true).queue()
            "remove" -> {
                val id = event.getRequiredOption("id").asInt
                event.replyEmbed { handleRemove(event.guild!!, event.user, id) }
                    .setEphemeral(true)
                    .queue()
            }

            "clear" -> event.replyEmbed { handleClear(event.guild!!, event.user) }.setEphemeral(true).queue()
        }
    }

    private fun handleList(event: SlashCommandInteractionEvent) {
        val config = FavouriteTracksCache.instance
        val member = event.member!!
        val guild = member.guild
        val tracks = config.getTracks(member.idLong)

        if (tracks.isEmpty()) {
            event.replyEmbed {
                embed(FavouriteTracksMessages.NO_FAV_TRACKS)
            }
                .setEphemeral(true)
                .queue()

            return
        }

        val list = mutableListOf<StringSelectMenuOption>()
        val localeManager = LocaleManager[guild]

        tracks.forEach { track ->
            list.add(
                StringSelectMenuOption(
                    label = localeManager.getMessage(
                        FavouriteTracksMessages.FT_SELECT_MENU_OPTION,
                        Pair("{title}", track.title.substring(0, track.title.length.coerceAtMost(75))),
                        Pair("{author}", track.author.substring(0, track.author.length.coerceAtMost(20)))
                    ),
                    value = "favouriteTrack:${track.id}:${track.source}"
                )
            )
        }

        val theme = ThemesConfig(guild).theme
        setDefaultEmbed(member, tracks, theme)
        PaginationHandler.paginateMenu(event, list)
    }

    private fun handleClear(guild: Guild, user: User): MessageEmbed {
        val config = FavouriteTracksCache.instance
        val trackList = config.getTracks(user.idLong)

        if (trackList.isEmpty())
            return RobertifyEmbedUtils.embedMessage(
                guild,
                FavouriteTracksMessages.NO_FAV_TRACKS
            ).build()

        return try {
            config.clearTracks(user.idLong)
            RobertifyEmbedUtils.embedMessage(
                guild,
                FavouriteTracksMessages.FAV_TRACKS_CLEARED
            ).build()
        } catch (e: NullPointerException) {
            RobertifyEmbedUtils.embedMessage(guild, FavouriteTracksMessages.NO_FAV_TRACKS)
                .build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    private fun handleRemove(guild: Guild, user: User, id: Int): MessageEmbed {
        if (id <= 0)
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.ID_GT_ZERO)
                .build()

        val config = FavouriteTracksCache.instance
        val trackList = config.getTracks(user.idLong)

        if (id > trackList.size)
            return RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.ID_OUT_OF_BOUNDS)
                .build()

        return try {
            config.removeTrack(user.idLong, id - 1)
            val trackRemoved = trackList.get(id - 1)
            RobertifyEmbedUtils.embedMessage(
                guild,
                FavouriteTracksMessages.FAV_TRACK_REMOVED,
                Pair("{title}", trackRemoved.title),
                Pair("{author}", trackRemoved.author)
            ).build()
        } catch (e: NullPointerException) {
            RobertifyEmbedUtils.embedMessage(guild, FavouriteTracksMessages.NO_FAV_TRACKS)
                .build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    fun handleAdd(guild: Guild, member: Member): MessageEmbed {
        val config = FavouriteTracksCache.instance
        val musicManager = RobertifyAudioManager[guild]
        val player = musicManager.player
        val playingTrack = player.playingTrack
        val memberVoiceState = member.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) return acChecks

        val id = playingTrack.identifier

        logger.debug("source: ${playingTrack.source}")
        val source = when (playingTrack.source) {
            "spotify" -> TrackSource.SPOTIFY
            "deezer" -> TrackSource.DEEZER
            "applemusic" -> TrackSource.APPLE_MUSIC
            "soundcloud" -> TrackSource.SOUNDCLOUD
            "resume" -> TrackSource.RESUMED
            else -> run {
                return RobertifyEmbedUtils.embedMessage(
                    guild,
                    FavouriteTracksMessages.FT_INVALID_SOURCE
                ).build()
            }
        }

        return try {
            config.addTrack(
                uid = member.idLong,
                trackID = id,
                title = playingTrack.title,
                author = playingTrack.author,
                source = source
            )

            RobertifyEmbedUtils.embedMessage(
                guild,
                FavouriteTracksMessages.FAV_TRACK_ADDED,
                Pair("{title}", playingTrack.title),
                Pair("{author}", playingTrack.author)
            ).build()
        } catch (e: IllegalArgumentException) {
            RobertifyEmbedUtils.embedMessage(guild, e.message ?: "Invalid arguments!").build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtils.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (event.selectMenu.id?.startsWith("menupage") == false) return
        if (event.user.id != event.componentId.split(":")[1]) return

        val selectionOption = event.selectedOptions.first().value
        if (!selectionOption.startsWith("favouriteTrack")) return

        val id = selectionOption.split(":")[1]
        val source = TrackSource.parse(selectionOption.split(":")[2])
        val audioManager = RobertifyAudioManager
        val guild = event.guild!!
        val memberVoiceState = event.member!!.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState, selfChannelNeeded = false)
        if (acChecks != null) {
            event.replyEmbed { acChecks }
                .setEphemeral(true)
                .queue()
            return
        }

        event.replyEmbed {
            embed(FavouriteTracksMessages.FT_ADDING_TO_QUEUE)
        }.setEphemeral(true)
            .queue()

        val messageChannel = event.channel.asGuildMessageChannel()
        messageChannel.sendEmbed(guild) {
            embed(FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2)
        }.queue { addingMsg ->
            val url = when (source) {
                TrackSource.DEEZER -> "https://www.deezer.com/us/track/$id"
                TrackSource.SPOTIFY -> "https://www.open.spotify.com/track/$id"
                TrackSource.APPLE_MUSIC -> "https://www.music.apple.com/us/song/$id"
                TrackSource.YOUTUBE,
                TrackSource.SOUNDCLOUD,
                TrackSource.RESUMED -> id
            }

            audioManager.loadAndPlay(
                trackUrl = url,
                memberVoiceState = memberVoiceState,
                botMessage = addingMsg
            )
        }
    }

    private fun setDefaultEmbed(
        member: Member,
        tracks: List<FavouriteTracksCache.Track>,
        theme: RobertifyTheme
    ) {
        val localeManager = LocaleManager.getLocaleManager(member.guild)
        PaginationHandler.embedStyle = {
            EmbedBuilder()
                .setColor(theme.color)
                .setTitle(
                    localeManager.getMessage(
                        FavouriteTracksMessages.FT_EMBED_TITLE,
                        Pair("{user}", member.effectiveName)
                    )
                )
                .setFooter(
                    localeManager.getMessage(
                        FavouriteTracksMessages.FT_EMBED_FOOTER,
                        Pair("{tracks}", tracks.size.toString())
                    )
                )
                .setDescription(localeManager.getMessage(FavouriteTracksMessages.FT_EMBED_DESCRIPTION))
                .setThumbnail(member.effectiveAvatarUrl)
        }
    }
}