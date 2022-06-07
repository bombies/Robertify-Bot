package main.commands.prefixcommands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.script.ScriptException;

@Deprecated @ForRemoval
public class PauseCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        msg.replyEmbeds(handlePauseEvent(ctx.getGuild(), selfVoiceState, memberVoiceState).build()).queue();
    }

    public EmbedBuilder handlePauseEvent(Guild guild, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        EmbedBuilder eb;

        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);
            return eb;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));
            return eb;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);
            return eb;
        }

        if (audioPlayer.isPaused()) {
            audioPlayer.setPaused(false);
            musicManager.setForcePaused(false);
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PauseMessages.RESUMED);
            new LogUtils(guild).sendLog(LogType.PLAYER_RESUME, RobertifyLocaleMessage.PauseMessages.RESUMED_LOG, Pair.of("{user}", memberVoiceState.getMember().getAsMention()));
        } else {
            audioPlayer.setPaused(true);
            musicManager.setForcePaused(true);
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.PauseMessages.PAUSED);
            new LogUtils(guild).sendLog(LogType.PLAYER_PAUSE, RobertifyLocaleMessage.PauseMessages.PAUSED_LOG, Pair.of("{user}", memberVoiceState.getMember().getAsMention()));
        }

        return eb;
    }

    @Override
    public String getName() {
        return "pause";
    }

    @Override
    public String getHelp(String prefix) {
        return "Pauses the song currently playing";
    }
}
