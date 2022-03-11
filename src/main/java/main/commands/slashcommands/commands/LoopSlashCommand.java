package main.commands.slashcommands.commands;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.LoopCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class LoopSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("loop")
                        .setDescription("Replay the current song being played")
                        .addSubCommands(
                                SubCommand.of(
                                        "track",
                                        "Toggle looping the currently playing track"
                                ),
                                SubCommand.of(
                                        "queue",
                                        "Toggle looping the current queue"
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
        if (!nameCheck(event)) return;
        if (!banCheck(event)) return;

        event.deferReply().queue();

        if (!musicCommandDJCheck(event)) {
            event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "You need to be a DJ to run this command!").build())
                    .queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
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
            case "track" -> event.getHook().sendMessageEmbeds(new LoopCommand().handleRepeat(musicManager, event.getUser()).build())
                    .setEphemeral(false).queue();
            case "queue" -> event.getHook().sendMessageEmbeds(new LoopCommand().handleQueueRepeat(musicManager, event.getUser(), audioPlayer, event.getGuild()).build())
                    .setEphemeral(false).queue();
        }
    }
}
