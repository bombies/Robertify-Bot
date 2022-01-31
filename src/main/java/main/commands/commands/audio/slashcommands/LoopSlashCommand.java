package main.commands.commands.audio.slashcommands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.LoopCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
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
                        List.of(),
                        List.of(
                                SubCommand.of(
                                    "track",
                                    "Toggle looping the currently playing track"
                                ),
                                SubCommand.of(
                                        "queue",
                                        "Toggle looping the current queue"
                                )
                        ),
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
        final var audioPlayer = musicManager.getPlayer();
        final var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final var memberVoiceState = event.getMember().getVoiceState();

        var checks = new LoopCommand().checks(selfVoiceState, memberVoiceState, audioPlayer);

        if (checks != null) {
            event.getHook().sendMessageEmbeds(checks.build())
                    .setEphemeral(true).queue();
            return;
        }

        switch (event.getSubcommandName()) {
            case "track" -> event.getHook().sendMessageEmbeds(new LoopCommand().handleRepeat(musicManager).build())
                    .setEphemeral(false).queue();
            case "queue" -> event.getHook().sendMessageEmbeds(new LoopCommand().handleQueueRepeat(musicManager, audioPlayer, event.getGuild()).build())
                    .setEphemeral(false).queue();
        }
    }
}
