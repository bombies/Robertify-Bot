package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.ShuffleCommand;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class ShuffleSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("shuffle")
                        .setDescription("Shuffle the queue")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Shuffle the current queue";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        event.getHook().sendMessageEmbeds(new ShuffleCommand().handleShuffle(event.getGuild(), event.getUser()).build())
                .setEphemeral(false).queue();
    }
}
