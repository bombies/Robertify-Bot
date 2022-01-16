package main.commands;

import main.constants.ENV;
import main.main.Config;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.cache.BotInfoCache;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

import java.time.Instant;
import java.util.List;
import java.util.Random;

public class RandomMessageManager {
    private final static double CHANCE = Double.parseDouble(Config.get(ENV.RANDOM_MESSAGE_CHANCE));

    public MessageEmbed getMessage(Guild guild) {
        List<String> messages = BotInfoCache.getInstance().getRandomMessages();

        if (messages.isEmpty())
            throw new NullPointerException("There are no random messages!");

        return RobertifyEmbedUtils.embedMessage(guild, messages.get(new Random().nextInt(messages.size())))
                .setTitle("✨ Robertify Notice")
                .setTimestamp(Instant.now())
                .build();
    }

    public boolean hasMessages() {
        return !getMessages().isEmpty();
    }

    public void addMessage(String s) {
        BotInfoCache.getInstance().addRandomMessage(s);
    }

    public List<String> getMessages() {
        return BotInfoCache.getInstance().getRandomMessages();
    }

    public void removeMessage(int id) {
        BotInfoCache.getInstance().removeMessage(id);
    }

    public void clearMessages() {
        BotInfoCache.getInstance().clearMessages();
    }

    public void randomlySendMessage(TextChannel channel) {
        if (!hasMessages()) return;

        if (new Random().nextDouble() <= CHANCE)
            channel.sendMessageEmbeds(getMessage(channel.getGuild())).queue();
    }
}
