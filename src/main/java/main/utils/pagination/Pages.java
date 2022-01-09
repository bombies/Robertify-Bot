package main.utils.pagination;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.constants.MessageButton;
import main.constants.RobertifyEmoji;
import main.utils.RobertifyEmbedUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Pages {
    private static final HashMap<Long, List<Page>> messages = new HashMap<>();
    private static final Paginator paginator = new Paginator(
            Emoji.fromMarkdown(RobertifyEmoji.PREVIOUS_EMOJI.toString()),
            Emoji.fromMarkdown(RobertifyEmoji.REWIND_EMOJI.toString()),
            Emoji.fromMarkdown(RobertifyEmoji.PLAY_EMOJI.toString()),
            Emoji.fromMarkdown(RobertifyEmoji.END_EMOJI.toString())
    );

    public static Message paginate(TextChannel channel, User user, List<Page> pages) {
        AtomicReference<Message> ret = new AtomicReference<>();

        channel.sendMessageEmbeds(pages.get(0).getEmbed()).queue(msg -> {
            if (pages.size() > 1) {
                msg.editMessageComponents(
                        ActionRow.of(
                                Button.of(ButtonStyle.SECONDARY, MessageButton.FRONT + user.getId(), paginator.getFrontEmoji()),
                                Button.of(ButtonStyle.SECONDARY, MessageButton.PREVIOUS + user.getId(), paginator.getPreviousEmoji()),
                                Button.of(ButtonStyle.SECONDARY, MessageButton.NEXT + user.getId(), paginator.getNextEmoji()),
                                Button.of(ButtonStyle.SECONDARY, MessageButton.END + user.getId(), paginator.getEndEmoji())
                        )
                ).queue();

                messages.put(msg.getIdLong(), pages);
                ret.set(msg);
            }
        });

        return ret.get();
    }

    public static Message paginate(SlashCommandEvent event, List<Page> pages) {
        AtomicReference<Message> ret = new AtomicReference<>();

        ReplyAction replyAction = event.replyEmbeds(pages.get(0).getEmbed()).setEphemeral(true);

        if (pages.size() > 1) {
            replyAction = replyAction.addActionRows(
                    ActionRow.of(
                            Button.of(ButtonStyle.SECONDARY, MessageButton.FRONT + event.getUser().getId(), paginator.getFrontEmoji()),
                            Button.of(ButtonStyle.SECONDARY, MessageButton.PREVIOUS + event.getUser().getId(), paginator.getPreviousEmoji()),
                            Button.of(ButtonStyle.SECONDARY, MessageButton.NEXT + event.getUser().getId(), paginator.getNextEmoji()),
                            Button.of(ButtonStyle.SECONDARY, MessageButton.END + event.getUser().getId(), paginator.getEndEmoji())
                    )
            );
        }

        replyAction.queue(msg -> {
           if (pages.size() > 1) {
               msg.retrieveOriginal().queue(msg2 -> {
                   messages.put(msg2.getIdLong(), pages);
                   ret.set(msg2);
               });
           }
        });

        return ret.get();
    }

    public static Message paginate(TextChannel channel, User user, List<String> content, int maxPerPage) {
        List<Page> pages = new ArrayList<>();

        logic(channel.getGuild(), pages, content, maxPerPage);

        return paginate(channel, user, pages);
    }

    public static Message paginate(List<String> content, int maxPerPage, SlashCommandEvent event) {
        List<Page> pages = new ArrayList<>();

        logic(event.getGuild(), pages, content, maxPerPage);

        return paginate(event, pages);
    }

    private static void logic(Guild guild, List<Page> pages, List<String> content, int maxPerPage) {
        if (content.size() <= maxPerPage) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");
            for (String str : content)
                eb.appendDescription(str + "\n");
            pages.add(new Page(eb.build()));
        } else {
            int pagesRequired = (int)Math.ceil((double)content.size() / maxPerPage);

            int lastIndex = 0;
            for (int i = 0; i < pagesRequired; i++) {
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "\t");
                for (int j = 0; j < maxPerPage; j++) {
                    if (lastIndex == content.size()) break;

                    eb.appendDescription(content.get(lastIndex) + "\n");
                    lastIndex++;
                }
                pages.add(new Page(eb.build()));
            }
        }
    }

    public static List<Page> getPages(long msg) {
        return messages.get(msg);
    }
}
