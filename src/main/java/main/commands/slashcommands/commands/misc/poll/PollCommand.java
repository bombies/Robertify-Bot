package main.commands.slashcommands.commands.misc.poll;

import lombok.Getter;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.constants.Toggles;
import main.constants.TimeFormat;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PollCommand extends AbstractSlashCommand implements ICommand {
    @Getter
    final static HashMap<Long, HashMap<Integer, Integer>> pollCache = new HashMap<>();

    final Logger logger = LoggerFactory.getLogger(PollCommand.class);

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var guild = ctx.getGuild();

        if (!new TogglesConfig(guild).getToggle(Toggles.POLLS))
            return;

        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_DJ)
            && !GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_POLLS)) {
            ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You do not have enough permissions " +
                    "to execute this command!\n\n" +
                            "You must either have `"+Permission.ROBERTIFY_DJ.name()+"`," +
                            " or `"+Permission.ROBERTIFY_POLLS.name()+"`!").build())
                    .queue();
            return;
        }

        final List<String> args = ctx.getArgs();
        final TextChannel channel = ctx.getChannel();
        final Message msg = ctx.getMessage();

        GeneralUtils.setCustomEmbed(ctx.getGuild(), "Polls");

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide arguments!").build())
                    .queue();
            GeneralUtils.setDefaultEmbed(ctx.getGuild());
            return;
        }

        msg.replyEmbeds(handlePoll(channel, ctx.getAuthor(), String.join(" ", args)).build())
                .queue(pollMsg -> {
                    pollMsg.delete().queueAfter(5, TimeUnit.SECONDS);
                    msg.delete().queueAfter(5, TimeUnit.SECONDS);
                });

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    public EmbedBuilder handlePoll(TextChannel channel, User sender, String question) {
        final Pattern choiceWithDurationRegex = Pattern.compile("^[\\w\\d\\W\\D]+(\\s\"[\\w\\d\\W\\D\\s]+\"(?=\\s))\\s\\d+[sSmMhHdD]\\s$");
        final Pattern choiceRegex = Pattern.compile("^[\\w\\d\\W\\D]+(\\s\"[\\w\\d\\W\\D\\s]+\"(?=\\s))\\s$");
        final var guild = channel.getGuild();

        question += " ";
        if (!choiceWithDurationRegex.matcher(question).matches() && !choiceRegex.matcher(question).matches())
            return RobertifyEmbedUtils.embedMessage(guild, """
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
                return RobertifyEmbedUtils.embedMessage(guild, e.getMessage());
            }
        }

        question = question.replaceAll("[0-9]+[sSmMhHdD]", "");

        var strings = question.split("\"");
        question = strings[0];

        ArrayList<String> choices = new ArrayList<>(Arrays.stream(strings).toList().subList(1, strings.length));

        choices.removeIf(s -> s.isEmpty() || s.isBlank());

        if (choices.size() < 2)
            return RobertifyEmbedUtils.embedMessage(guild, "You must provide at least 2 options");

        if (choices.size() > 9)
            return RobertifyEmbedUtils.embedMessage(guild, "You must set at most 9 options");

        final var localeManager = LocaleManager.getLocaleManager(guild);
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "**" + question + "**");

        eb.appendDescription(
                (
                        endTime != -1 ?
                                (localeManager.getMessage(RobertifyLocaleMessage.PollMessages.POLL_ENDS_AT, Pair.of("{time}", GeneralUtils.formatDate(endTime, TimeFormat.DD_M_YYYY_HH_MM_SS))))
                                :
                                ""
                ) + "\n\n");

        for (int i = 1; i  <= choices.size(); i++)
            eb.appendDescription(GeneralUtils.parseNumEmoji(i) + " - *" + choices.get(i-1) + "*\n\n");

        eb.setThumbnail("https://i.imgur.com/owL8bGL.png");
        eb.setFooter(localeManager.getMessage(RobertifyLocaleMessage.PollMessages.POLL_BY, Pair.of("{user}", sender.getAsTag())));
        eb.setTimestamp(Instant.now());

        String finalQuestion = question;
        boolean finalEndPoll = endPoll;
        long finalDuration = duration;
        channel.sendMessageEmbeds(eb.build())
                .queue(msg -> {
                    HashMap<Integer, Integer> map = new HashMap<>();
                    for (int i = 1; i <= choices.size(); i++) {
                        msg.addReaction(Emoji.fromFormatted(GeneralUtils.parseNumEmoji(i))).queue();
                        map.put(i, 0);
                    }

                    pollCache.put(msg.getIdLong(), map);

                    if (finalEndPoll)
                        doPollEnd(msg, sender, finalQuestion, choices, finalDuration);
                });

        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PollMessages.POLL_SENT);
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

                final var localeManager = LocaleManager.getLocaleManager(msg.getGuild());
                EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(msg.getGuild(), RobertifyLocaleMessage.PollMessages.POLL_ENDED, Pair.of("{question}", question));
                eb.setThumbnail("https://i.imgur.com/owL8bGL.png");
                eb.appendDescription("\n");
                eb.addField(localeManager.getMessage(RobertifyLocaleMessage.PollMessages.POLL_WINNER_LABEL), choices.get(winner-1) + "\n\n", false);
                eb.setFooter(localeManager.getMessage(RobertifyLocaleMessage.PollMessages.POLL_BY, Pair.of("{user}", sender.getAsTag())));
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
    public String getHelp(String prefix) {
        return """
                Want to ask your community members a question? This is the right tool for you. You are able to poll up to 9 choices with an optional time period.
                *This is a DJ only command*

                **Usages**
                `poll <question> "[choice]" "[choice]" ...`
                `poll <question> "[choice]" "[choice]" ... <num><s|m|d|h>`

                **Example**: `poll Best poll ever? "Yes" "Yes again" 15s` *(15 second poll)*""";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("polls")
                        .setDescription("Poll your community!")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.STRING,
                                        "question",
                                        "The question to poll",
                                        true
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice1",
                                        "First choice",
                                        true
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice2",
                                        "Second choice",
                                        true
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice3",
                                        "Third choice",
                                        false
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice4",
                                        "Fourth choice",
                                        false
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice5",
                                        "Fifth choice",
                                        false
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice6",
                                        "Sixth choice",
                                        false
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice7",
                                        "Seventh choice",
                                        false
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice8",
                                        "Eighth choice",
                                        false
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "choice9",
                                        "Ninth choice",
                                        false
                                ),
                                CommandOption.of(
                                        OptionType.STRING,
                                        "duration",
                                        "How long should the poll last?",
                                        false
                                )
                        )
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return """
                Want to ask your community members a question? This is the right tool for you. You are able to poll up to 9 choices with an optional time period.
                
                *NOTE: ROBERTIFY_POLLS is required to run this command, or you must be a DJ.*""";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final var guild = event.getGuild();
        if (!new TogglesConfig(guild).getToggle(Toggles.POLLS)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DISABLED_FEATURE).build())
                    .queue();
            return;
        }

        final List<String> choices = new ArrayList<>();
        final var question = event.getOption("question").getAsString();


        for (int i = 0; i < 8; i++) {
            OptionMapping option = event.getOption("choice" + (i + 1));
            if (option != null)
                choices.add(option.getAsString());
        }

        final var durationStr = event.getOption("duration") == null ? null : event.getOption("duration").getAsString();

        boolean endPoll = false;
        long duration = -1;
        long endTime = -1;

        if (durationStr != null) {
            if (Pattern.matches("^[0-9]+[sSmMhHdD]$", durationStr)) {
                endPoll = true;

                try {
                    endTime = GeneralUtils.getFutureTime(durationStr);
                    duration = GeneralUtils.getStaticTime(durationStr);
                } catch (Exception e) {
                    event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, e.getMessage()).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }
            }
        }


        final EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "**" + question + "**");
        final var localeManager = LocaleManager.getLocaleManager(guild);

        eb.appendDescription(
                (
                        endTime != -1 ?
                                (localeManager.getMessage(RobertifyLocaleMessage.PollMessages.POLL_ENDS_AT, Pair.of("{time}", GeneralUtils.formatDate(endTime, TimeFormat.DD_M_YYYY_HH_MM_SS))))
                                :
                                ""
                ) + "\n\n");

        for (int i = 1; i  <= choices.size(); i++)
            eb.appendDescription(GeneralUtils.parseNumEmoji(i) + " - *" + choices.get(i-1) + "*\n\n");

        eb.setThumbnail("https://i.imgur.com/owL8bGL.png");
        eb.setFooter(localeManager.getMessage(RobertifyLocaleMessage.PollMessages.POLL_BY, Pair.of("{user}", event.getUser().getAsTag())));
        eb.setTimestamp(Instant.now());

        boolean finalEndPoll = endPoll;
        long finalDuration = duration;
        event.getChannel().sendMessageEmbeds(eb.build())
                .queue(msg -> {
                    HashMap<Integer, Integer> map = new HashMap<>();
                    for (int i = 1; i <= choices.size(); i++) {
                        msg.addReaction(Emoji.fromFormatted(GeneralUtils.parseNumEmoji(i))).queue();
                        map.put(i, 0);
                    }

                    pollCache.put(msg.getIdLong(), map);

                    if (finalEndPoll)
                        doPollEnd(msg, event.getUser(), question, choices, finalDuration);
                });

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PollMessages.POLL_SENT)
                .build())
                .setEphemeral(true)
                .queue();
    }
}
