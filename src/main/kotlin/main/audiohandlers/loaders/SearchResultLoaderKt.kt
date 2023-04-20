package main.audiohandlers.loaders

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.messages.send
import dev.schlaubi.lavakord.Exception
import dev.schlaubi.lavakord.audio.player.Player
import dev.schlaubi.lavakord.rest.models.PartialTrack
import dev.schlaubi.lavakord.rest.models.TrackResponse
import main.audiohandlers.GuildMusicManagerKt
import main.main.RobertifyKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilderKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.RobertifyLocaleMessageKt
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class SearchResultLoaderKt(
    musicManager: GuildMusicManagerKt,
    private val searcher: User,
    override val query: String,
    private val botMessage: InteractionHook
) : AudioLoader(musicManager.link) {
    private val guild = musicManager.guild

    override suspend fun trackLoaded(player: Player, track: PartialTrack) {
        throw UnsupportedOperationException("This operation is not supported in the search result loader")
    }

    override suspend fun playlistLoaded(
        player: Player,
        tracks: List<PartialTrack>,
        playlistInfo: TrackResponse.NullablePlaylistInfo
    ) {
        throw UnsupportedOperationException("This operation is not supported in the search result loader")
    }

    override suspend fun searchLoaded(player: Player, tracks: List<PartialTrack>) {
        val localeManager = LocaleManagerKt.getLocaleManager(guild)
        val embedDesc = StringBuilder()

        val options = tracks.subList(0, tracks.size.coerceAtMost(10)).mapIndexed { i, track ->
            val info = track.info
            val label = "${info.title} by ${info.author}"
            val safeLabel = label.substring(0, label.length.coerceAtMost(100))

            embedDesc.append("**${i + 1}.** - ${info.title} by ${info.author} [${GeneralUtilsKt.formatTime(info.length)}]\n")

            StringSelectMenuOptionKt(
                label = safeLabel,
                value = "https://open.spotify.com/track/${info.identifier}",
                emoji = Emoji.fromUnicode(GeneralUtilsKt.parseNumEmoji(i + 1))
            )
        }

        val selectionMenu = StringSelectionMenuBuilderKt(
            _name = "",
            placeholder = localeManager.getMessage(RobertifyLocaleMessageKt.SearchMessages.SEARCH_MENU_PLACEHOLDER),
            range = Pair(1, 1),
            _options = options
        ).build()

        botMessage.send(
            embeds = listOf(
                RobertifyEmbedUtilsKt.embedMessage(guild, embedDesc.toString())
                    .setAuthor(
                        localeManager.getMessage(
                            RobertifyLocaleMessageKt.SearchMessages.SEARCH_EMBED_AUTHOR,
                            Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
                        ),
                        null,
                        ThemesConfigKt(guild).theme.transparent
                    )
                    .setFooter(localeManager.getMessage(RobertifyLocaleMessageKt.SearchMessages.SEARCH_EMBED_FOOTER))
                    .build()
            ),
            components = listOf(
                ActionRow.of(selectionMenu),
                ActionRow.of(guild.jda.button(
                    style = ButtonStyle.DANGER,
                    label = localeManager.getMessage(RobertifyLocaleMessageKt.SearchMessages.SEARCH_END_INTERACTION),
                    user = searcher,
                    listener = { /* TODO: End search interaction button listener */ }
                ))
            )
        ).queue()
    }

    override suspend fun noMatches() {
        botMessage.editOriginalEmbeds(
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                RobertifyLocaleMessageKt.AudioLoaderMessages.NO_TRACK_FOUND,
                Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
            ).build()
        )
            .queue()
    }

    override suspend fun loadFailed(exception: Exception?) {
        // TODO: Search result load fail handling
    }
}