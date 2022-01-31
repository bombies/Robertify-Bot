package main.utils.json.dedicatedchannel;

import lavalink.client.player.track.AudioTrack;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.commands.audio.LofiCommand;
import main.commands.commands.management.dedicatechannel.DedicatedChannelCommand;
import main.constants.RobertifyEmoji;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.database.mongodb.databases.GuildsDB;
import main.utils.json.AbstractGuildConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.spotify.SpotifyUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.MissingAccessException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.managers.ChannelManager;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;

public class DedicatedChannelConfig extends AbstractGuildConfig {

    public synchronized void setMessage(long gid, long mid) {
        var obj = getGuildObject(gid);

        var dediChannelObj = obj.getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        dediChannelObj.put(GuildsDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString(), mid);

        getCache().setField(gid, GuildsDB.Field.DEDICATED_CHANNEL_OBJECT, dediChannelObj);
    }

    public synchronized void setChannelAndMessage(long gid, long cid, long mid) {
        var obj = getGuildObject(gid);

        var dediChannelObject = obj.getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        dediChannelObject.put(GuildsDB.Field.DEDICATED_CHANNEL_ID.toString(), cid);
        dediChannelObject.put(GuildsDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString(), mid);

        getCache().setField(gid, GuildsDB.Field.DEDICATED_CHANNEL_OBJECT, dediChannelObject);
    }

    public synchronized void setOriginalAnnouncementToggle(long gid, boolean toggle) {
        var obj = getGuildObject(gid);

        var dedicatedChannelObj = obj.getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        dedicatedChannelObj.put(DedicatedChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString(), toggle);

        getCache().setField(gid, GuildsDB.Field.DEDICATED_CHANNEL_OBJECT, dedicatedChannelObj);
    }

    public synchronized boolean getOriginalAnnouncementToggle(long gid) {
        return getGuildObject(gid).getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                .getBoolean(DedicatedChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString());
    }

    public synchronized void removeChannel(long gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");

        var obj = getGuildObject(gid).getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        obj.put(GuildsDB.Field.DEDICATED_CHANNEL_ID.toString(), -1);
        obj.put(GuildsDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString(), -1);

        getCache().setField(gid, GuildsDB.Field.DEDICATED_CHANNEL_OBJECT, obj);
    }

    public synchronized boolean isChannelSet(long gid) {
        return getGuildObject(gid).getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                .getLong(GuildsDB.Field.DEDICATED_CHANNEL_ID.toString()) != -1;
    }

    public synchronized long getChannelID(long gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getGuildObject(gid).getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                .getLong(GuildsDB.Field.DEDICATED_CHANNEL_ID.toString());
    }

    public synchronized long getMessageID(long gid) {
        if (!isChannelSet(gid))
            throw new IllegalArgumentException(Robertify.api.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getGuildObject(gid).getJSONObject(GuildsDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                .getLong(GuildsDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString());
    }

    public synchronized TextChannel getTextChannel(long gid) {
        return Robertify.api.getTextChannelById(getChannelID(gid));
    }

    public synchronized RestAction<Message> getMessageRequest(long gid) {
        try {
            return getTextChannel(gid).retrieveMessageById(getMessageID(gid));
        } catch (MissingAccessException e) {
            if (new GuildConfig().announcementChannelIsSet(gid)) {
                TextChannel channel = Robertify.api.getTextChannelById(new GuildConfig().getAnnouncementChannelID(gid));
                channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(channel.getGuild(), "I don't have access to the requests channel anymore! I cannot update it.").build())
                        .queue(null, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, ignored -> {}));
            }
            return null;
        }
    }

