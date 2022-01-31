package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;

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

        final var musicManager = RobertifyAudioManager.getInstance().getLavaLinkMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();

        if (audioPlayer.getPlayingTrack() == null) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "There is nothing playing");
            return eb;
        }

        if (audioPlayer.isPaused()) {
            audioPlayer.setPaused(false);
            musicManager.setForcePaused(false);
            eb = RobertifyEmbedUtils.embedMessage(guild, "You have resumed the music!");
        } else {
            audioPlayer.setPaused(true);
            musicManager.setForcePaused(true);
            eb = RobertifyEmbedUtils.embedMessage(guild, "You have paused the music!");
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
