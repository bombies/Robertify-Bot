package main.commands;

import lombok.Setter;
import main.constants.ENV;
import main.constants.Toggles;
import main.main.Config;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Instant;
import java.util.List;
import java.util.Random;

public class RandomMessageManager {
    @Setter
    private static double chance = Double.parseDouble(Config.get(ENV.RANDOM_MESSAGE_CHANCE));

    public MessageEmbed getMessage(Guild guild) {
        LocaleManager localeManager = LocaleManager.getLocaleManager(guild);

        List<String> messages = BotBDCache.getInstance().getRandomMessages();

        if (messages.isEmpty())
            throw new NullPointerException(localeManager.getMessage(RobertifyLocaleMessage.RandomMessages.NO_RANDOM_MESSAGES));

        return RobertifyEmbedUtils.embedMessage(guild, messages.get(new Random().nextInt(messages.size())))
                .setTitle(localeManager.getMessage(RobertifyLocaleMessage.RandomMessages.TIP_TITLE))
                .setFooter(localeManager.getMessage(RobertifyLocaleMessage.RandomMessages.TIP_FOOTER))
                .setTimestamp(Instant.now())
                .build();
    }

    public boolean hasMessages() {
        return getMessages().size() != 0;
    }

    public void addMessage(String s) {
        BotBDCache.getInstance().addRandomMessage(s);
    }

    public List<String> getMessages() {
        return BotBDCache.getInstance().getRandomMessages();
    }

    public void removeMessage(int id) {
        BotBDCache.getInstance().removeMessage(id);
    }

    public void clearMessages() {
        BotBDCache.getInstance().clearMessages();
    }

    public void randomlySendMessage(TextChannel channel) {
        if (!new TogglesConfig(channel.getGuild()).getToggle(Toggles.TIPS))
            return;

        final var dedicatedChannelConfig = new DedicatedChannelConfig(channel.getGuild());
        if (dedicatedChannelConfig.isChannelSet()) {
            if (dedicatedChannelConfig.getChannelID() == channel.getIdLong())
                return;
        }

        if (!hasMessages()) return;

        if (new Random().nextDouble() <= chance)
            channel.sendMessageEmbeds(getMessage(channel.getGuild()))
                    .queue();
    }
}
