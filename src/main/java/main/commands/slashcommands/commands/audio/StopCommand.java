package main.commands.slashcommands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.GuildMusicManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.commands.slashcommands.commands.audio.LofiCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;

public class StopCommand extends AbstractSlashCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();

        msg.replyEmbeds(handleStop(ctx.getMember()).build()).queue();
    }

    public EmbedBuilder handleStop(Member stopper) {
        Guild guild = stopper.getGuild();
        final var selfVoiceState = guild.getSelfMember().getVoiceState();
        final var memberVoiceState = stopper.getVoiceState();

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);


        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));;

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();
        final AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);

        scheduler.repeating = false;
        scheduler.playlistRepeating = false;
        audioPlayer.stopTrack();
        scheduler.queue.clear();
        scheduler.getPastQueue().clear();

        if (audioPlayer.isPaused())
            audioPlayer.setPaused(false);

        if (new DedicatedChannelConfig(guild).isChannelSet())
            new DedicatedChannelConfig(guild).updateMessage();

        LofiCommand.getLofiEnabledGuilds().remove(guild.getIdLong());

        new LogUtils(guild).sendLog(LogType.PLAYER_STOP, RobertifyLocaleMessage.StopMessages.STOPPED_LOG, Pair.of("{user}", stopper.getAsMention()));

        scheduler.scheduleDisconnect(true);
        ResumeUtils.getInstance().removeInfo(guild);
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.StopMessages.STOPPED);
    }

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public String getHelp(String prefix) {
        return "Forces the bot to stop playing music and clear the queue" +
                " if already playing music.";
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("stop")
                        .setDescription("Stop the music and clear the queue")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Forces the bot to stop playing music and clear the queue" +
                " if already playing music.";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;

        event.replyEmbeds(handleStop(event.getMember())
                .build())
                .queue();
    }
}
