package main.commands.commands.audio.slashcommands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.commands.audio.RepeatCommand;
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
                                OptionType.BOOLEAN,
                                "queue",
                                "Toggle whether you want the current queue to be repeated or not",
                                false
                        ))
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(event.getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;

        if (event.getOptions().isEmpty()) {
            final var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
            final var memberVoiceState = event.getMember().getVoiceState();

            var checks = new RepeatCommand().checks(selfVoiceState, memberVoiceState, audioPlayer);

            if (checks != null) {
                event.replyEmbeds(checks.build())
                        .setEphemeral(true).queue();
                return;
            }

            event.replyEmbeds(new RepeatCommand().handleRepeat(musicManager).build())
                    .setEphemeral(false).queue();
        } else {
//            boolean choice = event.getOption("queue").getAsBoolean();

            event.replyEmbeds(new RepeatCommand().handleQueueRepeat(musicManager, audioPlayer, event.getGuild()).build())
                    .setEphemeral(false).queue();
        }
    }
}
