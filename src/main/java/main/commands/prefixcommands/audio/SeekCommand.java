package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Deprecated
public class SeekCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        if (args.isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the position you would like to jump to").build())
                    .queue();
            return;
        }

        var position = ctx.getArgs().get(0);

        if (Pattern.matches("^\\d{1,2}:\\d{2}$", position)) {
            String[] positionSplit = position.split(":");
            int mins = Integer.parseInt(positionSplit[0]);
            int secs = Integer.parseInt(positionSplit[1]);

            final GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();
            final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();

            msg.replyEmbeds(handleSeek(selfVoiceState, memberVoiceState, mins, secs).build())
                    .queue();
        } else if (Pattern.matches("^\\d{1,2}:\\d{1,2}:\\d{2}$", position)) {
            String[] positionSplit = position.split(":");
            int hours = Integer.parseInt(positionSplit[0]);
            int mins = Integer.parseInt(positionSplit[1]);
            int secs = Integer.parseInt(positionSplit[2]);

            final GuildVoiceState memberVoiceState = ctx.getMember().getVoiceState();
            final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();

            msg.replyEmbeds(handleSeek(selfVoiceState, memberVoiceState, hours, mins, secs).build())
                    .queue();
        } else {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the position in the format `mm:ss` **OR** `hh:mm:ss`").build())
                    .queue();
            return;
        }
    }

    public EmbedBuilder handleSeek(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, int mins, int sec) {
        final var guild = selfVoiceState.getGuild();

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED);

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        if (memberVoiceState.getChannel().getIdLong() != selfVoiceState.getChannel().getIdLong())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);

        if (mins < 0 || mins > 59)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.INVALID_MINUTES);


        if (sec < 0 || sec > 59)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.INVALID_SECONDS);


        long totalDurationInMillis = TimeUnit.MINUTES.toMillis(mins) + TimeUnit.SECONDS.toMillis(sec);

        AudioTrackInfo info = audioPlayer.getPlayingTrack().getInfo();
        if (totalDurationInMillis > info.length)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.POS_GT_DURATION);

        audioPlayer.seekTo(totalDurationInMillis);
        final var time = (mins > 9 ? mins : "0" + mins) + ":" + (sec > 9 ? sec : "0" + sec);
        new LogUtils().sendLog(guild, LogType.TRACK_SEEK, RobertifyLocaleMessage.SeekMessages.SOUGHT_LOG,
                Pair.of("{user}", memberVoiceState.getMember().getAsMention()),
                Pair.of("{time}", time),
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author)
        );
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.SOUGHT,
                Pair.of("{time}", time),
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author)
        );

    }

    public EmbedBuilder handleSeek(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, int hours, int mins, int sec) {
        final var guild = selfVoiceState.getGuild();

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED);

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        if (memberVoiceState.getChannel().getIdLong() != selfVoiceState.getChannel().getIdLong())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.NOTHING_PLAYING);

        if (mins < 0 || mins > 59)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.INVALID_MINUTES);


        if (sec < 0 || sec > 59)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.INVALID_SECONDS);


        long totalDurationInMillis = TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(mins) + TimeUnit.SECONDS.toMillis(sec);

        AudioTrackInfo info = audioPlayer.getPlayingTrack().getInfo();
        if (totalDurationInMillis > info.length)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.POS_GT_DURATION);

        audioPlayer.seekTo(totalDurationInMillis);
        final var time = (hours > 9 ? hours : "0" + hours) + ":"+ (mins > 9 ? mins : "0" + mins) + ":"+ (sec > 9 ? sec : "0" + sec);
        new LogUtils().sendLog(guild, LogType.TRACK_SEEK, RobertifyLocaleMessage.SeekMessages.SOUGHT_LOG,
                Pair.of("{user}", memberVoiceState.getMember().getAsMention()),
                Pair.of("{time}", time),
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author)
        );
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.SeekMessages.SOUGHT,
                Pair.of("{time}", time),
                Pair.of("{title}", info.title),
                Pair.of("{author}", info.author)
        );
    }

    @Override
    public String getName() {
        return "seek";
    }

    @Override
    public String getHelp(String prefix) {
        return "Jump to a specific position in the current song\n" +
                "\nUsage: `"+ prefix+"seek <mm:ss>`\n";
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
