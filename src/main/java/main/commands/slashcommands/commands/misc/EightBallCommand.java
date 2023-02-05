package main.commands.slashcommands.commands.misc;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.eightball.EightBallConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import javax.script.ScriptException;
import java.util.List;
import java.util.Random;

public class EightBallCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        if (!new TogglesConfig(guild).getToggle(Toggles.EIGHT_BALL))
            return;

        GeneralUtils.setCustomEmbed(guild, "");

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), RobertifyLocaleMessage.EightBallMessages.MUST_PROVIDE_SOMETHING_TO_RESPOND_TO)
                    .build())
                    .queue();
            return;
        }

        switch (args.get(0).toLowerCase()) {
            case "add" -> msg.replyEmbeds(handleAdd(ctx.getGuild(), ctx.getMember(), String.join(" ", args.subList(1, args.size()))).build())
                    .queue();
            case "remove" -> {
                if (args.size() < 2) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), RobertifyLocaleMessage.EightBallMessages.PROVIDE_INDEX_TO_REMOVE).build())
                            .queue();
                    return;
                }

                if (!GeneralUtils.stringIsInt(args.get(1))) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), RobertifyLocaleMessage.EightBallMessages.INVALID_INDEX_INTEGER).build())
                            .queue();
                    return;
                }

                int index = Integer.parseInt(args.get(1));

                msg.replyEmbeds(handleRemove(ctx.getGuild(), ctx.getMember(), index).build())
                        .queue();
            }
            case "clear" -> msg.replyEmbeds(handleClear(ctx.getGuild(), ctx.getMember()).build())
                    .queue();
            case "list" -> msg.replyEmbeds(handleList(ctx.getGuild(), ctx.getMember()).build())
                    .queue();
            default -> {
                final var localeManager = LocaleManager.getLocaleManager(guild);

                final var affirmativeAnswers = List.of(
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_1),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_2),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_3),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_4),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_5),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_6),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_7),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_8),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_9),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_10)
                );

                final var nonCommittalAnswers = List.of(
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_1),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_2),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_3),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_4),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_5)
                );

                final var negativeAnswers = List.of(
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_1),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_2),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_3),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_4),
                        localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_5)
                );

                final var customAnswers = new EightBallConfig(guild).getResponses();

                final var random = new Random().nextDouble();

                EmbedBuilder eb = null;

                if (!customAnswers.isEmpty()) {
                    if (random < 0.11) {
                        eb = RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  affirmativeAnswers.get(new Random().nextInt(affirmativeAnswers.size())));
                    } else if (random > 0.11 && random < 0.22) {
                        eb = RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  nonCommittalAnswers.get(new Random().nextInt(nonCommittalAnswers.size())));
                    } else if (random > 0.22 && random < 0.33) {
                        eb = RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  negativeAnswers.get(new Random().nextInt(negativeAnswers.size())));
                    } else if (random > 0.33) {
                        eb = RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  customAnswers.get(new Random().nextInt(customAnswers.size())));
                    }
                } else {
                    if (random < 0.5) {
                        eb = RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  affirmativeAnswers.get(new Random().nextInt(affirmativeAnswers.size())));
                    } else if (random > 0.5 && random < 0.75) {
                        eb = RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  nonCommittalAnswers.get(new Random().nextInt(nonCommittalAnswers.size())));
                    } else if (random > 0.75) {
                        eb = RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  negativeAnswers.get(new Random().nextInt(nonCommittalAnswers.size())));
                    }
                }

                ctx.getMessage().replyEmbeds(eb.build()).queue();
            }
        }

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    private EmbedBuilder handleAdd(Guild guild, Member user, String phraseToAdd) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_8BALL.name()));

        if (phraseToAdd == null)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.MISSING_RESPONSE_TO_ADD);

        if (phraseToAdd.isEmpty())
          return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.MISSING_RESPONSE_TO_ADD);
        var config = new EightBallConfig(guild);

        try {
            if (config.getResponses().contains(phraseToAdd))
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.ALREADY_A_RESPONSE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        config.addResponse(phraseToAdd);
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.ADDED_RESPONSE, Pair.of("{response}", phraseToAdd));
    }

    private EmbedBuilder handleRemove(Guild guild, Member user, int index) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_8BALL.name()));

        var config = new EightBallConfig(guild);

        try {
            if (index > config.getResponses().size() || index < 0)
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.NOT_A_RESPONSE);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        var eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.REMOVED_RESPONSE, Pair.of("{response}", config.getResponses().get(index)));
        config.removeResponse(index);
        return eb;
    }

    private EmbedBuilder handleClear(Guild guild, Member user) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_8BALL.name()));

        var config = new EightBallConfig(guild);
        config.removeAllResponses();

        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.CLEARED_RESPONSES);
    }

    private EmbedBuilder handleList(Guild guild, Member user) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS, Pair.of("{permissions}", Permission.ROBERTIFY_8BALL.name()));

        var config = new EightBallConfig(guild);
        final var responses = config.getResponses();

        if (responses.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.NO_CUSTOM_RESPONSES);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < responses.size(); i++)
            sb.append("*").append(i).append("* → ").append(responses.get(i)).append("\n");

        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.LIST_OF_RESPONSES, Pair.of("{responses}", sb.toString()));
    }

    private EmbedBuilder handle8Ball(Guild guild, User asker, String question) {
        final var localeManager = LocaleManager.getLocaleManager(guild);
        final var affirmativeAnswers = List.of(
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_1),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_2),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_3),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_4),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_5),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_6),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_7),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_8),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_9),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_AF_10)
        );

        final var nonCommittalAnswers = List.of(
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_1),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_2),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_3),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_4),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_NC_5)
        );

        final var negativeAnswers = List.of(
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_1),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_2),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_3),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_4),
                localeManager.getMessage(RobertifyLocaleMessage.EightBallMessages.EB_N_5)
        );

        final var customAnswers = new EightBallConfig(guild).getResponses();

        final var random = new Random().nextDouble();

        if (!customAnswers.isEmpty()) {
            if (random < 0.11) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.QUESTION_ASKED,
                        Pair.of("{user}", asker.getAsMention()),
                        Pair.of("{question}", question),
                        Pair.of("{response}", affirmativeAnswers.get(new Random().nextInt(affirmativeAnswers.size())))
                );
            } else if (random > 0.11 && random < 0.22) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.QUESTION_ASKED,
                        Pair.of("{user}", asker.getAsMention()),
                        Pair.of("{question}", question),
                        Pair.of("{response}",nonCommittalAnswers.get(new Random().nextInt(nonCommittalAnswers.size())))
                );
            } else if (random > 0.22 && random < 0.33) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.QUESTION_ASKED,
                        Pair.of("{user}", asker.getAsMention()),
                        Pair.of("{question}", question),
                        Pair.of("{response}", negativeAnswers.get(new Random().nextInt(negativeAnswers.size())))
                );
            } else if (random > 0.33) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.QUESTION_ASKED,
                        Pair.of("{user}", asker.getAsMention()),
                        Pair.of("{question}", question),
                        Pair.of("{response}", customAnswers.get(new Random().nextInt(customAnswers.size())))
                );
            }
        } else {
            if (random < 0.5) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.QUESTION_ASKED,
                        Pair.of("{user}", asker.getAsMention()),
                        Pair.of("{question}", question),
                        Pair.of("{response}", affirmativeAnswers.get(new Random().nextInt(affirmativeAnswers.size())))
                );
            } else if (random > 0.5 && random < 0.75) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.QUESTION_ASKED,
                        Pair.of("{user}", asker.getAsMention()),
                        Pair.of("{question}", question),
                        Pair.of("{response}", nonCommittalAnswers.get(new Random().nextInt(nonCommittalAnswers.size())))
                );
            } else if (random > 0.75) {
                return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.EightBallMessages.QUESTION_ASKED,
                        Pair.of("{user}", asker.getAsMention()),
                        Pair.of("{question}", question),
                        Pair.of("{response}", negativeAnswers.get(new Random().nextInt(negativeAnswers.size())))
                );
            }
        }

        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.UNEXPECTED_ERROR);
    }

    @Override
    public String getName() {
        return "8ball";
    }

    @Override
    public String getHelp(String prefix) {
        return "Want to determine your fate? Take a chance with the 8ball!\n" +
                "\n**__Usages__**\n" +
                "`"+ prefix+"8ball <question>` *(Ask 8ball a question)*\n" +
                "`"+ prefix+"8ball add <response>` *(Add a custom response to 8ball)*\n" +
                "`"+ prefix+"8ball remove <responseID>` *(Remove a custom response from 8ball)*\n" +
                "`"+ prefix+"8ball list` *(List all custom responses)*\n" +
                "`"+ prefix+"8ball clear` *(Clear all custom responses)*";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("8ball")
                        .setDescription("Curious of your fate?")
                        .addSubCommands(
                                SubCommand.of(
                                        "add",
                                        "Add a custom response to 8ball!",
                                        List.of(CommandOption.of(
                                                OptionType.STRING,
                                                "response",
                                                "The response to add",
                                                true
                                        ))
                                ),
                                SubCommand.of(
                                        "ask",
                                        "Ask 8ball a question!",
                                        List.of(CommandOption.of(
                                                OptionType.STRING,
                                                "question",
                                                "The question to ask",
                                                true
                                        ))
                                ),
                                SubCommand.of(
                                        "remove",
                                        "Remove a custom response from 8ball!",
                                        List.of(CommandOption.of(
                                                OptionType.INTEGER,
                                                "index",
                                                "The of the response to remove",
                                                true
                                        ))
                                ),
                                SubCommand.of(
                                        "clear",
                                        "Clear all custom responses from 8ball!"
                                ),
                                SubCommand.of(
                                        "list",
                                        "List all custom 8ball responses!"
                                )
                        )
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        final var guild = event.getGuild();

        if (!new TogglesConfig(guild).getToggle(Toggles.EIGHT_BALL)) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DISABLED_FEATURE).build())
                    .queue();
            return;
        }

        final var user = event.getMember();

        switch(event.getSubcommandName()) {
            case "ask" -> {
                GeneralUtils.setCustomEmbed(event.getGuild(), "");
                event.replyEmbeds(handle8Ball(guild, event.getUser(), event.getOption("question").getAsString()).build())
                        .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                        .queue();
                GeneralUtils.setDefaultEmbed(event.getGuild());
            }
            case "add" -> {
                final var response = event.getOption("response").getAsString();
                event.replyEmbeds(handleAdd(guild, user, response).build())
                        .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                        .queue();
            }
            case "remove" -> {
                final var indexToRemove = event.getOption("index").getAsLong();
                event.replyEmbeds(handleRemove(guild, user, (int) indexToRemove).build())
                        .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                        .queue();
            }
            case "clear" -> event.replyEmbeds(handleClear(guild, user).build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                    .queue();
            case "list" -> event.replyEmbeds(handleList(guild, user).build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                    .queue();
        }
    }
}
