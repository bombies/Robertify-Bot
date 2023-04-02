package main.commands.slashcommands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.MoveCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
        return """
                Move a specific track to a specific position in the queue

                Usage: `/move <id> <position>`""";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final var queueHandler = musicManager.getScheduler().getQueueHandler();
        final int id = GeneralUtils.longToInt(event.getOption("id").getAsLong());
        final int pos = GeneralUtils.longToInt(event.getOption("position").getAsLong());

        event.getHook().sendMessageEmbeds(new MoveCommand().handleMove(event.getGuild(), event.getUser(), queueHandler, id, pos).build())
                .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel()))
                .queue();
    }
}
