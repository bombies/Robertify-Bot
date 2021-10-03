package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.commands.CommandContext;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private  final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return  this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildID) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(audioPlayerManager, guild);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void removeMusicManager(Guild guild) {
        musicManagers.remove(guild.getIdLong());
    }

    public void loadAndPlay(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        joinVoiceChannel(selfVoiceState, memberVoiceState, ctx);

        final GuildMusicManager musicManager = getMusicManager(channel.getGuild());

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + audioTrack.getInfo().title
                        + "` by `" + audioTrack.getInfo().author + "`");
                channel.sendMessageEmbeds(eb.build()).queue();

                musicManager.scheduler.queue(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                List<AudioTrack> tracks = audioPlaylist.getTracks();

                if (trackUrl.startsWith("ytsearch:")) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + tracks.get(0).getInfo().title
                            + "` by `" + tracks.get(0).getInfo().author + "`");
                    channel.sendMessageEmbeds(eb.build()).queue();

                    musicManager.scheduler.queue(tracks.get(0));
                    return;
                }

                EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + tracks.size()
                + "` tracks from playlist `" + audioPlaylist.getName() + "`");
                channel.sendMessageEmbeds(eb.build()).queue();

                for (final AudioTrack track : tracks)
                    musicManager.scheduler.queue(track);
            }

            @Override
            public void noMatches() {
                if (musicManager.audioPlayer.getPlayingTrack() == null)
                    ctx.getGuild().getAudioManager().closeAudioConnection();

                EmbedBuilder eb = EmbedUtils.embedMessage("Nothing was found for `"+ctx.getArgs().get(0)+"`. Trying being more specific. (Adding name of the artiste)");
                ctx.getMessage().replyEmbeds(eb.build()).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                if (musicManager.audioPlayer.getPlayingTrack() == null)
                    ctx.getGuild().getAudioManager().closeAudioConnection();
                e.printStackTrace();

                EmbedBuilder eb = EmbedUtils.embedMessage("Error loading track");
                ctx.getMessage().replyEmbeds(eb.build()).queue();
            }
        });
    }

    private void joinVoiceChannel(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        if (!selfVoiceState.inVoiceChannel()) {
            AudioManager audioManager = ctx.getGuild().getAudioManager();
            audioManager.openAudioConnection(memberVoiceState.getChannel());
            audioManager.setSelfDeafened(true);
        }
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new PlayerManager();
        return INSTANCE;
    }
}
