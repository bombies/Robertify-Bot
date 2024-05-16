package main.audiohandlers.loaders

import com.github.topi314.lavasrc.spotify.SpotifySourceManager
import dev.arbjerg.lavalink.client.player.Track
import dev.arbjerg.lavalink.client.player.TrackException
import dev.arbjerg.lavalink.protocol.v4.PlaylistInfo
import dev.minn.jda.ktx.interactions.components.danger
import dev.minn.jda.ktx.messages.send
import main.audiohandlers.GuildMusicManager
import main.utils.GeneralUtils
import main.utils.RobertifyEmbedUtils
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder
import main.utils.json.themes.ThemesConfig
import main.utils.locale.LocaleManager
import main.utils.locale.messages.AudioLoaderMessages
import main.utils.locale.messages.SearchMessages
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.ActionRow

class SearchResultLoader(
    override val musicManager: GuildMusicManager,
    private val searcher: User,
    override val query: String,
    private val botMessage: InteractionHook
) : AudioLoader(musicManager.guild) {
    private val guild = musicManager.guild

    override fun onPlaylistLoad(playlist: List<Track>, playlistInfo: PlaylistInfo) {
        throw UnsupportedOperationException("This operation is not supported in the search result loader")
    }

    override fun onSearchResultLoad(results: List<Track>) {
        val localeManager = LocaleManager[guild]
        val embedDesc = StringBuilder()

        val options = results.subList(0, results.size.coerceAtMost(10)).mapIndexed { i, track ->
            val info = track.info
            val label = "${info.title} by ${info.author}"
            val safeLabel = label.substring(0, label.length.coerceAtMost(100))

            embedDesc.append("**${i + 1}.** - ${info.title} by ${info.author} [${GeneralUtils.formatTime(info.length)}]\n")

            StringSelectMenuOption(
                label = safeLabel,
                value = "https://open.spotify.com/track/${info.identifier}",
                emoji = Emoji.fromUnicode(GeneralUtils.parseNumEmoji(i + 1))
            )
        }

        val selectionMenu = StringSelectionMenuBuilder(
            _name = "searchresult:${searcher.id}:${query.lowercase().replace(" ", "%SPACE%")}",
            placeholder = localeManager.getMessage(SearchMessages.SEARCH_MENU_PLACEHOLDER),
            range = Pair(1, 1),
            _options = options
        ).build()

        botMessage.send(
            embeds = listOf(
                RobertifyEmbedUtils.embedMessage(guild, embedDesc.toString())
                    .setAuthor(
                        localeManager.getMessage(
                            SearchMessages.SEARCH_EMBED_AUTHOR,
                            Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
                        ),
                        null,
                        ThemesConfig(guild).getTheme().transparent
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

    override fun onTrackLoad(result: Track) {
        throw UnsupportedOperationException("This operation is not supported in the search result loader")
    }

    override fun onNoMatches() {
        botMessage.editOriginalEmbeds(
            RobertifyEmbedUtils.embedMessage(
                guild,
                AudioLoaderMessages.NO_TRACK_FOUND,
                Pair("{query}", query.replaceFirst(SpotifySourceManager.SEARCH_PREFIX, ""))
            ).build()
        )
            .queue()
    }

    override fun onException(exception: TrackException) {
        TODO("Not yet implemented")
    }
}