package main.commands.prefixcommands.audio;

import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.GuildMusicManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.commands.slashcommands.commands.audio.LofiCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.script.ScriptException;
@Deprecated @ForRemoval
public class StopCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfVoiceState = self.getVoiceState();
        final Guild guild = ctx.getGuild();
        final EmbedBuilder eb;
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!selfVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());

        msg.replyEmbeds(handleStop(ctx.getAuthor(), musicManager).build()).queue();
    }

    public EmbedBuilder handleStop(User stopper, GuildMusicManager musicManager) {
        final var audioPlayer = musicManager.getPlayer();
        final var scheduler = musicManager.getScheduler();
        final var guild = musicManager.getGuild();
        final AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null)
            return RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing!");


        scheduler.repeating = false;
        scheduler.playlistRepeating = false;
        audioPlayer.stopTrack();
        scheduler.queue.clear();
        scheduler.getPastQueue().clear();

        if (audioPlayer.isPaused())
            audioPlayer.setPaused(false);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        LofiCommand.getLofiEnabledGuilds().remove(guild.getIdLong());

        new LogUtils().sendLog(guild, LogType.PLAYER_STOP, stopper.getAsMention() + " has stopped the player");
        return RobertifyEmbedUtils.embedMessage(guild, "You have stopped the track and cleared the queue.");
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
