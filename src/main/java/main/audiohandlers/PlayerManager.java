package main.audiohandlers;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    private  final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;

    public PlayerManager() {
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();

        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    public GuildMusicManager getMusicManager(Guild guild) {
        return  this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildID) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager, guild);

            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void loadAndPlay(TextChannel channel, String trackUrl, GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        joinVoiceChannel(selfVoiceState, memberVoiceState, ctx);
        final GuildMusicManager musicManager = this.getMusicManager(channel.getGuild());

        this.audioPlayerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + audioTrack.getInfo().title
                        + "` by `" + audioTrack.getInfo().author + "`");
                channel.sendMessageEmbeds(eb.build()).queue();

//                joinVoiceChannel(selfVoiceState, memberVoiceState, ctx);

                musicManager.scheduler.queue(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                List<AudioTrack> tracks = audioPlaylist.getTracks();

                EmbedBuilder eb = EmbedUtils.embedMessage("🎼 Adding to queue: `" + tracks.size()
                + "` tracks from playlist `" + audioPlaylist.getName() + "`");
                channel.sendMessageEmbeds(eb.build()).queue();

//                joinVoiceChannel(selfVoiceState, memberVoiceState, ctx);

                for (final AudioTrack track : tracks)
                    musicManager.scheduler.queue(track);
            }

            @Override
            public void noMatches() {
                ctx.getGuild().getAudioManager().closeAudioConnection();
                channel.sendMessage("Nothing was found").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                ctx.getGuild().getAudioManager().closeAudioConnection();
                e.printStackTrace();
                channel.sendMessage("Error loading track").queue();
            }
        });
    }

    private void joinVoiceChannel(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, CommandContext ctx) {
        if (!selfVoiceState.inVoiceChannel()) {
            AudioManager audioManager = ctx.getGuild().getAudioManager();
            AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
            AudioSourceManagers.registerRemoteSources(audioPlayerManager);
            AudioPlayer player = audioPlayerManager.createPlayer();
            audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
            audioManager.openAudioConnection(memberVoiceState.getChannel());
        }
    }

    public static PlayerManager getInstance() {
        if (INSTANCE == null)
            INSTANCE = new PlayerManager();
        return INSTANCE;
    }
}
