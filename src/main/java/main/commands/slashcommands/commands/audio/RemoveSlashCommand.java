package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.RemoveCommand;
import main.utils.GeneralUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class RemoveSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("remove")
                        .setDescription("Remove a song from the queue")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "trackid",
                                        "The id of the track you would like to remove",
                                        true
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final int trackSelected = GeneralUtils.longToInt(event.getOption("trackid").getAsLong());
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final var queue = musicManager.getScheduler().queue;

        event.getHook().sendMessageEmbeds(new RemoveCommand().handleRemove(event.getGuild(), event.getUser(), queue, trackSelected).build())
                .setEphemeral(true)
                .queue();
    }
}
