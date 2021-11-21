package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.SkipCommand;
import main.commands.commands.audio.SkipToCommand;
import main.utils.GeneralUtils;
import main.utils.component.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SkipSlashCommand extends InteractiveCommand {
    private final String commandName = "skip";

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
                        "Skip the song currently being played",
                        List.of(CommandOption.of(
                                OptionType.INTEGER,
                                "trackstoskip",
                                "Number of tracks to skip",
                                false
                        ))
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        if (event.getOptions().isEmpty()) {
            var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
            var memberSelfVoiceState = event.getMember().getVoiceState();
            event.replyEmbeds(new SkipCommand().handleSkip(selfVoiceState, memberSelfVoiceState).build())
                    .setEphemeral(false)
                    .queue();
        } else {
            final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
            final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;
            final int tracksToSkip = GeneralUtils.longToInt(event.getOption("trackstoskip").getAsLong());
            event.replyEmbeds(new SkipToCommand().handleSkip(queue, musicManager, tracksToSkip).build())
                    .setEphemeral(false)
                    .queue();
        }
    }
}
