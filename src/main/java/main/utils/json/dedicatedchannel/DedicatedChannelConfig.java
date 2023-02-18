package main.utils.json.dedicatedchannel;

import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import main.audiohandlers.RobertifyAudioManager;
import main.audiohandlers.TrackScheduler;
import main.commands.slashcommands.commands.management.dedicatedchannel.DedicatedChannelCommand;
import main.constants.RobertifyEmoji;
import main.main.Robertify;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.selectionmenu.StringSelectMenuOption;
import main.utils.component.interactions.selectionmenu.StringSelectionMenuBuilder;
import main.utils.database.mongodb.databases.GuildDB;
import main.utils.json.AbstractGuildConfig;
import main.utils.json.themes.ThemesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DedicatedChannelConfig extends AbstractGuildConfig {
    private final static Logger logger = LoggerFactory.getLogger(DedicatedChannelConfig.class);

    private final Guild guild;
    private final long gid;

    public DedicatedChannelConfig(Guild guild) {
        super(guild);
        this.guild = guild;
        this.gid = guild.getIdLong();
    }

    public synchronized void setMessage(long mid) {
        var obj = getGuildObject();

        var dediChannelObj = obj.getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        dediChannelObj.put(GuildDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString(), mid);

        getCache().setField(gid, GuildDB.Field.DEDICATED_CHANNEL_OBJECT, dediChannelObj);
    }

    public synchronized void setChannelAndMessage(long cid, long mid) {
        var obj = getGuildObject();

        var dediChannelObject = obj.getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        dediChannelObject.put(GuildDB.Field.DEDICATED_CHANNEL_ID.toString(), cid);
        dediChannelObject.put(GuildDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString(), mid);

        getCache().setField(gid, GuildDB.Field.DEDICATED_CHANNEL_OBJECT, dediChannelObject);
    }

    public synchronized void setOriginalAnnouncementToggle(boolean toggle) {
        var obj = getGuildObject();

        var dedicatedChannelObj = obj.getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        dedicatedChannelObj.put(DedicatedChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString(), toggle);

        getCache().setField(gid, GuildDB.Field.DEDICATED_CHANNEL_OBJECT, dedicatedChannelObj);
    }

    public synchronized boolean getOriginalAnnouncementToggle() {
        return getGuildObject().getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                .getBoolean(DedicatedChannelConfigField.ORIGINAL_ANNOUNCEMENT_TOGGLE.toString());
    }

    public synchronized void removeChannel() {
        if (!isChannelSet())
            throw new IllegalArgumentException(Robertify.shardManager.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");

        getTextChannel().delete().queue(null, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e -> {}));

        var obj = getGuildObject().getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        obj.put(GuildDB.Field.DEDICATED_CHANNEL_ID.toString(), -1);
        obj.put(GuildDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString(), -1);

        getCache().setField(gid, GuildDB.Field.DEDICATED_CHANNEL_OBJECT, obj);
    }

    public synchronized boolean isChannelSet() {
        try {
            return getGuildObject().getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                    .getLong(GuildDB.Field.DEDICATED_CHANNEL_ID.toString()) != -1;
        } catch (JSONException e) {
            if (e.getMessage().contains("is not a ")) {
                return !getGuildObject().getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                        .getString(GuildDB.Field.DEDICATED_CHANNEL_ID.toString()).equals("-1");
            } else throw e;
        }
    }

    public synchronized long getChannelID() {
        if (!isChannelSet())
            throw new IllegalArgumentException(Robertify.shardManager.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getGuildObject().getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                .getLong(GuildDB.Field.DEDICATED_CHANNEL_ID.toString());
    }

    public synchronized long getMessageID() {
        if (!isChannelSet())
            throw new IllegalArgumentException(Robertify.shardManager.getGuildById(gid).getName() + "("+gid+") doesn't have a channel set");
        return getGuildObject().getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString())
                .getLong(GuildDB.Field.DEDICATED_CHANNEL_MESSAGE_ID.toString());
    }

    public synchronized TextChannel getTextChannel() {
        return Robertify.shardManager.getTextChannelById(getChannelID());
    }

    public ChannelConfig getConfig() {
        return new ChannelConfig(this, gid);
    }

    public synchronized RestAction<Message> getMessageRequest() {
        try {
            return getTextChannel().retrieveMessageById(getMessageID());
        } catch (InsufficientPermissionException e) {
                TextChannel channel = RobertifyAudioManager.getInstance().getMusicManager(guild)
                        .getScheduler().getAnnouncementChannel();

                if (channel != null)
                    channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(channel.getGuild(), "I don't have access to the requests channel anymore! I cannot update it.").build())
                            .queue(null, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, ignored -> {}));
            return null;
        }
    }

    public synchronized void updateMessage() {
        if (!isChannelSet())
            return;

        CompletableFuture.runAsync(() -> {
            final var msgRequest = getMessageRequest();

            if (msgRequest == null) return;

            final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
            final var audioPlayer = musicManager.getPlayer();
            final var playingTrack = audioPlayer.getPlayingTrack();
            final var queue = musicManager.getScheduler().queue;
            final var queueAsList = new ArrayList<>(queue);
            final var theme = new ThemesConfig(guild).getTheme();
            final var localeManager = LocaleManager.getLocaleManager(guild);

            EmbedBuilder eb = new EmbedBuilder();

            if (playingTrack == null) {
                eb.setColor(theme.getColor());
                eb.setTitle(localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_NOTHING_PLAYING));
                eb.setImage(theme.getIdleBanner());

                msgRequest.queue(msg -> msg.editMessage(localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NOTHING_PLAYING))
                                .setEmbeds(eb.build())
                                .queue(
                                        null,
                                        new ErrorHandler()
                                                .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                                                    final var scheduler = musicManager.getScheduler();
                                                    final var announcementChannel = scheduler.getAnnouncementChannel();

                                                    if (announcementChannel != null)
                                                        announcementChannel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(
                                                                guild,
                                                                RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_SELF_INSUFFICIENT_PERMS_EDIT
                                                        ).build())
                                                                .queue(errMsg -> errMsg.delete().queueAfter(
                                                                        10,
                                                                        TimeUnit.SECONDS,
                                                                        null,
                                                                        new ErrorHandler()
                                                                                .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                                                                ));
                                                })
                                ),
                        new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> removeChannel())
                );
            } else {
                final var trackInfo = playingTrack.getInfo();

                eb.setColor(theme.getColor());

                eb.setTitle(
                        localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PLAYING_EMBED_TITLE,
                                Pair.of("{title}", trackInfo.title),
                                Pair.of("{author}", trackInfo.author),
                                Pair.of("{duration}", GeneralUtils.formatTime(playingTrack.getInfo().length))
                        )
                );

                var requester = RobertifyAudioManager.getRequester(guild, playingTrack);
                eb.setDescription(localeManager.getMessage(RobertifyLocaleMessage.NowPlayingMessages.NP_ANNOUNCEMENT_REQUESTER, Pair.of("{requester}", requester)));

                if (playingTrack instanceof MirroringAudioTrack mirroringAudioTrack)
                    eb.setImage(mirroringAudioTrack.getArtworkURL());
                else eb.setImage(theme.getNowPlayingBanner());

                eb.setFooter(localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_PLAYING_EMBED_FOOTER,
                        Pair.of("{numSongs}", String.valueOf(queueAsList.size())),
                        Pair.of("{volume}", String.valueOf((int)(audioPlayer.getFilters().getVolume() * 100)))
                ));

                final StringBuilder nextTenSongs = new StringBuilder();
                nextTenSongs.append("```");
                if (queueAsList.size() > 10) {
                    int index = 1;
                    for (AudioTrack track : queueAsList.subList(0, 10))
                        nextTenSongs.append(index++).append(". → ").append(track.getInfo().title)
                                .append(" - ").append(track.getInfo().author)
                                .append(" [").append(GeneralUtils.formatTime(track.getInfo().length))
                                .append("]\n");
                } else {
                    if (queue.size() == 0)
                        nextTenSongs.append(localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_NO_SONGS));
                    else {
                        int index = 1;
                        for (AudioTrack track : queueAsList)
                            nextTenSongs.append(index++).append(". → ").append(track.getInfo().title).append(" - ").append(track.getInfo().author)
                                    .append(" [").append(GeneralUtils.formatTime(track.getInfo().length))
                                    .append("]\n");
                    }
                }
                nextTenSongs.append("```");

                msgRequest.queue(msg -> msg.editMessage(localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_QUEUE_PLAYING, Pair.of("{songs}", nextTenSongs.toString())))
                                .setEmbeds(eb.build())
                                .queue()
                        , new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, e -> removeChannel())
                );
            }
        });
    }

    public void updateAll() {
        try {
            updateMessage();
            updateButtons();
            updateAllTopics();
        } catch (InsufficientPermissionException e) {
            logger.error("I didn't have enough permissions to update the dedicated channel in {}", guild.getName());
        }

    }

    public static void updateAllButtons() {
        for (Guild g : Robertify.shardManager.getGuilds()) {
            final var config = new DedicatedChannelConfig(g);
            if (!config.isChannelSet()) continue;

            final var msgRequest = config.getMessageRequest();
            if (msgRequest == null) continue;

            msgRequest.queue(msg -> config.buttonUpdateRequest(msg).queue(null, new ErrorHandler().handle(ErrorResponse.MISSING_PERMISSIONS, e -> logger.warn("Couldn't update buttons in {}", g.getName()))));
        }
    }

    public void updateButtons() {
        if (!isChannelSet()) return;

        final var msgRequest = getMessageRequest();
        if (msgRequest == null) return;

        msgRequest.queue(msg -> buttonUpdateRequest(msg).queue());
    }

    public MessageEditAction buttonUpdateRequest(Message msg) {
        final var config = getConfig();
        final var localeManager = LocaleManager.getLocaleManager(msg.getGuild());

        final var firstRow = ActionRow.of(ChannelConfig.Field.getFirstRow().stream()
                .filter(config::getState)
                .map(field -> Button.of(ButtonStyle.PRIMARY, field.getId(), field.getEmoji()))
                .toList());
        final var secondRow = ActionRow.of(ChannelConfig.Field.getSecondRow().stream()
                .filter(config::getState)
                .map(field -> Button.of(field.equals(ChannelConfig.Field.DISCONNECT) ? ButtonStyle.DANGER : ButtonStyle.SECONDARY, field.getId(), field.getEmoji()))
                .toList());
        final var thirdRow = ActionRow.of(StringSelectionMenuBuilder.of(
                ChannelConfig.Field.FILTERS.id,
                LocaleManager.getLocaleManager(msg.getGuild()).getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_SELECT_PLACEHOLDER),
                Pair.of(0,5),
                List.of(
                        StringSelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.EIGHT_D), ChannelConfig.Field.FILTERS.id + ":8d"),
                        StringSelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.KARAOKE), ChannelConfig.Field.FILTERS.id + ":karaoke"),
                        StringSelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.NIGHTCORE), ChannelConfig.Field.FILTERS.id + ":nightcore"),
                        StringSelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.TREMOLO), ChannelConfig.Field.FILTERS.id + ":tremolo"),
                        StringSelectMenuOption.of(localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.VIBRATO), ChannelConfig.Field.FILTERS.id + ":vibrato")
                )

        ).build());
        return  config.getState(ChannelConfig.Field.FILTERS) ? msg.editMessageComponents(firstRow, secondRow, thirdRow) : msg.editMessageComponents(firstRow, secondRow);
    }

    public void updateTopic() {
        if (!isChannelSet()) return;

        final var channel = getTextChannel();
        channelTopicUpdateRequest(channel).queue();
    }

    public static void updateAllTopics() {
        for (Guild g : Robertify.shardManager.getGuilds()) {
            final var config = new DedicatedChannelConfig(g);
            if (!config.isChannelSet()) continue;

            final var channel = config.getTextChannel();
            config.channelTopicUpdateRequest(channel).queue();
        }
    }

    public synchronized TextChannelManager channelTopicUpdateRequest(TextChannel channel) {
        final var localeManager = LocaleManager.getLocaleManager(channel.getGuild());
        return channel.getManager().setTopic(
                RobertifyEmoji.PREVIOUS_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_PREVIOUS) +
                        RobertifyEmoji.REWIND_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_REWIND) +
                        RobertifyEmoji.PLAY_AND_PAUSE_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_PLAY_AND_PAUSE) +
                        RobertifyEmoji.STOP_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_STOP) +
                        RobertifyEmoji.END_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_END) +
                        RobertifyEmoji.STAR_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_STAR) +
                        RobertifyEmoji.LOOP_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_LOOP) +
                        RobertifyEmoji.SHUFFLE_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_SHUFFLE) +
                        RobertifyEmoji.QUIT_EMOJI + " " + localeManager.getMessage(RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_TOPIC_QUIT)
        );
    }

    protected void updateConfig(long gid, JSONObject config) {
        getCache().setField(gid, GuildDB.Field.DEDICATED_CHANNEL_OBJECT, config);
    }

    public void cleanChannel() {
        if (!isChannelSet())
            return;
        if (!guild.getSelfMember().hasPermission(Permission.MESSAGE_HISTORY))
            return;
        final var channel = getTextChannel();
        MessageHistory.getHistoryAfter(channel, String.valueOf(getMessageID()))
                .queue(messages -> {
                    final var validMessages = messages.getRetrievedHistory()
                            .stream()
                            .filter(msg -> msg.getTimeCreated().toEpochSecond() > ((int)(new Date().getTime() / 1000)) - (14 * 24 * 60 * 60))
                            .toList();
                    if (validMessages.size() >= 2)
                        channel.deleteMessages(validMessages.subList(0, Math.min(validMessages.size(), 100))).queue();
                });
    }

    public static class ChannelConfig {
        private final DedicatedChannelConfig mainConfig;
        private final long gid;

        private ChannelConfig(DedicatedChannelConfig mainConfig, long gid) {
            this.mainConfig = mainConfig;
            this.gid = gid;
        }

        public boolean getState(Field field) {
            if (!hasField(field))
                initConfig();
            final var config = getConfig();
            return config.getBoolean(field.name().toLowerCase());
        }

        public void setState(Field field, boolean state) {
            if (!hasField(field))
                initConfig();
            final var config = getConfig();
            config.put(field.name().toLowerCase(), state);
            final var fullConfig = getFullConfig().put(GuildDB.Field.DEDICATED_CHANNEL_CONFIG.toString(), config);
            mainConfig.updateConfig(gid, fullConfig);
        }

        private void initConfig() {
            final var config = mainConfig.getGuildObject()
                    .getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString());

            if (!config.has(GuildDB.Field.DEDICATED_CHANNEL_CONFIG.toString()))
                config.put(GuildDB.Field.DEDICATED_CHANNEL_CONFIG.toString(), new JSONObject());

            final var configObj = config.getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_CONFIG.toString());
            for (final var field : Field.values()) {
                if (!configObj.has(field.name().toLowerCase()))
                    configObj.put(field.name().toLowerCase(), !field.equals(Field.FILTERS));
            }

            mainConfig.updateConfig(gid, config);
        }

        private boolean hasField(Field field) {
            return getConfig().has(field.name().toLowerCase());
        }

        private JSONObject getConfig() {
            var dedicatedChannelObj = getFullConfig();

            if (!dedicatedChannelObj.has(GuildDB.Field.DEDICATED_CHANNEL_CONFIG.toString())) {
                initConfig();
                dedicatedChannelObj = getFullConfig();
            }

            return dedicatedChannelObj.getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_CONFIG.toString());
        }

        private JSONObject getFullConfig() {
            return mainConfig.getGuildObject()
                    .getJSONObject(GuildDB.Field.DEDICATED_CHANNEL_OBJECT.toString());
        }

        public enum Field {
            PREVIOUS(DedicatedChannelCommand.ButtonID.PREVIOUS.toString(), Emoji.fromFormatted(RobertifyEmoji.PREVIOUS_EMOJI.toString())),
            REWIND(DedicatedChannelCommand.ButtonID.REWIND.toString(), Emoji.fromFormatted(RobertifyEmoji.REWIND_EMOJI.toString())),
            PLAY_PAUSE(DedicatedChannelCommand.ButtonID.PLAY_AND_PAUSE.toString(), Emoji.fromFormatted(RobertifyEmoji.PLAY_AND_PAUSE_EMOJI.toString())),
            STOP(DedicatedChannelCommand.ButtonID.STOP.toString(), Emoji.fromFormatted(RobertifyEmoji.STOP_EMOJI.toString())),
            SKIP(DedicatedChannelCommand.ButtonID.END.toString(), Emoji.fromFormatted(RobertifyEmoji.END_EMOJI.toString())),
            FAVOURITE(DedicatedChannelCommand.ButtonID.FAVOURITE.toString(), Emoji.fromFormatted(RobertifyEmoji.STAR_EMOJI.toString())),
            LOOP(DedicatedChannelCommand.ButtonID.LOOP.toString(), Emoji.fromFormatted(RobertifyEmoji.LOOP_EMOJI.toString())),
            SHUFFLE(DedicatedChannelCommand.ButtonID.SHUFFLE.toString(), Emoji.fromFormatted(RobertifyEmoji.SHUFFLE_EMOJI.toString())),
            DISCONNECT(DedicatedChannelCommand.ButtonID.DISCONNECT.toString(), Emoji.fromFormatted(RobertifyEmoji.QUIT_EMOJI.toString())),
            FILTERS("dedicatedfilters", Emoji.fromFormatted(RobertifyEmoji.FILTER_EMOJI.toString()));

            @Getter
            private final String id;
            @Getter
            private final Emoji emoji;

            Field(String id, Emoji emoji) {
                this.id = id;
                this.emoji = emoji;
            }

            public static List<Field> getFirstRow() {
                return List.of(PREVIOUS, REWIND, PLAY_PAUSE, STOP, SKIP);
            }

            public static List<Field> getSecondRow() {
                return List.of(FAVOURITE, LOOP, SHUFFLE, DISCONNECT);
            }

            public static List<Field> getFinalRow() {
                return List.of(FILTERS);
            }
        }
    }


    @Override
    public void update() {
        // Nothing
    }
}
