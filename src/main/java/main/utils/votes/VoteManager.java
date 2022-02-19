package main.utils.votes;

import lombok.SneakyThrows;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import org.discordbots.api.client.DiscordBotListAPI;

import java.util.Random;

public class VoteManager {
    final static DiscordBotListAPI topGG = Robertify.getTopGGAPI();

    /**
     * Check if a specific user has voted on a specific site
     * @param id String id of the user to check
     * @param website Website to check
     * @return True/False
     */
    @SneakyThrows
    public boolean userVoted(String id, Website website) {
        switch (website) {
            case TOP_GG -> {
                return topGG.hasVoted(id).toCompletableFuture().get();
            }
            default -> throw new UnsupportedOperationException(website.name() + " isn't supported yet!");
        }
    }

    /**
     * Check if a specific user has voted on any of the available sites
     * @param id String id
     * @return True/False
     */
    @SneakyThrows
    public boolean userVoted(String id) {
        return userVoted(id, Website.TOP_GG);
    }

    public void sendReminder(Message msg) {
        if (new Random().nextDouble() > Double.parseDouble(Config.get(ENV.VOTE_REMINDER_CHANCE)))
            return;

        if (userVoted(msg.getAuthor().getId()))
            return;

        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(msg.getGuild(), msg.getAuthor().getAsMention() + " you haven't voted for us today!\n" +
                "You can help support us by [voting](https://robertify.me/vote) for us through the `vote` command!").build())
                .queue();
    }

    public enum Website {
        TOP_GG,
        DBL,
        DISCORDS
    }
}
