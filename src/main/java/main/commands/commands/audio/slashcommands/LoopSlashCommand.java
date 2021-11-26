package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.LoopCommand;
import main.utils.component.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LoopSlashCommand extends InteractiveCommand {
    private final String commandName = "loop";

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
                        "Replay the current song being played",
                        List.of(CommandOption.of(
                                OptionType.STRING,
                                "queue",
                                "Toggle whether you want the current queue to be repeated or not",
                                false,
                                List.of("queue")
                        ))
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;
        final var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final var memberVoiceState = event.getMember().getVoiceState();

        var checks = new LoopCommand().checks(selfVoiceState, memberVoiceState, audioPlayer);

        if (checks != null) {
            event.getHook().sendMessageEmbeds(checks.build())
                    .setEphemeral(true).queue();
            return;
        }

        if (event.getOptions().isEmpty()) {
            event.getHook().sendMessageEmbeds(new LoopCommand().handleRepeat(musicManager).build())
                    .setEphemeral(false).queue();
        } else {
            event.getHook().sendMessageEmbeds(new LoopCommand().handleQueueRepeat(musicManager, audioPlayer, event.getGuild()).build())
                    .setEphemeral(false).queue();
        }
    }
}
