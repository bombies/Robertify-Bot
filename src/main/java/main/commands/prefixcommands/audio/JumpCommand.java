package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.IPlayer;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.annotation.Nullable;
import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Deprecated @ForRemoval
public class JumpCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        msg.replyEmbeds(doJump(selfVoiceState, memberVoiceState, ctx, null).build()).queue();
    }

    public EmbedBuilder doJump(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, @Nullable CommandContext ctx, @Nullable String input) {
        final var guild = memberVoiceState.getGuild();

        EmbedBuilder eb;
        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);
            return eb;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL);
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));
            return eb;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        final var audioPlayer = musicManager.getPlayer();
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);
            return eb;
        }

        if (ctx != null)
            if (ctx.getArgs().isEmpty()) {
                eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JumpMessages.JUMP_MISSING_AMOUNT);
                return eb;
            } else
                return doActualJump(ctx.getGuild(), memberVoiceState.getMember().getUser(), ctx.getArgs().get(0), audioPlayer, track);
        else {
            if (input == null)
                throw new NullPointerException("Input string cannot be null");
            return doActualJump(guild, memberVoiceState.getMember().getUser(), input, audioPlayer, track);
        }
    }

    private EmbedBuilder doActualJump(Guild guild, User jumper, String input, IPlayer player, AudioTrack track) {
        long time;
        EmbedBuilder eb;
        if (GeneralUtils.stringIsInt(input))
            time = Long.parseLong(input);
        else {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JumpMessages.JUMP_INVALID_DURATION);
            return eb;
        }

        if (time <= 0) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JumpMessages.JUMP_DURATION_NEG_ZERO);
            return eb;
        }

        time = TimeUnit.SECONDS.toMillis(time);

        if (time > track.getInfo().length - player.getTrackPosition()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JumpMessages.JUMP_DURATION_GT_TIME_LEFT);
            return eb;
        }

        player.seekTo(player.getTrackPosition() + time);
        new LogUtils().sendLog(guild, LogType.TRACK_JUMP, RobertifyLocaleMessage.JumpMessages.JUMPED_LOG, Pair.of("{user}", jumper.getAsMention()), Pair.of("{duration}", String.valueOf(time)));

        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JumpMessages.JUMPED, Pair.of("{duration}", String.valueOf(time)));
    }

    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Skips the song by the given number of seconds\n" +
                "\nUsage: `"+ prefix+"jump <seconds_to_jump>` *(Skips the song to a specific duration)*\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("j", "ff", "fastforward");
    }
}
