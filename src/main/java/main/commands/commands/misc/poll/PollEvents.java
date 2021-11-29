package main.commands.commands.misc.poll;

import main.utils.GeneralUtils;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class PollEvents extends ListenerAdapter {
    private final HashMap<Long, HashMap<Integer, Integer>> polls = PollCommand.getPollCache();


    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (!polls.containsKey(event.getMessageIdLong())) return;
        if (event.getUser().isBot()) return;

        final var emoji = event.getReactionEmote().getEmoji();
        var countMap = polls.get(event.getMessageIdLong());
        var oldVal = countMap.get(GeneralUtils.parseNumEmoji(emoji));

        countMap.put(GeneralUtils.parseNumEmoji(emoji), ++oldVal);
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        if (!polls.containsKey(event.getMessageIdLong())) return;
        if (event.getUser().isBot()) return;

        final var emoji = event.getReactionEmote().getEmoji();
        var countMap = polls.get(event.getMessageIdLong());
        var oldVal = countMap.get(GeneralUtils.parseNumEmoji(emoji));

        countMap.put(GeneralUtils.parseNumEmoji(emoji), --oldVal);
    }
}
