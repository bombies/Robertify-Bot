package main.commands.slashcommands.commands.dev;

import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.commands.RandomMessageManager;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class RandomMessageCommand extends AbstractSlashCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();

        if (args.isEmpty()) {
            msg.reply("Please provide args").queue();
        } else {
            switch (args.get(0).toLowerCase()) {
                case "add" -> add(msg, args);
                case "remove" -> remove(msg, args);
                case "list" -> list(msg);
                case "clear" -> clear(msg);
                default -> msg.reply("Invalid args").queue();
            }
        }
    }

    public void add(Message msg, List<String> args) {
        if (args.size() < 2) {
            msg.reply("Please provide a message to add lol").queue();
            return;
        }

        final String message = String.join(" ", args.subList(1, args.size()))
                .replaceAll("\\\\n", "\n");

        new RandomMessageManager().addMessage(message);
        msg.addReaction(Emoji.fromFormatted("✅")).queue();
    }

    public void remove(Message msg, List<String> args) {
        if (args.size() < 2) {
            msg.reply("Please provide an ID to remove").queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(args.get(1))) {
            msg.reply("Invalid ID!").queue();
            return;
        }

        int id = Integer.parseInt(args.get(1));

        try {
            new RandomMessageManager().removeMessage(id);
            msg.addReaction(Emoji.fromFormatted("✅")).queue();
        } catch (IndexOutOfBoundsException e) {
            msg.reply(e.getMessage()).queue();
        }
    }

    public void list(Message msg) {
        final var messages = new RandomMessageManager().getMessages();
        final var sb = new StringBuilder();
        sb.append("```\n");
        for (int i = 0; i < messages.size(); i++)
            sb.append(i).append(" - ").append("*").append(messages.get(i)).append("*\n");
        sb.append("```");
        msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(msg.getGuild(), sb.toString()).build()).queue();
    }

    public void clear(Message msg) {
        new RandomMessageManager().clearMessages();
        msg.reply("✅").queue();
    }

    @Override
    public String getName() {
        return "randommessage";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("rm");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("randommessage")
                        .setDescription("Configure your random messages!")
                        .addSubCommands(
                                SubCommand.of(
                                        "add",
                                        "Add a random message",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.STRING,
                                                        "message",
                                                        "The message to add",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "remove",
                                        "Remove a random message",
                                        List.of(
                                                CommandOption.of(
                                                        OptionType.INTEGER,
                                                        "id",
                                                        "The ID of the message to remove",
                                                        true
                                                )
                                        )
                                ),
                                SubCommand.of(
                                        "list",
                                        "List all random messages"
                                ),
                                SubCommand.of(
                                        "clear",
                                        "Clear all random messages"
                                )
                        )
                        .setDevCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!devCheck(event)) return;

        final RandomMessageManager randomMessageManager = new RandomMessageManager();
        switch (event.getSubcommandName()) {
            case "add" -> {
                final String message = event.getOption("message").getAsString();
                randomMessageManager.addMessage(message);
                event.reply("Added `"+message+"` as a random message.")
                        .setEphemeral(true)
                        .queue();
            }
            case "remove" -> {
                final int id = (int)event.getOption("id").getAsLong();

                try {
                    final List<String> messages = randomMessageManager.getMessages();
                    randomMessageManager.removeMessage(id);
                    event.reply("Successfully removed `"+messages.get(id)+"` as a random message.")
                            .setEphemeral(true)
                            .queue();
                } catch (IndexOutOfBoundsException e) {
                    event.reply(e.getMessage())
                            .setEphemeral(true)
                            .queue();
                } catch (Exception e) {
                    event.reply("Unexpected error occured!")
                            .setEphemeral(true)
                            .queue();
                }
            }
            case "list" -> {
                final var messages = new RandomMessageManager().getMessages();
                final var sb = new StringBuilder();
                sb.append("```\n");
                for (int i = 0; i < messages.size(); i++)
                    sb.append(i).append(" - ").append("*").append(messages.get(i)).append("*\n");
                sb.append("```");
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), sb.toString()).build())
                        .setEphemeral(true)
                        .queue();
            }
            case "clear" -> {
                new RandomMessageManager().clearMessages();
                event.reply("Successfully cleared all messages!")
                        .setEphemeral(true)
                        .queue();
            }
        }
    }
}
