package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.JumpCommand;
import main.utils.component.InteractiveCommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JumpSlashCommand extends InteractiveCommand {
    private final String commandName = new JumpCommand().getName();

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
                        "Skips the song by the given number of seconds",
                        List.of(CommandOption.of(OptionType.INTEGER, "seconds", "Seconds to skip in the song", true))
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        event.replyEmbeds(
                new JumpCommand().doJump(selfVoiceState, memberVoiceState, null, String.valueOf(event.getOption("seconds").getAsLong()))
                        .build()
        ).setEphemeral(true).queue();
    }
}
