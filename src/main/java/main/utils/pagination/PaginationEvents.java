package main.utils.pagination;

import main.constants.MessageButton;
import main.utils.RobertifyEmbedUtils;
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

        if (event.getButton().getId().equals(MessageButton.FRONT + event.getUser().getId())) {
            currentPage.put(msg, 0);
            event.editMessageEmbeds(messagePages.get(0).getEmbed()).queue();

        } else if (event.getButton().getId().equals(MessageButton.PREVIOUS + event.getUser().getId())) {
            if (currentPage.get(msg) == 0) {
                event.deferEdit().queue();
                return;
            }

            currentPage.put(msg, currentPage.get(msg)-1);
            event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed()).queue();

        } else if (event.getButton().getId().equals(MessageButton.NEXT + event.getUser().getId())) {
            if (currentPage.get(msg) == messagePages.size()-1) {
                event.deferEdit().queue();
                return;
            }

            currentPage.put(msg, currentPage.get(msg)+1);
            event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed()).queue();

        } else if (event.getButton().getId().equals(MessageButton.END + event.getUser().getId())) {
            currentPage.put(msg, messagePages.size()-1);
            event.editMessageEmbeds(messagePages.get(currentPage.get(msg)).getEmbed()).queue();

        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "You do not have permission to interact with this button.");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        }
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (!event.getComponentId().startsWith("menupage")) return;

        if (!currentPage.containsKey(event.getMessage().getIdLong()))
            currentPage.put(event.getMessage().getIdLong(), 0);

        final var msg = event.getMessage();

        switch (event.getSelectedOptions().get(0).getValue().split(":")[1]) {
            case "nextPage" -> {
                currentPage.put(msg.getIdLong(), currentPage.get(msg.getIdLong()) + 1);
                event.editSelectionMenu(
                        Pages.getSelectionMenu(
                                event.getUser(),
                                Pages.getMenuPages(msg.getIdLong())
                                        .get(currentPage.get(msg.getIdLong()))
                                        .getOptions()
                        )
                ).queue();
            }
            case "previousPage" -> {
                currentPage.put(msg.getIdLong(), currentPage.get(msg.getIdLong()) - 1);
                event.editSelectionMenu(
                        Pages.getSelectionMenu(
                                event.getUser(),
                                Pages.getMenuPages(msg.getIdLong())
                                        .get(currentPage.get(msg.getIdLong()))
                                        .getOptions()
                        )
                ).queue();
            }
        }
    }
}
