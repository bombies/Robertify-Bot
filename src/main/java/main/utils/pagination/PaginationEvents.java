package main.utils.pagination;

import main.commands.slashcommands.commands.audio.FavouriteTracksCommand;
import main.constants.MessageButton;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.cache.FavouriteTracksCache;
import main.utils.json.themes.ThemesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class PaginationEvents extends ListenerAdapter {
    private static final HashMap<Long, Integer> currentPage = new HashMap<>();

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getButton().getId().startsWith(MessageButton.PAGE_ID.toString()))
            return;

        event.getHook().setEphemeral(true);

        if (!currentPage.containsKey(event.getMessage().getIdLong()))
            currentPage.put(event.getMessage().getIdLong(), 0);

        long msg = event.getMessage().getIdLong();
        List<MessagePage> messagePages = Pages.getMessagePages(msg);

        if (messagePages == null) {
            event.deferEdit().queue();
            return;
        }

        if (event.getButton().getId().equals(MessageButton.FRONT + event.getUser().getId())) {
            currentPage.put(msg, 0);
            event.editMessageEmbeds(messagePages.get(0).getEmbed())
                    .setActionRows(((currentPage.get(msg) == 0) ?
                            Paginator.getButtons(event.getUser(), false, false, true, true) :
                            Paginator.getButtons(event.getUser())))
                    .queue();
        } else if (event.getButton().getId().equals(MessageButton.PREVIOUS + event.getUser().getId())) {
                currentPage.put(msg, currentPage.get(msg) - 1);
                event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed())
                        .setActionRows(((currentPage.get(msg) == 0) ?
                                Paginator.getButtons(event.getUser(), false, false, true, true) :
                                Paginator.getButtons(event.getUser())))
                        .queue();
        } else if (event.getButton().getId().equals(MessageButton.NEXT + event.getUser().getId())) {
                currentPage.put(msg, currentPage.get(msg) + 1);
                event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed())
                        .setActionRows(((currentPage.get(msg) == messagePages.size()-1) ?
                                Paginator.getButtons(event.getUser(), true, true, false, false) :
                                Paginator.getButtons(event.getUser())))
                        .queue();
        } else if (event.getButton().getId().equals(MessageButton.END + event.getUser().getId())) {
            currentPage.put(msg, messagePages.size()-1);
            event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed())
                    .setActionRows(((currentPage.get(msg) == messagePages.size()-1) ?
                            Paginator.getButtons(event.getUser(), true, true, false, false) :
                            Paginator.getButtons(event.getUser()))).queue();
        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "You do not have permission to interact with this button.");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().startsWith("menupage")) return;

        if (!event.getUser().getId().equals(event.getComponentId().split(":")[1])) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You do not have permission to interact with this selection menu").build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!currentPage.containsKey(event.getMessage().getIdLong()))
            currentPage.put(event.getMessage().getIdLong(), 0);

        final var msg = event.getMessage();

        int currentPage = PaginationEvents.currentPage.get(msg.getIdLong());
        switch (event.getSelectedOptions().get(0).getValue().split(":")[1]) {
            case "nextPage" -> PaginationEvents.currentPage.put(msg.getIdLong(), currentPage + 1);
            case "previousPage" -> PaginationEvents.currentPage.put(msg.getIdLong(), currentPage - 1);
            default -> {
                return;
            }
        }

        currentPage = PaginationEvents.currentPage.get(msg.getIdLong());

        MenuPage menuPage = Pages.getMenuPages(msg.getIdLong())
                .get(currentPage);

        int finalCurrentPage = currentPage;
        event.editSelectionMenu(
                Pages.getSelectionMenu(
                        event.getUser(),
                        menuPage.getOptions()
                )
        ).queue(s -> {
            final var tracks = FavouriteTracksCache.getInstance().getTracks(event.getMember().getIdLong());
            final var theme = new ThemesConfig().getTheme(event.getGuild().getIdLong());
            FavouriteTracksCommand.setDefaultEmbed(event.getMember(), tracks, theme);
            msg.editMessageEmbeds(Pages.getPaginatedEmbed(msg.getGuild(), menuPage.toStringList(), 25, finalCurrentPage, true))
                    .queue();
        });
    }
}
