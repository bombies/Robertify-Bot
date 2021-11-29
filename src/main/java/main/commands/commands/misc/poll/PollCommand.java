package main.commands.commands.misc.poll;

import lombok.Getter;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.constants.TimeFormat;
import main.utils.GeneralUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PollCommand implements ICommand {
    @Getter
    final static HashMap<Long, HashMap<Integer, Integer>> pollCache = new HashMap<>();

    final Logger logger = LoggerFactory.getLogger(PollCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_DJ)) {
            ctx.getMessage().replyEmbeds(EmbedUtils.embedMessage("You do not have enough permissions " +
                    "to execute this command!").build())
                    .queue();
            return;
        }

        final List<String> args = ctx.getArgs();
        final TextChannel channel = ctx.getChannel();
        final Message msg = ctx.getMessage();

        GeneralUtils.setCustomEmbed("Polls");

        if (args.isEmpty()) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide arguments!").build())
                    .queue();
            GeneralUtils.setDefaultEmbed();
            return;
        }

        msg.replyEmbeds(handlePoll(channel, ctx.getAuthor(), String.join(" ", args)).build())
                .queue();

        GeneralUtils.setDefaultEmbed();
    }

    public EmbedBuilder handlePoll(TextChannel channel, User sender,  String question) {
        final Pattern choiceWithDurationRegex = Pattern.compile("^[\\w\\d\\W\\D]+(\\s\"[\\w\\d\\W\\D\\s]+\"(?=\\s))\\s[0-9]+[sSmMhHdD]\\s$");
        final Pattern choiceRegex = Pattern.compile("^[\\w\\d\\W\\D]+(\\s\"[\\w\\d\\W\\D\\s]+\"(?=\\s))\\s$");

        question += " ";
        if (!choiceWithDurationRegex.matcher(question).matches() && !choiceRegex.matcher(question).matches())
            return EmbedUtils.embedMessage("""
                    Invalid format!

                    **__Correct Formats__**
                    `poll <question> "[choice]" "[choice]" ...`
                    `poll <question> "[choice]" "[choice]" ... <num><s|m|d|h>`

                    **Example**: `poll Best poll ever? "Yes" "Yes again" 15s` *(15 second poll)*""");

        boolean endPoll = false;
        long duration = -1;
        long endTime = -1;
        if (choiceWithDurationRegex.matcher(question).matches()) {
            endPoll = true;
            final String[] s = question.split(" ");
            final var durationStr = s[s.length-1];

            try {
                endTime = GeneralUtils.getFutureTime(durationStr);
                duration = GeneralUtils.getStaticTime(durationStr);
            } catch (Exception e) {
                return EmbedUtils.embedMessage(e.getMessage());
            }
        }

        question = question.replaceAll("[0-9]+[sSmMhHdD]", "");

        var strings = question.split("\"");
        question = strings[0];

        ArrayList<String> choices = new ArrayList<>(Arrays.stream(strings).toList().subList(1, strings.length));

        choices.removeIf(s -> s.isEmpty() || s.isBlank());

        if (choices.size() < 2)
            return EmbedUtils.embedMessage("You must provide at least 2 options");

        if (choices.size() > 9)
            return EmbedUtils.embedMessage("You must set at most 9 options");


        EmbedBuilder eb = EmbedUtils.embedMessage("**" + question + "**");

        eb.appendDescription(
                (
                        endTime != -1 ?
                                (" | Ends at " + GeneralUtils.formatDate(endTime, TimeFormat.DD_M_YYYY_HH_MM_SS))
                                :
                                ""
                ) + "\n\n");

        for (int i = 1; i  <= choices.size(); i++)
            eb.appendDescription(GeneralUtils.parseNumEmoji(i) + " - *" + choices.get(i-1) + "*\n\n");

        eb.setThumbnail("https://i.imgur.com/owL8bGL.png");
        eb.setFooter("Poll by " + sender.getAsTag());
        eb.setTimestamp(Instant.now());

        String finalQuestion = question;
        boolean finalEndPoll = endPoll;
        long finalDuration = duration;
        channel.sendMessageEmbeds(eb.build())
                .queue(msg -> {
                    HashMap<Integer, Integer> map = new HashMap<>();
                    for (int i = 1; i <= choices.size(); i++) {
                        msg.addReaction(GeneralUtils.parseNumEmoji(i)).queue();
                        map.put(i, 0);
                    }

                    pollCache.put(msg.getIdLong(), map);

                    if (finalEndPoll)
                        doPollEnd(msg, sender, finalQuestion, choices, finalDuration);
                });

        return EmbedUtils.embedMessage("Sent poll");
    }

    private void doPollEnd(Message msg, User sender, String question, List<String> choices, long timeToEnd) {
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        final var endPoll = new Runnable() {
            @Override
            public void run() {
                HashMap<Integer, Integer> results = pollCache.get(msg.getIdLong());

                int winner = -1;
                for (int i : results.keySet()) {
                    if (winner == -1)
                        winner = i;
                    else if (results.get(i) > results.get(winner))
                        winner = i;
                }

                EmbedBuilder eb = EmbedUtils.embedMessage("**" + question + " [ENDED]**\n\n");
                eb.setThumbnail("https://i.imgur.com/owL8bGL.png");
                eb.appendDescription("\n");
                eb.addField("🎊 POLL WINNER 🎊", choices.get(winner-1) + "\n\n", false);
                eb.setFooter("Poll by " + sender.getAsTag());
                eb.setTimestamp(Instant.now());

                msg.editMessageEmbeds(eb.build())
                        .queue(editedMsg -> editedMsg.clearReactions().queue());

                pollCache.remove(msg.getIdLong());
            }
        };
        executorService.schedule(endPoll, timeToEnd, TimeUnit.MILLISECONDS);
    }

    @Override
    public String getName() {
        return "poll";
    }

    @Override
    public String getHelp(String guildID) {
        return "Want to ask your community members a question? This is the right tool for you." +
                " You are able to poll up to 9 choices with an optional time period.\n*This is a DJ only command*\n\n**Usages**\n" +
                "`poll <question> \"[choice]\" \"[choice]\" ...`\n" +
                "`poll <question> \"[choice]\" \"[choice]\" ... <num><s|m|d|h>`\n" +
                "\n**Example**: `poll Best poll ever? \"Yes\" \"Yes again\" 15s` *(15 second poll)*";
    }
}
