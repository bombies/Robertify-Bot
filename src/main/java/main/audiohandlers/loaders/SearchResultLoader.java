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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

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
        SelectionMenuBuilder selectionMenuBuilder = new SelectionMenuBuilder()
                .setName("searchresult:" + searcher.getId() + ":" + query.toLowerCase()
                        .replaceAll(" ", "%SPACE%"))
                .setPlaceHolder("Choose a result!")
                .setRange(1, 1);

        final StringBuilder embedDescription = new StringBuilder();

        for (int i = 0; i < Math.min(10, tracks.size()); i++) {
            AudioTrackInfo info = tracks.get(i).getInfo();
            selectionMenuBuilder.addOption(info.title, info.identifier, null);
            embedDescription.append("**").append(i+1).append(".** - ").append(info.title).append(" by ")
                    .append(info.author).append(" [").append(GeneralUtils.formatTime(info.length))
                    .append("]").append("\n");
        }

        SelectMenu selectionMenu = selectionMenuBuilder.build();

        if (botMsg != null)
            botMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, embedDescription.toString())
                            .setAuthor("Search results for: " + query.replaceFirst("ytsearch:", ""), null, new ThemesConfig().getTheme(guild.getIdLong()).getTransparent())
                            .setFooter("Select a result from the selection menu below")
                            .build())
                    .setActionRows(
                            ActionRow.of(selectionMenu),
                            ActionRow.of(Button.of(ButtonStyle.DANGER, "searchresult:end:" + searcher.getId(), "End Interaction"))
                    )
                    .queue();
        else interactionBotMsg.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, embedDescription.toString())
                        .setAuthor("Search results for: " + query.replaceFirst("ytsearch:", ""), null, new ThemesConfig().getTheme(guild.getIdLong()).getTransparent())
                        .setFooter("Select a result from the selection menu below")
                        .build())
                .addActionRows(
                        ActionRow.of(selectionMenu),
                        ActionRow.of(Button.of(ButtonStyle.DANGER, "searchresult:end:" + searcher.getId(), "End Interaction"))
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
