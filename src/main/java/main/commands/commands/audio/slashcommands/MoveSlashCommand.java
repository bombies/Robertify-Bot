package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.lavalink.LavaLinkGuildMusicManager;
import main.audiohandlers.lavaplayer.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.MoveCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MoveSlashCommand extends InteractiveCommand {
    private final String commandName = new MoveCommand().getName();

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

    public InteractionCommand getCommand() {
        return InteractionCommand.create()
                .setCommand(Command.of(
                        commandName,
                        "Rearrange the position of tracks in the queue",
                        List.of(
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
                        ),
                        List.of(),
                        djPredicate
                )).build();

    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getLavaLinkMusicManager(event.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.getScheduler().queue;
        final int id = GeneralUtils.longToInt(event.getOption("trackid").getAsLong());
        final int pos = GeneralUtils.longToInt(event.getOption("position").getAsLong());

        event.getHook().sendMessageEmbeds(new MoveCommand().handleMove(event.getGuild(), queue, id, pos).build())
                .setEphemeral(false)
                .queue();
    }
}
