package main.commands.slashcommands.commands;

import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.MoveCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MoveSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("move")
                        .setDescription("Rearrange the position of tracks in the queue")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "id",
                                        "The ID of the track in the queue to move",
                                        true
                                ),
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "position",
                                        "The position to move the track to",
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

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.getScheduler().queue;
        final int id = GeneralUtils.longToInt(event.getOption("trackid").getAsLong());
        final int pos = GeneralUtils.longToInt(event.getOption("position").getAsLong());

        event.getHook().sendMessageEmbeds(new MoveCommand().handleMove(event.getGuild(), event.getUser(), queue, id, pos).build())
                .setEphemeral(false)
                .queue();
    }
}
