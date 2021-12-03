package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.RemoveCommand;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class RemoveSlashCommand extends InteractiveCommand {
    private final String commandName = new RemoveCommand().getName();

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
                        commandName,
                        "Remove a song from the queue",
                        List.of(CommandOption.of(
                                OptionType.INTEGER,
                                "trackid",
                                "The id of the track you would like to remove",
                                true
                        )),
                        djPredicate
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        if (!getCommand().getCommand().permissionCheck(event)) {
            event.getHook().sendMessageEmbeds(EmbedUtils.embedMessage("You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final int trackSelected = GeneralUtils.longToInt(event.getOption("trackid").getAsLong());
        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;

        event.getHook().sendMessageEmbeds(new RemoveCommand().handleRemove(queue, trackSelected).build())
                .setEphemeral(true)
                .queue();
    }
}
