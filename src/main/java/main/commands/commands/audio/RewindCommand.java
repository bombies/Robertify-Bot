package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.BotDB;
import main.utils.database.sqlite3.ServerDB;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

        GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        AudioPlayer audioPlayer = musicManager.audioPlayer;
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        long time = -1;

        if (!ctx.getArgs().isEmpty()) {
            if (GeneralUtils.stringIsInt(ctx.getArgs().get(0)))
                time = Long.parseLong(ctx.getArgs().get(0));
            else {
                eb = EmbedUtils.embedMessage("You must provide a valid duration to rewind");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }
        }

        msg.replyEmbeds(handleRewind(selfVoiceState, time, ctx.getArgs().isEmpty()).build()).queue();
    }

    public EmbedBuilder handleRewind(GuildVoiceState selfVoiceState, long time, boolean rewindToBeginning) {
        final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(selfVoiceState.getGuild());
        final AudioPlayer audioPlayer = musicManager.audioPlayer;
        final AudioTrack track = audioPlayer.getPlayingTrack();
        EmbedBuilder eb;

        if (track == null) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            return eb;
        }

        if (rewindToBeginning) {
            track.setPosition(0L);
            eb = EmbedUtils.embedMessage("You have rewound the song to the beginning!");
        } else {
            if (time <= 0) {
                eb = EmbedUtils.embedMessage("The duration cannot be negative or zero!");
                return eb;
            }

            time = TimeUnit.SECONDS.toMillis(time);

            if (time > track.getPosition()) {
                eb = EmbedUtils.embedMessage("This duration cannot be more than the current time in the song");
                return eb;
            }

            track.setPosition(track.getPosition() - time);
            eb = EmbedUtils.embedMessage("You have rewound the song by "+TimeUnit.MILLISECONDS.toSeconds(time)+" seconds!");
        }

        return eb;
    }

    public EmbedBuilder checks(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState) {
        EmbedBuilder eb;

        if (!selfVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            return eb;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
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
        return List.of("r", "rep", "repeat");
    }
}
