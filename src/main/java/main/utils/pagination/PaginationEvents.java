package main.utils.pagination;

import main.constants.MessageButton;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class PaginationEvents extends ListenerAdapter {
    private static HashMap<Message, Integer> currentPage = new HashMap<>();

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        event.getHook().setEphemeral(true);

        if (!currentPage.containsKey(event.getMessage()))
            currentPage.put(event.getMessage(), 0);

        Message msg = event.getMessage();
        List<Page> pages = Pages.getPages(msg);

        if (event.getButton().getId().equals(MessageButton.FRONT + event.getUser().getId())) {
            currentPage.put(msg, 0);
            event.editMessageEmbeds(pages.get(0).getEmbed()).queue();
            event.deferEdit().queue();

        } else if (event.getButton().getId().equals(MessageButton.PREVIOUS + event.getUser().getId())) {
            if (currentPage.get(msg) == 0) {
                event.deferEdit().queue();
                return;
            }

            currentPage.put(msg, currentPage.get(msg)-1);
            msg.editMessageEmbeds(pages.get(currentPage.get(msg)).getEmbed()).queue();
            event.deferEdit().queue();

        } else if (event.getButton().getId().equals(MessageButton.NEXT + event.getUser().getId())) {
            if (currentPage.get(msg) == pages.size()-1) {
                event.deferEdit().queue();
                return;
            }

            currentPage.put(msg, currentPage.get(msg)+1);
            msg.editMessageEmbeds(pages.get(currentPage.get(msg)).getEmbed()).queue();
            event.deferEdit().queue();

        } else if (event.getButton().getId().equals(MessageButton.END + event.getUser().getId())) {
            currentPage.put(msg, pages.size()-1);
            event.editMessageEmbeds(pages.get(currentPage.get(msg)).getEmbed()).queue();
            event.deferEdit().queue();

        } else {
            EmbedBuilder eb = EmbedUtils.embedMessage("You do not have permission to interact with this button.");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        }
    }
}
