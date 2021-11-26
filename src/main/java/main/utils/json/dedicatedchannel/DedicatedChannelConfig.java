package main.utils.json.dedicatedchannel;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.PlayerManager;
import main.constants.BotConstants;
import main.constants.ENV;
import main.constants.JSONConfigFile;
import main.main.Config;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import main.utils.database.ServerUtils;
import main.utils.json.JSONConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;

public class DedicatedChannelConfig extends JSONConfig {
    public DedicatedChannelConfig() {
        super(JSONConfigFile.DEDICATED_CHANNELS);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile(JSONConfigFile.DEDICATED_CHANNELS);
        } catch (IllegalStateException e) {
            updateConfig();
            return;
        }

        final var obj = new JSONObject();
        for (Guild g : new BotUtils().getGuilds())
            obj.put(g.getId(), "");
        setJSON(obj);
    }

    private synchronized void updateConfig() {
        var obj = getJSONObject();

        for (Guild g : new BotUtils().getGuilds())
            try {
                obj.getString(g.getId());
            } catch (JSONException e) {
                obj.put(g.getId(), "");
            }
    }

    public synchronized DedicatedChannelConfig setChannel(String gid, String cid) {
        var obj = getJSONObject();

        var guild = new JSONObject();
        guild.put(DedicatedChannelConfigField.CHANNEL_ID.toString(), cid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized DedicatedChannelConfig setMessage(String gid, String mid) {
        var obj = getJSONObject();

        var guild = obj.getJSONObject(gid);
        guild.put(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString(), mid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized DedicatedChannelConfig setChannelAndMessage(String gid, String cid, String mid) {
        var obj = getJSONObject();

        var guild = new JSONObject();
        guild.put(DedicatedChannelConfigField.CHANNEL_ID.toString(), cid);
        guild.put(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString(), mid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized DedicatedChannelConfig setOriginalAnnouncementToggle(String gid, boolean toggle) {
        var obj = getJSONObject();

        var guild = obj.getJSONObject(gid);
        guild.put(DedicatedChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString(), toggle);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized boolean getOriginalAnnouncementToggle(String gid) {
        return getJSONObject().getJSONObject(gid).getBoolean(DedicatedChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString());
    }

    public synchronized DedicatedChannelConfig removeChannel(String gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");

        var obj = getJSONObject();
        obj.remove(gid);

        setJSON(obj);
        return this;
    }

    public synchronized boolean isChannelSet(String gid) {
        try {
            getJSONObject().getJSONObject(gid);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    public synchronized String getChannelID(String gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getJSONObject().getJSONObject(gid).getString(DedicatedChannelConfigField.CHANNEL_ID.toString());
    }

    public synchronized String getMessageID(String gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getJSONObject().getJSONObject(gid).getString(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString());
    }

    public synchronized TextChannel getTextChannel(String gid) {
        return Robertify.api.getTextChannelById(getChannelID(gid));
    }

    public synchronized Message getMessage(String gid) {
        return getTextChannel(gid).retrieveMessageById(getMessageID(gid)).complete(); // DANGER
    }

    public synchronized void updateMessage(Guild guild) {
        final var msg = getMessage(guild.getId());
        final var musicManager = PlayerManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.audioPlayer;
        final var playingTrack = audioPlayer.getPlayingTrack();
        final var queue = musicManager.scheduler.queue;
        final var queueAsList = new ArrayList<>(queue);

        EmbedBuilder eb = new EmbedBuilder();

        if (playingTrack == null) {
            eb.setColor(GeneralUtils.parseColor(Config.get(ENV.BOT_COLOR)));
            eb.setTitle("No song playing...");
            eb.setImage("https://64.media.tumblr.com/9942a8261011606a2e78d75effad6220/c353caede4addfc4-52/s1280x1920/d085001a961ff09af5217c114c5cf0d7df7a63b9.png");
            eb.setFooter("Prefix for this server is: " + ServerUtils.getPrefix(guild.getIdLong()));

            msg.editMessage("**__Queue:__**\nJoin a voice channel and start playing songs!")
                    .setEmbeds(eb.build()).queue();
        } else {
            final var trackInfo = playingTrack.getInfo();

            eb.setColor(GeneralUtils.parseColor(Config.get(ENV.BOT_COLOR)));
            eb.setTitle(trackInfo.title + " by " + trackInfo.author + " ["+ GeneralUtils.formatTime(playingTrack.getDuration()) +"]");
            eb.setDescription("Requested by " + PlayerManager.getRequester(playingTrack).getAsMention());
            eb.setImage(BotConstants.DEFAULT_IMAGE.toString());
            eb.setFooter(queueAsList.size() + " songs in queue | Volume: " + audioPlayer.getVolume() + "%");

            final StringBuilder nextTenSongs = new StringBuilder();
            nextTenSongs.append("```");
            if (queueAsList.size() > 10) {
                for (AudioTrack track : queueAsList.subList(0, 10))
                    nextTenSongs.append("→ ").append(track.getInfo().title).append(" [").append(GeneralUtils.formatTime(track.getDuration()))
                            .append("]\n");
            } else {
                if (queue.size() == 0)
                    nextTenSongs.append("Songs in the queue will appear here.");
                else
                    for (AudioTrack track : queueAsList)
                        nextTenSongs.append("→ ").append(track.getInfo().title).append(" [").append(GeneralUtils.formatTime(track.getDuration()))
                                .append("]\n");
            }
            nextTenSongs.append("```");


            msg.editMessage("**__Queue__**\n" + nextTenSongs.toString())
                    .setEmbeds(eb.build())
                    .queue();
        }


    }
}
