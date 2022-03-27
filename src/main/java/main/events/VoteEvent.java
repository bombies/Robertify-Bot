package main.events;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import main.constants.ENV;
import main.main.Config;
import main.main.Robertify;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.entities.User;

public class VoteEvent {
    private static VoteEvent INSTANCE;
    private final WebhookClient client;

    private VoteEvent() {
        WebhookClientBuilder builder = new WebhookClientBuilder(Config.get(ENV.VOTE_WEBHOOK_URL));
        builder.setThreadFactory(job -> {
            Thread thread = new Thread(job);
            thread.setName("RobertifyVotes");
            thread.setDaemon(true);
            return thread;
        });
        client = builder.build();
    }

    public void sendVoteMessage(long userID, VoteManager.Website website) {
        final User user = Robertify.shardManager.retrieveUserById(userID).complete();

        WebhookMessage message = new WebhookMessageBuilder()
                .setContent(user.getAsMention() + " has voted on:  **" + (website.equals(VoteManager.Website.TOP_GG) ? "TOP.GG" : "Discord Bot List") + "**")
                .build();

        client.send(message);
    }

    public static VoteEvent getInstance() {
        if (INSTANCE == null)
            INSTANCE = new VoteEvent();
        return INSTANCE;
    }
}
