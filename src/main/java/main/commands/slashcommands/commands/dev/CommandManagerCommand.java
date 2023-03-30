package main.commands.slashcommands.commands.dev;

import main.commands.slashcommands.SlashCommandManager;
import main.main.Robertify;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CommandManagerCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("commands")
                        .setDescription("Manage slash commands for the bot")
                        .addSubCommands(
                                SubCommand.builder()
                                        .name("load")
                                        .description("Load a command")
                                        .options(List.of(
                                                CommandOption.builder()
                                                        .name("command")
                                                        .description("The command to load")
                                                        .type(OptionType.STRING)
                                                        .required(true)
                                                        .build()
                                        ))
                                        .build(),
                                SubCommand.builder()
                                        .name("unload")
                                        .description("Unload a command")
                                        .options(List.of(
                                                CommandOption.builder()
                                                        .name("command")
                                                        .description("The command to unload")
                                                        .type(OptionType.STRING)
                                                        .required(true).build()
                                        ))
                                        .build(),
                                SubCommand.builder()
                                        .name("reload")
                                        .description("Reload a command")
                                        .options(List.of(
                                                CommandOption.builder()
                                                        .name("command")
                                                        .description("The command to reload")
                                                        .type(OptionType.STRING)
                                                        .required(true).build()
                                        ))
                                        .build()
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
        if (!checks(event)) return;

        final var shardManager = Robertify.getShardManager();
        final var commandManager = SlashCommandManager.getInstance();
        final var command = event.getOption("command").getAsString();
        final var commandObject = commandManager.getCommand(command);

        if (commandObject == null) {
            event.reply(String.format("There was no such command with the name `%s`", command)).queue();
            return;
        }

        switch (event.getSubcommandName()) {
            case "load" -> event.reply("Loading command...").queue(msg -> {
                boolean[] loadedShards = new boolean[shardManager.getShardsTotal()];

                for (final var shard : shardManager.getShards()) {
                    shard.retrieveCommands().queue(commands -> {
                        if (commands.stream().noneMatch(cmd -> cmd.getName().equals(command))) {
                            shard.upsertCommand(commandObject.getCommandData()).queue();
                            loadedShards[shard.getShardInfo().getShardId()] = true;
                        } else loadedShards[shard.getShardInfo().getShardId()] = false;
                    });
                }

                final var out = new StringBuilder();
                for (int i = 0; i < loadedShards.length; i++)
                    out.append(String.format("%-20s %s\n", String.format("Shard %d", i), loadedShards[i] ? "Loaded" : "Already loaded"));

                msg.editOriginal(String.format("Completed loading. Here are your results.\n\n%s", out)).queue();
            });
            case "unload" -> event.reply("Unloading command...").queue(msg -> {
                boolean[] unloadedShards = new boolean[shardManager.getShardsTotal()];

                for (final var shard : shardManager.getShards()) {
                    shard.retrieveCommands().queue(commands -> {
                        final var c = commands.stream()
                                .filter(cmd -> cmd.getName().equals(command))
                                .findFirst()
                                .orElse(null);

                        if (c != null) {
                            shard.deleteCommandById(c.getId()).queue();
                            unloadedShards[shard.getShardInfo().getShardId()] = true;
                        } else unloadedShards[shard.getShardInfo().getShardId()] = false;
                    });
                }

                final var out = new StringBuilder();
                for (int i = 0; i < unloadedShards.length; i++)
                    out.append(String.format("%-20s %s\n", String.format("Shard %d", i), unloadedShards[i] ? "Unloaded" : "Already wasn't loaded"));

                msg.editOriginal(String.format("Completed unloading. Here are your results.\n\n%s", out)).queue();
            });
            case "reload" -> event.reply("Reloading command...").queue(msg -> {
                boolean[] reloadedShards = new boolean[shardManager.getShardsTotal()];

                for (final var shard : shardManager.getShards()) {
                    shard.retrieveCommands().queue(commands -> {
                        if (commands.stream().anyMatch(cmd -> cmd.getName().equals(command))) {
                            shard.upsertCommand(commandObject.getCommandData()).queue();
                            reloadedShards[shard.getShardInfo().getShardId()] = true;
                        } else reloadedShards[shard.getShardInfo().getShardId()] = false;
                    });
                }

                final var out = new StringBuilder();
                for (int i = 0; i < reloadedShards.length; i++)
                    out.append(String.format("%-20s %s\n", String.format("Shard %d", i), reloadedShards[i] ? "Reloaded" : "Command doesn't exist."));

                msg.editOriginal(String.format("Completed reloading. Here are your results.\n\n%s", out)).queue();
            });
        }
    }
}
