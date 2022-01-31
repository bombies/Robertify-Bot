package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class PreviousTrackCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();
        final Member member = ctx.getMember();

        msg.replyEmbeds(handlePrevious(guild, member.getVoiceState()).build())
                .queue();
    }

    public EmbedBuilder handlePrevious(Guild guild, GuildVoiceState memberVoiceState) {
        final var musicManager = RobertifyAudioManager.getInstance().getLavaLinkMusicManager(guild);
        final var scheduler = musicManager.getScheduler();
        final var previouslyPlayedTracks = scheduler.getPastQueue();
        final var audioPlayer = musicManager.getPlayer();
        final var selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "I must be in a voice channel to execute this command!");

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention());

        if (previouslyPlayedTracks.size() == 0)
            return RobertifyEmbedUtils.embedMessage(guild, "There are no tracks played previously");

        if (audioPlayer.getPlayingTrack() != null) {
            final var nowPlayingTrack = audioPlayer.getPlayingTrack();
            audioPlayer.stopTrack();
            nowPlayingTrack.setPosition(0);
            scheduler.addToBeginningOfQueue(nowPlayingTrack);
        }

        audioPlayer.playTrack(previouslyPlayedTracks.pop());

        if (new DedicatedChannelConfig().isChannelSet(musicManager.getGuild().getIdLong()))
            new DedicatedChannelConfig().updateMessage(musicManager.getGuild());

        return RobertifyEmbedUtils.embedMessage(guild, "Now playing the previous track!");
    }

    @Override
    public String getName() {
        return "previous";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Go back to he track that was played previously\n\n" +
                "**Usage**: `"+ prefix +"previous`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("prev");
    }
}
