package main.commands.commands.audio.slashcommands;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.utils.component.InteractiveCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.legacy.dedicatedchannel.LegacyDedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LeaveSlashCommand extends InteractiveCommand {
    private final String commandName = "disconnect";

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
                        "Disconnect the bot from the voice channel it's currently in",
                        List.of(),
                        List.of(),
                        djPredicate
                ))
                .build();
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!event.getName().equals(commandName)) return;

        event.deferReply().queue();

        EmbedBuilder eb;

        if (!getCommand().getCommand().permissionCheck(event)) {
            eb  = EmbedUtils.embedMessage("You need to be a DJ to use this command!");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();



        if (!selfVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("I'm already not in a voice channel!");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            return;
        }

        GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        musicManager.scheduler.queue.clear();

        if (musicManager.scheduler.player.getPlayingTrack() != null)
            musicManager.scheduler.player.stopTrack();

        event.getGuild().getAudioManager().closeAudioConnection();

        if (new DedicatedChannelConfig().isChannelSet(event.getGuild().getIdLong()))
            new DedicatedChannelConfig().updateMessage(event.getGuild());

        musicManager.scheduler.repeating = false;
        musicManager.scheduler.playlistRepeating = false;
        musicManager.scheduler.getPastQueue().clear();

        eb = EmbedUtils.embedMessage("Disconnected!");
        event.getHook().sendMessageEmbeds(eb.build()).queue();
    }
}
