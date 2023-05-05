package main.audiohandlers.loaders

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.messages.send
import main.audiohandlers.GuildMusicManagerKt
import main.utils.GeneralUtilsKt
import main.utils.RobertifyEmbedUtilsKt
import main.utils.component.interactions.selectionmenu.StringSelectMenuOptionKt
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilderKt
import main.utils.json.themes.ThemesConfigKt
import main.utils.locale.LocaleManagerKt
import main.utils.locale.messages.AudioLoaderMessages
import main.utils.locale.messages.SearchMessages
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow

class SearchResultLoaderKt(
    override val musicManager: GuildMusicManagerKt,
    private val searcher: User,
    override val query: String,
    private val botMessage: InteractionHook
) : AudioLoader() {
    private val guild = musicManager.guild

    override fun trackLoaded(track: AudioTrack?) {
        throw UnsupportedOperationException("This operation is not supported in the search result loader")
    }

    override fun onPlaylistLoad(playlist: AudioPlaylist) {
        throw UnsupportedOperationException("This operation is not supported in the search result loader")
    }

    override fun onSearchResultLoad(results: AudioPlaylist) {
        val tracks = results.tracks
        val localeManager = LocaleManagerKt[guild]
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
            _name = "searchresult:${searcher.id}:${query.lowercase().replace(" ", "%SPACE%")}",
            placeholder = localeManager.getMessage(SearchMessages.SEARCH_MENU_PLACEHOLDER),
            range = Pair(1, 1),
            _options = options
        ).build()

        botMessage.send(
            embeds = listOf(
                RobertifyEmbedUtilsKt.embedMessage(guild, embedDesc.toString())
                    .setAuthor(
                        localeManager.getMessage(
                            SearchMessages.SEARCH_EMBED_AUTHOR,
                            Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
                        ),
                        null,
                        ThemesConfigKt(guild).theme.transparent
                    )
                    .setFooter(localeManager.getMessage(SearchMessages.SEARCH_EMBED_FOOTER))
                    .build()
            ),
            components = listOf(
                ActionRow.of(selectionMenu),
                ActionRow.of(
                    danger(
                        label = localeManager.getMessage(SearchMessages.SEARCH_END_INTERACTION),
                        id = "searchresult:end:${searcher.id}"
                    )
                )
            )
        ).queue()
    }

    override fun noMatches() {
        botMessage.editOriginalEmbeds(
            RobertifyEmbedUtilsKt.embedMessage(
                guild,
                AudioLoaderMessages.NO_TRACK_FOUND,
                Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
            ).build()
        )
            .queue()
    }

    override fun loadFailed(exception: FriendlyException?) {
        TODO("Not yet implemented")
    }
}