    public synchronized void updateMessage(Guild guild) {
        if (!isChannelSet(guild.getIdLong()))
            return;

        final var msgRequest = getMessageRequest(guild.getIdLong());

        if (msgRequest == null) return;

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var playingTrack = audioPlayer.getPlayingTrack();
        final var queue = musicManager.getScheduler().queue;
        final var queueAsList = new ArrayList<>(queue);
        final var theme = new ThemesConfig().getTheme(guild.getIdLong());

        EmbedBuilder eb = new EmbedBuilder();

        if (playingTrack == null) {
            eb.setColor(theme.getColor());
            eb.setTitle("No song playing...");
            eb.setImage(theme.getIdleBanner());
            eb.setFooter("Prefix for this server is: " + new GuildConfig().getPrefix(guild.getIdLong()));

            msgRequest.queue(msg -> msg.editMessage("**__Queue:__**\nJoin a voice channel and start playing songs!")
                    .setEmbeds(eb.build()).queue());
        } else {
            final var trackInfo = playingTrack.getInfo();

            eb.setColor(theme.getColor());

            eb.setTitle(
                    LofiCommand.getLofiEnabledGuilds().contains(guild.getIdLong()) ? "Lo-Fi Music"
                            :
                    trackInfo.getTitle() + " by " + trackInfo.getAuthor() + " ["+ GeneralUtils.formatTime(playingTrack.getInfo().getLength()) +"]"
            );

            var requester = RobertifyAudioManager.getRequester(guild, playingTrack);
            eb.setDescription("Requested by " + requester);

            if (trackInfo.getSourceName().equals("spotify"))
                eb.setImage(SpotifyUtils.getArtworkUrl(trackInfo.getIdentifier()));
            else
                eb.setImage(theme.getNowPlayingBanner());

            eb.setFooter(queueAsList.size() + " songs in queue | Volume: " + (int)(audioPlayer.getFilters().getVolume() * 100) + "%");

            final StringBuilder nextTenSongs = new StringBuilder();
            nextTenSongs.append("```");
            if (queueAsList.size() > 10) {
                int index = 1;
                for (AudioTrack track : queueAsList.subList(0, 10))
                    nextTenSongs.append(index++).append(". → ").append(track.getInfo().getTitle())
                            .append(" - ").append(track.getInfo().getAuthor())
                            .append(" [").append(GeneralUtils.formatTime(track.getInfo().getLength()))
                            .append("]\n");
            } else {
                if (queue.size() == 0)
                    nextTenSongs.append("Songs in the queue will appear here.");
                else {
                    int index = 1;
                    for (AudioTrack track : queueAsList)
                        nextTenSongs.append(index++).append(". → ").append(track.getInfo().getTitle()).append(" - ").append(track.getInfo().getAuthor())
                                .append(" [").append(GeneralUtils.formatTime(track.getInfo().getLength()))
                                .append("]\n");
                }
            }
            nextTenSongs.append("```");

            msgRequest.queue(msg -> msg.editMessage("**__Queue__**\n" + nextTenSongs)
                    .setEmbeds(eb.build())
                    .queue());
        }
    }

    public void updateButtons() {
        for (Guild g : Robertify.api.getGuilds()) {
            if (!isChannelSet(g.getIdLong())) continue;

            final var msgRequest = getMessageRequest(g.getIdLong());
            if (msgRequest == null) continue;

            msgRequest.queue(msg -> buttonUpdateRequest(msg).queue());
        }
    }

    public synchronized void updateButtons(Guild g) {
        if (!isChannelSet(g.getIdLong())) return;

        final var msgRequest = getMessageRequest(g.getIdLong());
        if (msgRequest == null) return;

        msgRequest.queue(msg -> buttonUpdateRequest(msg).queue());
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
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.SECONDARY, DedicatedChannelCommand.ButtonID.FAVOURITE.toString(), Emoji.fromMarkdown("⭐")),
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.SECONDARY, DedicatedChannelCommand.ButtonID.LOOP.toString(), Emoji.fromMarkdown(RobertifyEmoji.LOOP_EMOJI.toString())),
                        net.dv8tion.jda.api.interactions.components.Button.of(ButtonStyle.SECONDARY, DedicatedChannelCommand.ButtonID.SHUFFLE.toString(), Emoji.fromMarkdown(RobertifyEmoji.SHUFFLE_EMOJI.toString())),
                        Button.of(ButtonStyle.DANGER, DedicatedChannelCommand.ButtonID.DISCONNECT.toString(), Emoji.fromMarkdown(RobertifyEmoji.QUIT_EMOJI.toString()))
                ));
    }

    public synchronized void updateTopic(Guild g) {
        if (!isChannelSet(g.getIdLong())) return;

        final var channel = getTextChannel(g.getIdLong());
        channelTopicUpdateRequest(channel).queue();
    }

    public void updateTopic() {
        for (Guild g : Robertify.api.getGuilds()) {
            if (!isChannelSet(g.getIdLong())) continue;

            final var channel = getTextChannel(g.getIdLong());
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


    @Override
    public void update() {
        // Nothing
    }
}
