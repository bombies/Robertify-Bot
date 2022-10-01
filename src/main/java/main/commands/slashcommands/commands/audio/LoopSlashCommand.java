package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.LoopCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
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
        return "Set the song being currently played or the queue to constantly loop";
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

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
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel())).queue();
            case "queue" -> event.getHook().sendMessageEmbeds(new LoopCommand().handleQueueRepeat(musicManager, event.getUser()).build())
                    .setEphemeral(RobertifyEmbedUtils.getEphemeralState(event.getChannel().asGuildMessageChannel())).queue();
        }
    }
}
