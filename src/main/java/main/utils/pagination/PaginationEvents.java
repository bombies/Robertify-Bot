package main.utils.pagination;

import main.constants.MessageButton;
import main.utils.RobertifyEmbedUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
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
        List<Page> pages = Pages.getPages(msg);

        if (event.getButton().getId().equals(MessageButton.FRONT + event.getUser().getId())) {
            currentPage.put(msg, 0);
            event.editMessageEmbeds(pages.get(0).getEmbed()).queue();

        } else if (event.getButton().getId().equals(MessageButton.PREVIOUS + event.getUser().getId())) {
            if (currentPage.get(msg) == 0) {
                event.deferEdit().queue();
                return;
            }

            currentPage.put(msg, currentPage.get(msg)-1);
            event.editMessageEmbeds(pages.get(currentPage.get(msg)).getEmbed()).queue();

        } else if (event.getButton().getId().equals(MessageButton.NEXT + event.getUser().getId())) {
            if (currentPage.get(msg) == pages.size()-1) {
                event.deferEdit().queue();
                return;
            }

            currentPage.put(msg, currentPage.get(msg)+1);
            event.editMessageEmbeds(pages.get(currentPage.get(msg)).getEmbed()).queue();

        } else if (event.getButton().getId().equals(MessageButton.END + event.getUser().getId())) {
            currentPage.put(msg, pages.size()-1);
            event.editMessageEmbeds(pages.get(currentPage.get(msg)).getEmbed()).queue();

        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(event.getGuild(), "You do not have permission to interact with this button.");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        }
    }
}
