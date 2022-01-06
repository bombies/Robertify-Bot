package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.lavalink.LavaLinkGuildMusicManager;
import main.audiohandlers.lavaplayer.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;

public class StopCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final EmbedBuilder eb;
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!selfVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());

        msg.replyEmbeds(handleStop(musicManager).build()).queue();
    }

    public EmbedBuilder handleStop(GuildMusicManager musicManager) {
        final var audioPlayer = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();
        final AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null)
            return EmbedUtils.embedMessage("There is nothing playing!");


        audioPlayer.stopTrack();
        scheduler.queue.clear();
        scheduler.getPastQueue().clear();
        scheduler.repeating = false;
        scheduler.playlistRepeating = false;

        if (audioPlayer.isPaused())
            audioPlayer.setPaused(false);

        if (new DedicatedChannelConfig().isChannelSet(musicManager.getGuild().getIdLong()))
            new DedicatedChannelConfig().updateMessage(musicManager.getGuild());

        LofiCommand.getLofiEnabledGuilds().remove(musicManager.getGuild().getIdLong());

        return EmbedUtils.embedMessage("You have stopped the track and cleared the queue.");
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
}
