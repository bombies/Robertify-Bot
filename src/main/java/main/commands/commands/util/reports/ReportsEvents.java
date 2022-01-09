package main.commands.commands.util.reports;

import main.constants.BotConstants;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.cache.BotInfoCache;
import main.utils.json.legacy.reports.LegacyReportsConfig;
import main.utils.json.legacy.reports.ReportsConfigField;
import main.utils.pagination.Page;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.category.CategoryDeleteEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
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
    public void onCategoryDelete(@NotNull CategoryDeleteEvent event) {
        final var config = BotInfoCache.getInstance();

        if (!config.isReportsSetup()) return;

        if (event.getCategory().getIdLong() != config.getReportsID(ReportsConfigField.CATEGORY)) return;

        final var openedReportsChannelID = config.getReportsID(ReportsConfigField.CHANNEL);
        config.resetReportsConfig();

        TextChannel channel = Robertify.api.getTextChannelById(openedReportsChannelID);
        if (channel != null)
            channel.delete().queueAfter(1, TimeUnit.SECONDS);
    }

    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
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

            final var config = BotInfoCache.getInstance();
            final var openedReportsChannel = Robertify.api.getTextChannelById(config.getReportsID(ReportsConfigField.CHANNEL));

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

    List<Page> getQuestions() {
        final List<Page> ret = new ArrayList<>();

        ret.add(getQuestionPage("Which command/feature does the bug originate from?"));
        ret.add(getQuestionPage("Describe **in detail** how this bug can be replicated"));
        ret.add(getQuestionPage("Any additional comments?"));

        return ret;
    }

    private Page getQuestionPage(String q) {
        return new Page(EmbedUtils.embedMessageWithTitle("Bug Reports", q)
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

    private Page getNextPage(long userID) {
        return getQuestions().get(incrementPage(userID));
    }

    Page getFirstPage(long userID) {
        responses.put(userID, new ArrayList<>());
        return getQuestions().get(getCurrentPage(userID));
    }
}
