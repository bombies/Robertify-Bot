package main.commands.commands.audio.slashcommands;

import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.SkipCommand;
import main.commands.commands.audio.SkipToCommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.legacy.InteractiveCommand;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
                        )),
                        djPredicate
                )).build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        final var selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final var memberVoiceState = event.getMember().getVoiceState();

        if (!getCommand().getCommand().permissionCheck(event)) {
            if (selfVoiceState.getChannel().getMembers().size() != 1) {
                MessageEmbed embed = new SkipCommand().handleVoteSkip(event.getTextChannel(), selfVoiceState, memberVoiceState);
                if (embed != null) {
                    event.getHook().sendMessageEmbeds(embed)
                            .setEphemeral(false)
                            .queue();
                } else {
                    event.getHook().sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "Started a vote skip!").build())
                            .setEphemeral(true)
                            .queue();
                }
                return;
            }
        }

        if (event.getOptions().isEmpty()) {
            event.getHook().sendMessageEmbeds(new SkipCommand().handleSkip(selfVoiceState, memberVoiceState))
                    .setEphemeral(false)
                    .queue();
        } else {
            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
            final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.getScheduler().queue;
            final int tracksToSkip = GeneralUtils.longToInt(event.getOption("trackstoskip").getAsLong());
            event.getHook().sendMessageEmbeds(new SkipToCommand().handleSkip(event.getUser(), queue, musicManager, tracksToSkip).build())
                    .setEphemeral(false)
                    .queue();
        }
    }
}
