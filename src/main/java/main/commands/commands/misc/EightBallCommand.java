package main.commands.commands.misc;

import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Permission;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.eightball.EightBallConfig;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.component.legacy.InteractiveCommand;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;

import javax.script.ScriptException;
import java.util.List;
import java.util.Random;

public class EightBallCommand extends InteractiveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (!new TogglesConfig().getToggle(ctx.getGuild(), Toggles.EIGHT_BALL))
            return;

        GeneralUtils.setCustomEmbed(ctx.getGuild(), "");

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "You must provide something for me to respond to...")
                    .build())
                    .queue();
            return;
        }

        switch (args.get(0).toLowerCase()) {
            case "add" -> msg.replyEmbeds(handleAdd(ctx.getGuild(), ctx.getMember(), String.join(" ", args.subList(1, args.size()))).build())
                    .queue();
            case "remove" -> {
                if (args.size() < 2) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "You must provide an index to remove!").build())
                            .queue();
                    return;
                }

                if (!GeneralUtils.stringIsInt(args.get(1))) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "You must provide a valid integer as the index!").build())
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
            default -> msg.replyEmbeds(handle8Ball(ctx.getGuild()).build())
                    .queue();
        }

        GeneralUtils.setDefaultEmbed(ctx.getGuild());
    }

    private EmbedBuilder handleAdd(Guild guild, Member user, String phraseToAdd) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have enough permissions to execute this command" +
                    "\n\nYou must have `"+Permission.ROBERTIFY_8BALL.name()+"`!");

        if (phraseToAdd == null)
            return RobertifyEmbedUtils.embedMessage(guild, "You need to provide a response to add!");

        if (phraseToAdd.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, "You need to provide a response to add!");
        var config = new EightBallConfig();

        try {
            if (config.getResponses(guild.getIdLong()).contains(phraseToAdd))
                return RobertifyEmbedUtils.embedMessage(guild, "This is already a response!");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        config.addResponse(guild.getIdLong(), phraseToAdd);
        return RobertifyEmbedUtils.embedMessage(guild, "Added `"+phraseToAdd+"` as a response!");
    }

    private EmbedBuilder handleRemove(Guild guild, Member user, int index) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have enough permissions to execute this command" +
                    "\n\nYou must have `"+Permission.ROBERTIFY_8BALL.name()+"`!");

        var config = new EightBallConfig();

        try {
            if (index > config.getResponses(guild.getIdLong()).size() || index < 0)
                return RobertifyEmbedUtils.embedMessage(guild, "This is not a response!");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        var eb = RobertifyEmbedUtils.embedMessage(guild, "Removed `"+config.getResponses(guild.getIdLong()).get(index)+"` as a response!");
        config.removeResponse(guild.getIdLong(), index);
        return eb;
    }

    private EmbedBuilder handleClear(Guild guild, Member user) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have enough permissions to execute this command" +
                    "\n\nYou must have `"+Permission.ROBERTIFY_8BALL.name()+"`!");

        var config = new EightBallConfig();
        config.removeAllResponses(guild.getIdLong());

        return RobertifyEmbedUtils.embedMessage(guild, "You have cleared all of your custom responses!");
    }

    private EmbedBuilder handleList(Guild guild, Member user) {
        if (!GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_8BALL))
            return RobertifyEmbedUtils.embedMessage(guild, "You do not have enough permissions to execute this command" +
                    "\n\nYou must have `"+Permission.ROBERTIFY_8BALL.name()+"`!");

        var config = new EightBallConfig();
        final var responses = config.getResponses(guild.getIdLong());

        if (responses.isEmpty())
            return RobertifyEmbedUtils.embedMessage(guild, "There are no custom responses!");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < responses.size(); i++)
            sb.append("*").append(i).append("* → ").append(responses.get(i)).append("\n");

        return RobertifyEmbedUtils.embedMessage(guild, "**List of Responses**\n\n" + sb);
    }

    private EmbedBuilder handle8Ball(Guild guild) {
        final var affirmativeAnswers = List.of(
                "It is certain.",
                "It is decidedly so.",
                "Without a doubt.",
                "Yes definitely.",
                "You may rely on it.",
                "As I see it, yes.",
                "Most likely.",
                "Outlook good.",
                "Yes.",
                "Signs point to yes.");

        final var nonCommittalAnswers = List.of(
            "Reply hazy, try again.",
                "Ask again later.",
                "Better not tell you now.",
                "Cannot predict now.",
                "Concentrate and ask again."
                );

        final var negativeAnswers = List.of(
            "Don't count on it.",
                "My reply is no.",
                "My sources say no.",
                "Outlook not so good.",
                "Very doubtful."
                );

        final var customAnswers = new EightBallConfig().getResponses(guild.getIdLong());

        final var random = new Random().nextDouble();

        if (!customAnswers.isEmpty()) {
            if (random < 0.11) {
                return RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  affirmativeAnswers.get(new Random().nextInt(affirmativeAnswers.size())));
            } else if (random > 0.11 && random < 0.22) {
                return RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  nonCommittalAnswers.get(new Random().nextInt(nonCommittalAnswers.size())));
            } else if (random > 0.22 && random < 0.33) {
                return RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  negativeAnswers.get(new Random().nextInt(negativeAnswers.size())));
            } else if (random > 0.33) {
                return RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  customAnswers.get(new Random().nextInt(customAnswers.size())));
            }
        } else {
            if (random < 0.5) {
                return RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  affirmativeAnswers.get(new Random().nextInt(affirmativeAnswers.size())));
            } else if (random > 0.5 && random < 0.75) {
                return RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  nonCommittalAnswers.get(new Random().nextInt(nonCommittalAnswers.size())));
            } else if (random > 0.75) {
                return RobertifyEmbedUtils.embedMessage(guild, "🎱| " +  negativeAnswers.get(new Random().nextInt(nonCommittalAnswers.size())));
            }
        }

        return RobertifyEmbedUtils.embedMessage(guild, "Something went wrong!");
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
    public void initCommand() {
        setInteractionCommand(getCommand());
        upsertCommand();
    }

    @Override
    public void initCommand(Guild g) {
        setInteractionCommand(getCommand());
        upsertCommand(g);
    }

    private InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        getName(),
                        "Curious of your fate?",
                        List.of(),
                        List.of(
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
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(getName())) return;

        final var guild = event.getGuild();
        final var user = event.getMember();

        switch(event.getSubcommandName()) {
            case "ask" -> event.replyEmbeds(handle8Ball(guild).build())
                    .setEphemeral(false)
                    .queue();
            case "add" -> {
                final var response = event.getOption("response").getAsString();
                event.replyEmbeds(handleAdd(guild, user, response).build())
                        .setEphemeral(false)
                        .queue();
            }
            case "remove" -> {
                final var indexToRemove = event.getOption("index").getAsLong();
                event.replyEmbeds(handleRemove(guild, user, (int) indexToRemove).build())
                        .setEphemeral(false)
                        .queue();
            }
            case "clear" -> event.replyEmbeds(handleClear(guild, user).build())
                    .setEphemeral(false)
                    .queue();
            case "list" -> event.replyEmbeds(handleList(guild, user).build())
                    .setEphemeral(false)
                    .queue();
        }
    }
}
