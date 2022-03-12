package main.audiohandlers.loaders;

import lavalink.client.io.FriendlyException;
import lavalink.client.io.LoadResultHandler;
import lavalink.client.player.track.AudioPlaylist;
import lavalink.client.player.track.AudioTrack;
import lavalink.client.player.track.AudioTrackInfo;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.slashcommands.commands.audio.LofiCommand;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AudioLoader implements LoadResultHandler {
    private final Logger logger = LoggerFactory.getLogger(AudioLoader.class);

    private final Guild guild;
    private final User sender;
    private final GuildMusicManager musicManager;
    private final boolean announceMsg;
    private final HashMap<Long, List<String>> trackRequestedByUser;
    private final String trackUrl;
    private final Message botMsg;
    private final boolean loadPlaylistShuffled;
    private final boolean addToBeginning;
    private final TextChannel announcementChannel;

    public AudioLoader(User sender, GuildMusicManager musicManager, HashMap<Long, List<String>> trackRequestedByUser,
                       String trackUrl, boolean announceMsg, Message botMsg, boolean loadPlaylistShuffled, boolean addToBeginning) {

        this.guild = musicManager.getGuild();
        this.sender = sender;
        this.musicManager = musicManager;
        this.trackRequestedByUser = trackRequestedByUser;
        this.trackUrl = trackUrl;
        this.announceMsg = announceMsg;
        this.botMsg = botMsg;
        this.loadPlaylistShuffled = loadPlaylistShuffled;
        this.addToBeginning = addToBeginning;
        this.announcementChannel = botMsg != null ? botMsg.getTextChannel() : null;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        sendTrackLoadedMessage(audioTrack);

        if (!announceMsg)
            RobertifyAudioManager.getUnannouncedTracks().add(audioTrack.getTrack());


        trackRequestedByUser.putIfAbsent(guild.getIdLong(), new ArrayList<>());
        trackRequestedByUser.get(guild.getIdLong()).add(sender.getId() + ":" + audioTrack.getTrack());

        final var scheduler = musicManager.getScheduler();
        scheduler.setAnnouncementChannel(announcementChannel);

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(audioTrack);
        else
            scheduler.queue(audioTrack);

        AudioTrackInfo info = audioTrack.getInfo();
        new LogUtils().sendLog(guild, LogType.QUEUE_ADD, sender.getAsMention() + " has added `"+ info.getTitle() +" by "+ info.getAuthor() +"` to the queue.");

        if (scheduler.playlistRepeating)
            scheduler.setSavedQueue(guild, scheduler.queue);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

    }

    private void sendTrackLoadedMessage(AudioTrack audioTrack) {
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Added to queue: `" + audioTrack.getInfo().getTitle()
                + "` by `" + audioTrack.getInfo().getAuthor() + "`");

        if (botMsg != null) {
            if (LofiCommand.getLofiEnabledGuilds().contains(guild.getIdLong()) && LofiCommand.getAnnounceLofiMode().contains(guild.getIdLong())) {
                LofiCommand.getAnnounceLofiMode().remove(guild.getIdLong());
                botMsg.editMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You have enabled Lo-Fi mode").build())
                        .queue();
            } else {
                botMsg.editMessageEmbeds(eb.build())
                        .queue(success -> success.editMessageComponents().queue());
            }
        } else {
            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
                new DedicatedChannelConfig().getTextChannel(guild.getIdLong())
                        .sendMessageEmbeds(eb.build()).queue();
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        List<AudioTrack> tracks = audioPlaylist.getTracks();

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Added to queue: `" + tracks.size()
                + "` tracks from `" + audioPlaylist.getName() + "`");

        if (botMsg != null)
          botMsg.editMessageEmbeds(eb.build()).queue();
        else {
            new DedicatedChannelConfig().getTextChannel(guild.getIdLong())
                    .sendMessageEmbeds(eb.build()).queue();
        }

        if (!announceMsg)
            for (final AudioTrack track : tracks)
                RobertifyAudioManager.getUnannouncedTracks().add(track.getTrack());

        if (loadPlaylistShuffled)
            Collections.shuffle(tracks);

        final var scheduler = musicManager.getScheduler();
        scheduler.setAnnouncementChannel(announcementChannel);

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(tracks);

        trackRequestedByUser.putIfAbsent(guild.getIdLong(), new ArrayList<>());
        new LogUtils().sendLog(guild, LogType.QUEUE_ADD, sender.getAsMention() + " has added `"+audioPlaylist.getTracks().size()+"` songs from playlist `"+ audioPlaylist.getName() +"` to the queue.");
        for (final AudioTrack track : tracks) {
            trackRequestedByUser.get(guild.getIdLong()).add(sender.getId() + ":" + track.getTrack());

            if (!addToBeginning)
                scheduler.queue(track);
        }

        if (scheduler.playlistRepeating)
            scheduler.setSavedQueue(guild, scheduler.queue);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);
    }

    @Override
    public void searchResultLoaded(List<AudioTrack> list) {
        sendTrackLoadedMessage(list.get(0));

        if (!announceMsg)
            RobertifyAudioManager.getUnannouncedTracks().add(list.get(0).getTrack());

        trackRequestedByUser.putIfAbsent(guild.getIdLong(), new ArrayList<>());
        trackRequestedByUser.get(guild.getIdLong()).add(sender.getId() + ":" + list.get(0).getTrack());

        final var scheduler = musicManager.getScheduler();
        scheduler.setAnnouncementChannel(announcementChannel);

        if (addToBeginning)
            scheduler.addToBeginningOfQueue(list.get(0));
        else
            scheduler.queue(list.get(0));

        AudioTrackInfo info = list.get(0).getInfo();
        new LogUtils().sendLog(guild, LogType.QUEUE_ADD, sender.getAsMention() + " has added `"+ info.getTitle() +" by "+ info.getAuthor() +"` to the queue.");

        if (scheduler.playlistRepeating)
            scheduler.setSavedQueue(guild, scheduler.queue);

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);
    }

    @Override
    public void noMatches() {
        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "Nothing was found for `" + trackUrl.replace("ytsearch:", "")
                + "`. Try being more specific. *(Adding name of the artiste)*");
        if (botMsg != null)
            botMsg.editMessageEmbeds(eb.build()).queue();
        else {
            new DedicatedChannelConfig().getTextChannel(guild.getIdLong())
                    .sendMessageEmbeds(eb.build()).queue();
        }

        if (musicManager.getScheduler().queue.isEmpty())
            musicManager.getScheduler().scheduleDisconnect(false, 1, TimeUnit.SECONDS);
    }

    @Override
    public void loadFailed(FriendlyException e) {
            if (musicManager.getPlayer().getPlayingTrack() == null)
                musicManager.getGuild().getAudioManager().closeAudioConnection();

        if (!e.getMessage().contains("available") && !e.getMessage().contains("format"))
            logger.error("[FATAL ERROR] Could not load track!", e);

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild,
                e.getMessage().contains("available") ? e.getMessage()
                        : e.getMessage().contains("format") ? e.getMessage() :
                        "Error loading track"
        );
        if (botMsg != null)
            botMsg.editMessageEmbeds(eb.build()).queue();
        else {
            new DedicatedChannelConfig().getTextChannel(guild.getIdLong())
                    .sendMessageEmbeds(eb.build()).queue();
        }
    }
}
