package main.commands.prefixcommands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lavalink.client.player.IPlayer;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.GuildMusicManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.script.ScriptException;
import java.util.List;

@Deprecated @ForRemoval
public class LoopCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        var audioPlayer = musicManager.getPlayer();

        if (checks(selfVoiceState, memberVoiceState, audioPlayer) != null) {
            msg.replyEmbeds(checks(selfVoiceState, memberVoiceState, audioPlayer).build()).queue();
            return;
        }

        if (ctx.getArgs().isEmpty()) {
            msg.replyEmbeds(handleRepeat(musicManager, ctx.getAuthor()).build()).queue();
            return;
        }

        if (ctx.getArgs().get(0).equalsIgnoreCase("queue") || ctx.getArgs().get(0).equalsIgnoreCase("q")) {
            eb = handleQueueRepeat(musicManager, ctx.getAuthor());
        } else {
            eb = RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "Invalid arguments!");
        }
        msg.replyEmbeds(eb.build()).queue();
    }

    public EmbedBuilder checks(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, IPlayer audioPlayer) {
        final var guild = selfVoiceState.getGuild();
        EmbedBuilder eb;

        if (!selfVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            return eb;
        }

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            return eb;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            return eb;
        }

        if (audioPlayer.getPlayingTrack() == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is no song playing. I can't repeat nothing.");
            return eb;
        }

        if (audioPlayer.getPlayingTrack() == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is no song playing. I can't repeat nothing.");
            return eb;
        }

        return null;
    }

    public EmbedBuilder handleRepeat(GuildMusicManager musicManager, User looper) {
        final var guild = musicManager.getGuild();

        final var player = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();

        if (player.getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");

        EmbedBuilder eb;

        AudioTrackInfo info = player.getPlayingTrack().getInfo();
        if (scheduler.repeating) {
            scheduler.repeating = false;
            eb = RobertifyEmbedUtils.embedMessage(guild, "`" + info.title + "` will no longer be looped!");
        } else {
            scheduler.repeating = true;
            eb = RobertifyEmbedUtils.embedMessage(guild, "`" + info.title + "` will now be looped");
        }

        new LogUtils().sendLog(guild, LogType.TRACK_LOOP, looper.getAsMention() + " has "+(scheduler.repeating ? "looped" : "unlooped")+" `"+info.title+" by "+info.author+"`");

        return eb;
    }

    public EmbedBuilder handleQueueRepeat(GuildMusicManager musicManager, User looper) {
        EmbedBuilder eb;
        final var scheduler = musicManager.getScheduler();
        final var guild = musicManager.getGuild();
        final var audioPlayer = musicManager.getPlayer();

        if (scheduler.playlistRepeating) {
            scheduler.playlistRepeating = false;
            scheduler.removeSavedQueue(guild);
            eb = RobertifyEmbedUtils.embedMessage(guild, "The current queue will no longer be repeated!");
        } else {
            scheduler.playlistRepeating = true;

            if (audioPlayer.getPlayingTrack() == null) {
                eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing currently playing!");
                return eb;
            }

            AudioTrack thisTrack = audioPlayer.getPlayingTrack();

            if (scheduler.queue.isEmpty()) {
                eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing in the queue to repeat!\n");
                return eb;
            }

            scheduler.addToBeginningOfQueue(thisTrack);
            scheduler.setSavedQueue(guild, scheduler.queue);
            scheduler.queue.remove(thisTrack);
            eb = RobertifyEmbedUtils.embedMessage(guild, "The current queue will now be looped!");
        }

        new LogUtils().sendLog(guild, LogType.TRACK_LOOP, looper.getAsMention() + " has "+(scheduler.playlistRepeating ? "looped" : "unlooped")+" the queue");
        return eb;
    }

    @Override
    public String getName() {
        return "loop";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Set the song being currently played or the queue to constantly loop\n" +
                "\nUsage `" + prefix + "loop [queue]` *(Add `queue` to start repeating the current queue)*";
    }

    @Override
    public List<String> getAliases() {
        return List.of("l");
    }
}
