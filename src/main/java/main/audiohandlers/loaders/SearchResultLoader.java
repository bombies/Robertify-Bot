package main.audiohandlers.loaders;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.selectionmenu.SelectionMenuBuilder;
import main.utils.json.themes.ThemesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.internal.utils.tuple.Pair;

public class SearchResultLoader implements AudioLoadResultHandler {
    private final Guild guild;
    private final User searcher;
    private final String query;
    private final Message botMsg;
    private final InteractionHook interactionBotMsg;

    public SearchResultLoader(Guild guild, User searcher, String query, Message botMsg) {
        this.guild = guild;
        this.searcher = searcher;
        this.query = query;
        this.botMsg = botMsg;
        this.interactionBotMsg = null;
    }

    public SearchResultLoader(Guild guild, User searcher, String query, InteractionHook botMsg) {
        this.guild = guild;
        this.searcher = searcher;
        this.query = query;
        this.interactionBotMsg = botMsg;
        this.botMsg = null;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        throw new UnsupportedOperationException("This operation is not supported in the search result loader");
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (!playlist.isSearchResult())
            throw new UnsupportedOperationException("This operation is not supported in the search result loader");

        final var tracks = playlist.getTracks();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        SelectionMenuBuilder selectionMenuBuilder = new SelectionMenuBuilder()
                .setName("searchresult:" + searcher.getId() + ":" + query.toLowerCase()
                        .replaceAll(" ", "%SPACE%"))
                .setPlaceHolder(localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.SEARCH_MENU_PLACEHOLDER))
                .setRange(1, 1);

        final StringBuilder embedDescription = new StringBuilder();

        for (int i = 0; i < Math.min(10, tracks.size()); i++) {
            AudioTrackInfo info = tracks.get(i).getInfo();
            selectionMenuBuilder.addOption(info.title, info.identifier, null);
            embedDescription.append("**").append(i+1).append(".** - ").append(info.title).append(" by ")
                    .append(info.author).append(" [").append(GeneralUtils.formatTime(info.length))
                    .append("]").append("\n");
        }

        SelectionMenu selectionMenu = selectionMenuBuilder.build();

        if (botMsg != null)
            botMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, embedDescription.toString())
                            .setAuthor(localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.SEARCH_EMBED_AUTHOR, Pair.of("{query}", query.replaceFirst("ytsearch:", ""))), null, new ThemesConfig(guild).getTheme().getTransparent())
                            .setFooter(localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.SEARCH_EMBED_FOOTER))
                            .build())
                    .setActionRows(
                            ActionRow.of(selectionMenu),
                            ActionRow.of(Button.of(ButtonStyle.DANGER, "searchresult:end:" + searcher.getId(), localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.SEARCH_END_INTERACTION)))
                    )
                    .queue();
        else interactionBotMsg.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, embedDescription.toString())
                        .setAuthor(localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.SEARCH_EMBED_AUTHOR, Pair.of("{query}", query.replaceFirst("ytsearch:", ""))), null, new ThemesConfig(guild).getTheme().getTransparent())
                        .setFooter(localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.SEARCH_EMBED_FOOTER))
                        .build())
                .addActionRows(
                        ActionRow.of(selectionMenu),
                        ActionRow.of(Button.of(ButtonStyle.DANGER, "searchresult:end:" + searcher.getId(), localeManager.getMessage(RobertifyLocaleMessage.SearchMessages.SEARCH_END_INTERACTION)))
                )
                .setEphemeral(false)
                .queue();
    }

    @Override
    public void noMatches() {
        botMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.AudioLoaderMessages.NO_TRACK_FOUND, Pair.of("{query}", query.replaceFirst("ytsearch:", ""))).build())
                .queue();
    }

    @Override
    public void loadFailed(FriendlyException exception) {

    }
}
