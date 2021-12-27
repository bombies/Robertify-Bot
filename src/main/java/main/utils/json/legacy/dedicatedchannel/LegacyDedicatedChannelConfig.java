package main.utils.json.legacy.dedicatedchannel;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.management.dedicatechannel.DedicatedChannelCommand;
import main.constants.BotConstants;
import main.constants.ENV;
import main.constants.JSONConfigFile;
import main.constants.RobertifyEmoji;
import main.main.Config;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.BotDB;
import main.utils.database.sqlite3.ServerDB;
import main.utils.json.legacy.AbstractJSONFile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class LegacyDedicatedChannelConfig extends AbstractJSONFile {
    public LegacyDedicatedChannelConfig() {
        super(JSONConfigFile.DEDICATED_CHANNELS);
    }

    @Override
    public void initConfig() {
        try {
            makeConfigFile();
        } catch (IllegalStateException e) {
            updateConfig();
            return;
        }

        final var obj = new JSONObject();
        for (Guild g : new BotDB().getGuilds())
            obj.put(g.getId(), "");
        setJSON(obj);
    }

    public synchronized void updateConfig() {
        var obj = getJSONObject();

        for (Guild g : new BotDB().getGuilds())
            if (!obj.has(g.getId()))
                obj.put(g.getId(), "");

        setJSON(obj);
    }

    public synchronized LegacyDedicatedChannelConfig setChannel(String gid, String cid) {
        var obj = getJSONObject();

        var guild = new JSONObject();
        guild.put(DedicatedChannelConfigField.CHANNEL_ID.toString(), cid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized LegacyDedicatedChannelConfig setMessage(String gid, String mid) {
        var obj = getJSONObject();

        var guild = obj.getJSONObject(gid);
        guild.put(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString(), mid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized LegacyDedicatedChannelConfig setChannelAndMessage(String gid, String cid, String mid) {
        var obj = getJSONObject();

        var guild = new JSONObject();
        guild.put(DedicatedChannelConfigField.CHANNEL_ID.toString(), cid);
        guild.put(DedicatedChannelConfigField.QUEUE_MESSAGE_ID.toString(), mid);
        obj.put(gid, guild);

        setJSON(obj);
        return this;
    }

    public synchronized LegacyDedicatedChannelConfig setOriginalAnnouncementToggle(String gid, boolean toggle) {
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

    public synchronized LegacyDedicatedChannelConfig removeChannel(String gid) {
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
        if (!isChannelSet(guild.getId()))
            return;

        final var msg = getMessage(guild.getId());
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.audioPlayer;
        final var playingTrack = audioPlayer.getPlayingTrack();
        final var queue = musicManager.scheduler.queue;
        final var queueAsList = new ArrayList<>(queue);

        EmbedBuilder eb = new EmbedBuilder();

        if (playingTrack == null) {
            eb.setColor(GeneralUtils.parseColor(Config.get(ENV.BOT_COLOR)));
            eb.setTitle("No song playing...");
            eb.setImage("https://i.imgur.com/1HDoSgP.png");
            eb.setFooter("Prefix for this server is: " + ServerDB.getPrefix(guild.getIdLong()));

            msg.editMessage("**__Queue:__**\nJoin a voice channel and start playing songs!")
                    .setEmbeds(eb.build()).queue();
        } else {
            final var trackInfo = playingTrack.getInfo();

            eb.setColor(GeneralUtils.parseColor(Config.get(ENV.BOT_COLOR)));
            eb.setTitle(trackInfo.title + " by " + trackInfo.author + " ["+ GeneralUtils.formatTime(playingTrack.getDuration()) +"]");

            var requester = RobertifyAudioManager.getRequester(playingTrack);
            if (requester != null)
                eb.setDescription("Requested by " + requester.getAsMention());

            eb.setImage(BotConstants.DEFAULT_IMAGE.toString());
            eb.setFooter(queueAsList.size() + " songs in queue | Volume: " + audioPlayer.getVolume() + "%");

            final StringBuilder nextTenSongs = new StringBuilder();
            nextTenSongs.append("```");
            if (queueAsList.size() > 10) {
                for (AudioTrack track : queueAsList.subList(0, 10))
                    nextTenSongs.append("→ ").append(track.getInfo().title)
                            .append(" - ").append(track.getInfo().author)
                            .append(" [").append(GeneralUtils.formatTime(track.getDuration()))
                            .append("]\n");
            } else {
                if (queue.size() == 0)
                    nextTenSongs.append("Songs in the queue will appear here.");
                else
                    for (AudioTrack track : queueAsList)
                        nextTenSongs.append("→ ").append(track.getInfo().title).append(" - ").append(track.getInfo().author)
                                .append(" [").append(GeneralUtils.formatTime(track.getDuration()))
                                .append("]\n");
            }
            nextTenSongs.append("```");

            msg.editMessage("**__Queue__**\n" + nextTenSongs)
                    .setEmbeds(eb.build())
                    .queue();
        }


    }

    public void updateButtons() {
        for (Guild g : new BotDB().getGuilds()) {
            if (!isChannelSet(g.getId())) continue;

            final var msg = getMessage(g.getId());

            buttonUpdateRequest(msg).queue();
        }
    }

    public synchronized void updateButtons(Guild g) {
            if (!isChannelSet(g.getId())) return;

            final var msg = getMessage(g.getId());

            buttonUpdateRequest(msg).queue();
    }

    public synchronized MessageAction buttonUpdateRequest(Message msg) {
        return msg.editMessageComponents(
                ActionRow.of(
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.PRIMARY, DedicatedChannelCommand.ButtonID.PREVIOUS.toString(), Emoji.fromMarkdown(RobertifyEmoji.PREVIOUS_EMOJI.toString())),
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.PRIMARY, DedicatedChannelCommand.ButtonID.REWIND.toString(), Emoji.fromMarkdown(RobertifyEmoji.REWIND_EMOJI.toString())),
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.PRIMARY, DedicatedChannelCommand.ButtonID.PLAY_AND_PAUSE.toString(), Emoji.fromMarkdown(RobertifyEmoji.PLAY_AND_PAUSE_EMOJI.toString())),
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.PRIMARY, DedicatedChannelCommand.ButtonID.STOP.toString(), Emoji.fromMarkdown(RobertifyEmoji.STOP_EMOJI.toString())),
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.PRIMARY, DedicatedChannelCommand.ButtonID.END.toString(), Emoji.fromMarkdown(RobertifyEmoji.END_EMOJI.toString()))
                ),
                ActionRow.of(
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.SECONDARY, DedicatedChannelCommand.ButtonID.LOOP.toString(), Emoji.fromMarkdown(RobertifyEmoji.LOOP_EMOJI.toString())),
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.SECONDARY, DedicatedChannelCommand.ButtonID.SHUFFLE.toString(), Emoji.fromMarkdown(RobertifyEmoji.SHUFFLE_EMOJI.toString())),
                        Button.of(ButtonStyle.DANGER, DedicatedChannelCommand.ButtonID.DISCONNECT.toString(), Emoji.fromMarkdown(RobertifyEmoji.QUIT_EMOJI.toString()))
                ));
    }

    public synchronized void updateTopic(Guild g) {
        if (!isChannelSet(g.getId())) return;

        final var channel = getTextChannel(g.getId());
        channelTopicUpdateRequest(channel).queue();
    }

    public void updateTopic() {
        for (Guild g : new BotDB().getGuilds()) {
            if (!isChannelSet(g.getId())) continue;

            final var channel = getTextChannel(g.getId());
            channelTopicUpdateRequest(channel).queue();
        }
    }

    public synchronized ChannelManager channelTopicUpdateRequest(TextChannel channel) {
        return channel.getManager().setTopic(
                        RobertifyEmoji.PREVIOUS_EMOJI + " Go to the previous song. " +
                        RobertifyEmoji.REWIND_EMOJI + " Rewind the song. " +
                        RobertifyEmoji.PLAY_AND_PAUSE_EMOJI + " Pause/Resume the song. " +
                        RobertifyEmoji.STOP_EMOJI + " Stop the song and clear the queue. " +
                        RobertifyEmoji.END_EMOJI + " Skip the song. " +
                        RobertifyEmoji.LOOP_EMOJI + " Loop the song. " +
                        RobertifyEmoji.SHUFFLE_EMOJI + " Shuffle the song. " +
                        RobertifyEmoji.QUIT_EMOJI + " Disconnect the bot "
        );
    }
}