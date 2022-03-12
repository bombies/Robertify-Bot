package main.commands.prefixcommands.audio;

import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackInfo;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Deprecated @ForRemoval
public class RewindCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (checks(selfVoiceState, memberVoiceState) != null) {
            msg.replyEmbeds(checks(selfVoiceState, memberVoiceState).build()).queue();
            return;
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final var audioPlayer = musicManager.getPlayer();
        final var track = audioPlayer.getPlayingTrack();
        final var guild = ctx.getGuild();

        if (track == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        long time = -1;

        if (!ctx.getArgs().isEmpty()) {
            if (GeneralUtils.stringIsInt(ctx.getArgs().get(0)))
                time = Long.parseLong(ctx.getArgs().get(0));
            else {
                eb = RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid duration to rewind");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }
        }

        msg.replyEmbeds(handleRewind(ctx.getAuthor(), selfVoiceState, time, ctx.getArgs().isEmpty()).build()).queue();
    }

    public EmbedBuilder handleRewind(User user, GuildVoiceState selfVoiceState, long time, boolean rewindToBeginning) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        final var audioPlayer = musicManager.getPlayer();
        final AudioTrack track = audioPlayer.getPlayingTrack();
        final var guild = selfVoiceState.getGuild();
        EmbedBuilder eb;

        if (track == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            return eb;
        }

        AudioTrackInfo info = track.getInfo();
        if (info.isStream())
            return RobertifyEmbedUtils.embedMessage(guild, "You can't rewind a stream!");

        if (rewindToBeginning) {
            audioPlayer.seekTo(0L);
            eb = RobertifyEmbedUtils.embedMessage(guild, "You have rewound the song to the beginning!");
            new LogUtils().sendLog(guild, LogType.TRACK_REWIND, user.getAsMention() + " has rewound `"+info.getTitle()+" by "+info.getAuthor()+"` to the beginning");
        } else {
            if (time <= 0) {
                eb = RobertifyEmbedUtils.embedMessage(guild, "The duration cannot be negative or zero!");
                return eb;
            }

            time = TimeUnit.SECONDS.toMillis(time);

            if (time > audioPlayer.getTrackPosition()) {
                eb = RobertifyEmbedUtils.embedMessage(guild, "This duration cannot be more than the current time in the song");
                return eb;
            }

            audioPlayer.seekTo(audioPlayer.getTrackPosition() - time);
            eb = RobertifyEmbedUtils.embedMessage(guild, "You have rewound the song by "+TimeUnit.MILLISECONDS.toSeconds(time)+" seconds!");
            new LogUtils().sendLog(guild, LogType.TRACK_REWIND, user.getAsMention() + " has rewound `"+info.getTitle()+" by "+info.getAuthor()+"` by "+TimeUnit.MILLISECONDS.toSeconds(time)+" seconds");
        }

        return eb;
    }

    public EmbedBuilder checks(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        final var guild = selfVoiceState.getGuild();
        EmbedBuilder eb;

        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            return eb;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            return eb;
        }

        return null;
    }

    @Override
    public String getName() {
        return "rewind";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n" +
                "Rewind the song\n" +
                "\nUsage: `"+ prefix+"rewind` *(Rewinds the song to the beginning)*\n" +
                "\nUsage: `"+ prefix+"rewind <seconds_to_rewind>` *(Rewinds the song by a specific duration)*\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("r", "rep", "repeat", "replay");
    }
}
