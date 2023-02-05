package main.commands.slashcommands.commands.dev;

import main.main.Robertify;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class UnloadGuildCommandsCommand extends AbstractSlashCommand {
    @Override
    protected void buildCommand() {
        setCommand(getBuilder()
                .setName("unloadguildcommands")
                .setDescription("Unload all the unnecessary guild commands.")
                .setDevCommand()
                .setGuildCommand()
                .build());
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;

        event.deferReply().queue();

        final var deleteEvent = CompletableFuture.runAsync(() -> {
            final var shards = Robertify.shardManager.getShards();
            shards.forEach(shard -> {
                shard.getGuilds()
                        .forEach(AbstractSlashCommand::unloadAllCommands);
            });
        });

        deleteEvent.thenAccept(unused -> {
            event.getHook().sendMessage("Finished unloading commands for all guilds.")
                    .setEphemeral(false)
                    .queue();
        })
                .exceptionally(e -> {
                    event.getHook().sendMessage("There was an error when attempting to unload commands.\n\nMessage: `"+e.getMessage()+"`")
                            .setEphemeral(false)
                            .queue();
                    return null;
                });


    }
}
