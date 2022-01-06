package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lavalink.client.player.IPlayer;
import main.audiohandlers.lavalink.LavaLinkGuildMusicManager;
import main.audiohandlers.lavaplayer.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
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
            eb = EmbedUtils.embedMessage("Invalid arguments!");
        }
        msg.replyEmbeds(eb.build()).queue();
    }

    public EmbedBuilder checks(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, AudioPlayer audioPlayer) {
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

        if (audioPlayer.getPlayingTrack() == null) {
            eb = EmbedUtils.embedMessage("There is no song playing. I can't repeat nothing.");
            return eb;
        }

        if (audioPlayer.getPlayingTrack() == null) {
            eb = EmbedUtils.embedMessage("There is no song playing. I can't repeat nothing.");
            return eb;
        }

        return null;
    }

    public EmbedBuilder handleRepeat(GuildMusicManager musicManager) {
        if (musicManager.getPlayer().getPlayingTrack() == null)
            return EmbedUtils.embedMessage("There is nothing playing!");

        EmbedBuilder eb;

        if (musicManager.getScheduler().repeating) {
            musicManager.getScheduler().repeating = false;
            eb = EmbedUtils.embedMessage("`" + musicManager.getPlayer().getPlayingTrack().getInfo().title + "` will no longer be looped!");
        } else {
            musicManager.getScheduler().repeating = true;
            eb = EmbedUtils.embedMessage("`" + musicManager.getPlayer().getPlayingTrack().getInfo().title + "` will now be looped");
        }

        return eb;
    }

    public EmbedBuilder handleQueueRepeat(GuildMusicManager musicManager, AudioPlayer audioPlayer, Guild guild) {
        EmbedBuilder eb;
        final var scheduler = musicManager.getScheduler();


        if (scheduler.playlistRepeating) {
            scheduler.playlistRepeating = false;
            scheduler.removeSavedQueue(guild);
            eb = EmbedUtils.embedMessage("The current queue will no longer be repeated!");
        } else {
            scheduler.playlistRepeating = true;

            if (audioPlayer.getPlayingTrack() == null) {
                eb = EmbedUtils.embedMessage("There is nothing currently playing!");
                return eb;
            }

            AudioTrack thisTrack = audioPlayer.getPlayingTrack().makeClone();
            thisTrack.setPosition(0L);

            if (scheduler.queue.isEmpty()) {
                eb = EmbedUtils.embedMessage("There is nothing in the queue to repeat!\n");
                return eb;
            }

            scheduler.addToBeginningOfQueue(thisTrack);
            scheduler.setSavedQueue(guild, musicManager.getScheduler().queue);
            scheduler.queue.remove(thisTrack);
            eb = EmbedUtils.embedMessage("The current queue will now be looped!");
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
                "Set the song being replayed\n" +
                "\nUsage `" + prefix + "loop [queue]` *(Add `queue` to start repeating the current queue)*";
    }

    @Override
    public List<String> getAliases() {
        return List.of("l");
    }
}
