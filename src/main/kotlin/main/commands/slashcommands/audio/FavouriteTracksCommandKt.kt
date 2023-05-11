package main.commands.slashcommands.audio

import dev.minn.jda.ktx.util.SLF4J
import main.audiohandlers.RobertifyAudioManagerKt
import main.audiohandlers.utils.author
import main.audiohandlers.utils.source
import main.audiohandlers.utils.title
import main.commands.slashcommands.SlashCommandManagerKt.getRequiredOption
import main.constants.RobertifyThemeKt
import main.constants.TrackSourceKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.RobertifyEmbedUtilsKt.Companion.replyEmbed
import main.utils.RobertifyEmbedUtilsKt.Companion.sendEmbed
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.slashcommand.AbstractSlashCommandKt
import main.utils.component.interactions.slashcommand.models.CommandKt
import main.utils.component.interactions.slashcommand.models.CommandOptionKt
import main.utils.component.interactions.slashcommand.models.SubCommandKt
import main.utils.database.mongodb.cache.FavouriteTracksCacheKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.FavouriteTracksMessages
import main.utils.locale.messages.GeneralMessages
import main.utils.pagination.PaginationHandlerKt
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType

class FavouriteTracksCommandKt : AbstractSlashCommandKt(
    CommandKt(
        name = "favouritetracks",
        description = "Interact with your favourite tracks using this command.",
        subcommands = listOf(
            SubCommandKt(
                name = "view",
                description = "View all of your favourite tracks."
            ),
            SubCommandKt(
                name = "add",
                description = "Add the currently playing song as one of your favourites."
            ),
            SubCommandKt(
                name = "remove",
                description = "Remove a specific track as a favourite track.",
                listOf(
                    CommandOptionKt(
                        type = OptionType.INTEGER,
                        name = "id",
                        description = "The ID of the song to remove."
                    )
                )
            ),
            SubCommandKt(
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
        val config = FavouriteTracksCacheKt.instance
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

        val list = mutableListOf<StringSelectMenuOptionKt>()
        val localeManager = LocaleManagerKt[guild]

        tracks.forEach { track ->
            list.add(
                StringSelectMenuOptionKt(
                    label = localeManager.getMessage(
                        FavouriteTracksMessages.FT_SELECT_MENU_OPTION,
                        Pair("{title}", track.title.substring(0, track.title.length.coerceAtMost(75))),
                        Pair("{author}", track.author.substring(0, track.author.length.coerceAtMost(20)))
                    ),
                    value = "favouriteTrack:${track.id}:${track.source}"
                )
            )
        }

        val theme = ThemesConfigKt(guild).theme
        setDefaultEmbed(member, tracks, theme)
        PaginationHandlerKt.paginateMenu(event, list)
    }

    private fun handleClear(guild: Guild, user: User): MessageEmbed {
        val config = FavouriteTracksCacheKt.instance
        val trackList = config.getTracks(user.idLong)

        if (trackList.isEmpty())
            return RobertifyEmbedUtilsKt.embedMessage(
                guild,
                FavouriteTracksMessages.NO_FAV_TRACKS
            ).build()

        return try {
            config.clearTracks(user.idLong)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                FavouriteTracksMessages.FAV_TRACKS_CLEARED
            ).build()
        } catch (e: NullPointerException) {
            RobertifyEmbedUtilsKt.embedMessage(guild, FavouriteTracksMessages.NO_FAV_TRACKS)
                .build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    private fun handleRemove(guild: Guild, user: User, id: Int): MessageEmbed {
        if (id <= 0)
            return RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.ID_GT_ZERO)
                .build()

        val config = FavouriteTracksCacheKt.instance
        val trackList = config.getTracks(user.idLong)

        if (id > trackList.size)
            return RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.ID_OUT_OF_BOUNDS)
                .build()

        return try {
            config.removeTrack(user.idLong, id - 1)
            val trackRemoved = trackList.get(id - 1)
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                FavouriteTracksMessages.FAV_TRACK_REMOVED,
                Pair("{title}", trackRemoved.title),
                Pair("{author}", trackRemoved.author)
            ).build()
        } catch (e: NullPointerException) {
            RobertifyEmbedUtilsKt.embedMessage(guild, FavouriteTracksMessages.NO_FAV_TRACKS)
                .build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    fun handleAdd(guild: Guild, member: Member): MessageEmbed {
        val config = FavouriteTracksCacheKt.instance
        val musicManager = RobertifyAudioManagerKt[guild]
        val player = musicManager.player
        val playingTrack = player.playingTrack
        val memberVoiceState = member.voiceState!!
        val selfVoiceState = guild.selfMember.voiceState!!

        val acChecks = audioChannelChecks(memberVoiceState, selfVoiceState)
        if (acChecks != null) return acChecks

        val id = playingTrack.identifier

        logger.debug("source: ${playingTrack.source}")
        val source = when (playingTrack.source) {
            "spotify" -> TrackSourceKt.SPOTIFY
            "deezer" -> TrackSourceKt.DEEZER
            "applemusic" -> TrackSourceKt.APPLE_MUSIC
            "soundcloud" -> TrackSourceKt.SOUNDCLOUD
            "resume" -> TrackSourceKt.RESUMED
            else -> run {
                return RobertifyEmbedUtilsKt.embedMessage(
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

            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                FavouriteTracksMessages.FAV_TRACK_ADDED,
                Pair("{title}", playingTrack.title),
                Pair("{author}", playingTrack.author)
            ).build()
        } catch (e: IllegalArgumentException) {
            RobertifyEmbedUtilsKt.embedMessage(guild, e.message ?: "Invalid arguments!").build()
        } catch (e: Exception) {
            logger.error("Unexpected error", e)
            RobertifyEmbedUtilsKt.embedMessage(guild, GeneralMessages.UNEXPECTED_ERROR).build()
        }
    }

    override suspend fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (event.selectMenu.id?.startsWith("menupage") == false) return
        if (event.user.id != event.componentId.split(":")[1]) return

        val selectionOption = event.selectedOptions.first().value
        if (!selectionOption.startsWith("favouriteTrack")) return

        val id = selectionOption.split(":")[1]
        val source = TrackSourceKt.parse(selectionOption.split(":")[2])
        val audioManager = RobertifyAudioManagerKt
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
                TrackSourceKt.DEEZER -> "https://www.deezer.com/us/track/$id"
                TrackSourceKt.SPOTIFY -> "https://www.open.spotify.com/track/$id"
                TrackSourceKt.APPLE_MUSIC -> "https://www.music.apple.com/us/song/$id"
                TrackSourceKt.YOUTUBE,
                TrackSourceKt.SOUNDCLOUD,
                TrackSourceKt.RESUMED -> id
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
        tracks: List<FavouriteTracksCacheKt.Track>,
        theme: RobertifyThemeKt
    ) {
        val localeManager = LocaleManagerKt.getLocaleManager(member.guild)
        PaginationHandlerKt.embedStyle = {
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