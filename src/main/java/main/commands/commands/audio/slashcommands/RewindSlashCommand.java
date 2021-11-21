package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.RewindCommand;
import main.utils.component.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RewindSlashCommand extends InteractiveCommand {
    private final String commandName = new RewindCommand().getName();

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
                    "Rewind the song by the seconds provided or all the way to the beginning",
                    List.of(CommandOption.of(
                            OptionType.INTEGER,
                            "seconds",
                            "Seconds to rewind the song by",
                            false
                    ))
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        long time = -1;

        if (!event.getOptions().isEmpty()) {
            time = event.getOption("seconds").getAsLong();
        }

        event.replyEmbeds(new RewindCommand().handleRewind(event.getGuild().getSelfMember().getVoiceState(), time, event.getOptions().isEmpty()).build())
                .queue();
    }
}
