package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.database.sqlite3.ServerDB;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
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
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var scheduler = musicManager.scheduler;
        final var queue = scheduler.queue;
        final var previouslyPlayedTracks = scheduler.getPastQueue();
        final var audioPlayer = musicManager.audioPlayer;
        final var selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel())
            return EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command!");

        if (!selfVoiceState.inVoiceChannel())
            return EmbedUtils.embedMessage("I must be in a voice channel to execute this command!");

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command!");

        if (previouslyPlayedTracks.size() == 0)
            return EmbedUtils.embedMessage("There are no tracks played previously");

        if (audioPlayer.getPlayingTrack() != null) {
            final var nowPlayingTrack = audioPlayer.getPlayingTrack();
            audioPlayer.stopTrack();
            nowPlayingTrack.setPosition(0);
            scheduler.addToBeginningOfQueue(nowPlayingTrack);
            audioPlayer.playTrack(previouslyPlayedTracks.pop());
        } else {
            audioPlayer.playTrack(previouslyPlayedTracks.pop());
        }

        if (new DedicatedChannelConfig().isChannelSet(musicManager.scheduler.getGuild().getId()))
            new DedicatedChannelConfig().updateMessage(musicManager.scheduler.getGuild());

        return EmbedUtils.embedMessage("Now playing the previous track!");
    }

    @Override
    public String getName() {
        return "previous";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Go back to he track that was played previously\n\n" +
                "**Usage**: `"+ ServerDB.getPrefix(Long.parseLong(guildID)) +"previous`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("prev");
    }
}
