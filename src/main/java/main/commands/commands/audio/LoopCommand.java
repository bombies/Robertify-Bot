package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.lavaplayer.GuildMusicManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

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
            msg.replyEmbeds(handleRepeat(musicManager).build()).queue();
            return;
        }

        if (ctx.getArgs().get(0).equalsIgnoreCase("queue") || ctx.getArgs().get(0).equalsIgnoreCase("q")) {
            eb = handleQueueRepeat(musicManager, audioPlayer, ctx.getGuild());
        } else {
            eb = RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "Invalid arguments!");
        }
        msg.replyEmbeds(eb.build()).queue();
    }

    public EmbedBuilder checks(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, AudioPlayer audioPlayer) {
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
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command");
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

    public EmbedBuilder handleRepeat(GuildMusicManager musicManager) {
        final var guild = musicManager.getGuild();

        if (musicManager.getPlayer().getPlayingTrack() == null)
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");

        EmbedBuilder eb;

        if (musicManager.getScheduler().repeating) {
            musicManager.getScheduler().repeating = false;
            eb = RobertifyEmbedUtils.embedMessage(guild, "`" + musicManager.getPlayer().getPlayingTrack().getInfo().title + "` will no longer be looped!");
        } else {
            musicManager.getScheduler().repeating = true;
            eb = RobertifyEmbedUtils.embedMessage(guild, "`" + musicManager.getPlayer().getPlayingTrack().getInfo().title + "` will now be looped");
        }

        return eb;
    }

    public EmbedBuilder handleQueueRepeat(GuildMusicManager musicManager, AudioPlayer audioPlayer, Guild guild) {
        EmbedBuilder eb;
        final var scheduler = musicManager.getScheduler();

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

            AudioTrack thisTrack = audioPlayer.getPlayingTrack().makeClone();
            thisTrack.setPosition(0L);

            if (scheduler.queue.isEmpty()) {
                eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing in the queue to repeat!\n");
                return eb;
            }

            scheduler.addToBeginningOfQueue(thisTrack);
            scheduler.setSavedQueue(guild, musicManager.getScheduler().queue);
            scheduler.queue.remove(thisTrack);
            eb = RobertifyEmbedUtils.embedMessage(guild, "The current queue will now be looped!");
        }

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
