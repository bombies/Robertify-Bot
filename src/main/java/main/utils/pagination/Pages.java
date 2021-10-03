package main.utils.pagination;

import main.constants.MessageButton;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Pages {
    private static HashMap<Message, List<Page>> messages = new HashMap<>();

    public static void paginate(TextChannel channel, User user, List<Page> pages) {
        Paginator paginator = new Paginator("◂◂", "◂", "▸", "▸▸");

        channel.sendMessageEmbeds(pages.get(0).getEmbed()).queue(msg -> {
            if (pages.size() > 1) {
                msg.editMessageComponents(
                        ActionRow.of(
                                Button.of(ButtonStyle.SECONDARY, MessageButton.FRONT + user.getId(), paginator.getFront()),
                                Button.of(ButtonStyle.SECONDARY, MessageButton.PREVIOUS + user.getId(), paginator.getPrevious()),
                                Button.of(ButtonStyle.SECONDARY, MessageButton.NEXT + user.getId(), paginator.getNext()),
                                Button.of(ButtonStyle.SECONDARY, MessageButton.END + user.getId(), paginator.getEnd())
                        )
                ).queue();

                messages.put(msg, pages);
            }
        });
    }

    public static void paginate(TextChannel channel, User user, List<String> content, int maxPerPage) {
        List<Page> pages = new ArrayList<>();

        if (content.size() <= maxPerPage) {
            EmbedBuilder eb = EmbedUtils.embedMessage("\t");
            for (String str : content)
                eb.appendDescription(str + "\n");
            pages.add(new Page(eb.build()));
        } else {
            int pagesRequired = (int)Math.ceil((double)content.size() / maxPerPage);

            int lastIndex = 0;
            for (int i = 0; i < pagesRequired; i++) {
                EmbedBuilder eb = EmbedUtils.embedMessage("\t");
                for (int j = 0; j < maxPerPage; j++) {
                    if (lastIndex == content.size()) break;

                    eb.appendDescription(content.get(lastIndex) + "\n");
                    lastIndex++;
                }
                pages.add(new Page(eb.build()));
            }
        }

        paginate(channel, user, pages);
    }

    public static List<Page> getPages(Message msg) {
        return messages.get(msg);
    }
}
