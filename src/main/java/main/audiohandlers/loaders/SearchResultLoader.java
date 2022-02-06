package main.audiohandlers.loaders;

import lavalink.client.io.FriendlyException;
import lavalink.client.io.LoadResultHandler;
import lavalink.client.player.track.AudioPlaylist;
import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackInfo;
import lombok.SneakyThrows;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.builders.selectionmenu.SelectionMenuBuilder;
import main.utils.json.themes.ThemesConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

import java.util.List;

public class SearchResultLoader implements LoadResultHandler {
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
        throw new UnsupportedOperationException("This operation is not supported in the search result loader");
    }

    @Override @SneakyThrows
    public void searchResultLoaded(List<AudioTrack> tracks) {
        SelectionMenuBuilder selectionMenuBuilder = new SelectionMenuBuilder()
                .setName("searchresult:" + searcher.getId() + ":" + query.toLowerCase()
                        .replaceAll(" ", "%SPACE%"))
                .setPlaceHolder("Choose a result!")
                .setRange(1, 1);

        final StringBuilder embedDescription = new StringBuilder();

        for (int i = 0; i < Math.min(10, tracks.size()); i++) {
            AudioTrackInfo info = tracks.get(i).getInfo();
            selectionMenuBuilder.addOption(info.getTitle(), info.getIdentifier(), null);
            embedDescription.append("**").append(i+1).append(".** - ").append(info.getTitle()).append(" by ")
                    .append(info.getAuthor()).append(" [").append(GeneralUtils.formatTime(info.getLength()))
                    .append("]").append("\n");
        }

        SelectionMenu selectionMenu = selectionMenuBuilder.build();

        if (botMsg != null)
            botMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, embedDescription.toString())
                            .setAuthor("Search results for: " + query.replaceFirst("ytsearch:", ""), null, new ThemesConfig().getTheme(guild.getIdLong()).getTransparent())
                            .setFooter("Select a result from the selection menu below")
                            .build())
                    .setActionRows(
                            ActionRow.of(selectionMenu),
                            ActionRow.of(Button.of(ButtonStyle.DANGER, "searchresult:end", "End Interaction"))
                    )
                    .queue();
        else interactionBotMsg.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, embedDescription.toString())
                        .setAuthor("Search results for: " + query.replaceFirst("ytsearch:", ""), null, new ThemesConfig().getTheme(guild.getIdLong()).getTransparent())
                        .setFooter("Select a result from the selection menu below")
                        .build())
                .addActionRows(
                        ActionRow.of(selectionMenu),
                        ActionRow.of(Button.of(ButtonStyle.DANGER, "searchresult:end", "End Interaction"))
                )
                .setEphemeral(false)
                .queue();
    }

    @Override
    public void noMatches() {
        botMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "There was nothing found for: `" + query.replaceFirst("ytsearch:", "") + "`").build())
                .queue();
    }

    @Override
    public void loadFailed(FriendlyException exception) {

    }
}
