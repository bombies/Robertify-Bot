package main.commands.prefixcommands.util.reports;

import main.constants.BotConstants;
import main.main.Robertify;
import main.utils.database.mongodb.cache.BotBDCache;
import main.utils.pagination.MessagePage;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ReportsEvents extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(ReportsEvents.class);

    private static final HashMap<Long, Integer> currentQuestionForUser = new HashMap<>();
    private static final HashMap<Long, List<String>> responses = new HashMap<>();

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (!event.isFromType(ChannelType.CATEGORY))
            return;

        final var category = event.getChannel().asCategory();
        final var config = BotBDCache.getInstance();

        if (!config.isReportsSetup()) return;

        if (category.getIdLong() != config.getReportsID(BotBDCache.ReportsConfigField.CATEGORY)) return;

        final var openedReportsChannelID = config.getReportsID(BotBDCache.ReportsConfigField.CHANNEL);
        config.resetReportsConfig();

        TextChannel channel = Robertify.shardManager.getTextChannelById(openedReportsChannelID);
        if (channel != null)
            channel.delete().queueAfter(1, TimeUnit.SECONDS);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild()) return;

        if (!ReportsCommand.activeReports.contains(event.getAuthor().getIdLong())) return;

        final var user = event.getAuthor();
        final var channel = event.getChannel();
        try {
            final var response = event.getMessage().getContentRaw();

            if (response.chars().count() > 1024) {
                channel.sendMessageEmbeds(EmbedUtils.embedMessageWithTitle("Bug reports", "Your response cannot be more than 1024 characters!").build())
                        .queue();
                return;
            }

            final var nextPage = getNextPage(user.getIdLong());
            var responseList = responses.get(user.getIdLong());

            responseList.add(response);
            responses.put(user.getIdLong(), responseList);
            event.getChannel().sendMessageEmbeds(nextPage.getEmbed()).queue();
        } catch (IllegalStateException e) {
            var responseList = responses.get(user.getIdLong());
            responseList.add(event.getMessage().getContentRaw());
            responses.put(user.getIdLong(), responseList);

            final var collectedResponses = responses.get(user.getIdLong());
            responses.remove(user.getIdLong());

            final var config = BotBDCache.getInstance();
            final var openedReportsChannel = Robertify.shardManager.getTextChannelById(config.getReportsID(BotBDCache.ReportsConfigField.CHANNEL));

            if (openedReportsChannel == null) {
                channel.sendMessageEmbeds(EmbedUtils.embedMessage("Could not send your report!\n" +
                                "Please contact a developer in our [support server](https://discord.gg/VbjmtfJDvU).")
                        .build()).queue();
                return;
            }

            openedReportsChannel.sendMessageEmbeds(
                    new EmbedBuilder()
                            .setTitle("Bug Report from " + user.getName())
                            .setThumbnail(user.getEffectiveAvatarUrl())
                            .addField("Reporter", user.getAsMention(), false)
                            .addField("Command/Feature Origin", collectedResponses.get(0), false)
                            .addField("Reproduction of Bug", collectedResponses.get(1), false)
                            .addField("Additional Comments", collectedResponses.get(2), false)
                            .setColor(new Color(255, 106, 0))
                            .build()
            ).queue(success -> {
                ReportsCommand.activeReports.remove(user.getIdLong());
                channel.sendMessageEmbeds(EmbedUtils.embedMessageWithTitle("Bug Reports", "You have submitted your bug report. \nThank you for answering all the questions!" +
                        " Be on the lookout for a response from us!").build()).queue();
                currentQuestionForUser.remove(user.getIdLong());
            });
        }
    }

    List<MessagePage> getQuestions() {
        final List<MessagePage> ret = new ArrayList<>();

        ret.add(getQuestionPage("Which command/feature does the bug originate from?"));
        ret.add(getQuestionPage("Describe **in detail** how this bug can be replicated"));
        ret.add(getQuestionPage("Any additional comments?"));

        return ret;
    }

    private MessagePage getQuestionPage(String q) {
        return new MessagePage(EmbedUtils.embedMessageWithTitle("Bug Reports", q)
                .setThumbnail(BotConstants.ROBERTIFY_LOGO.toString())
                .setFooter("NOTE: Any abuse of this system will result in a ban")
                .setTimestamp(Instant.now())
                .build());
    }

    private int getCurrentPage(long userID) {
        currentQuestionForUser.putIfAbsent(userID, 0);
        return currentQuestionForUser.get(userID);
    }

    private int incrementPage(long userID) {
        int curPage = getCurrentPage(userID);

        if (++curPage > getQuestions().size()-1)
            throw new IllegalStateException("Cannot increment further!");

        currentQuestionForUser.put(userID, curPage);
        return curPage;
    }

    private MessagePage getNextPage(long userID) {
        return getQuestions().get(incrementPage(userID));
    }

    MessagePage getFirstPage(long userID) {
        responses.put(userID, new ArrayList<>());
        return getQuestions().get(getCurrentPage(userID));
    }
}
