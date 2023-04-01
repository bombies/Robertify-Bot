package main.commands.slashcommands.commands.management.requestchannel;

import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import lavalink.client.io.filters.*;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.audio.*;
import main.commands.slashcommands.commands.audio.*;
import main.constants.ENV;
import main.constants.Permission;
import main.constants.SourcePlaylistPatterns;
import main.constants.Toggles;
import main.main.Config;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RequestChannelEvents extends ListenerAdapter {

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (!event.isFromType(ChannelType.TEXT))
            return;

        final Guild guild = event.getGuild();
        final RequestChannelConfig config = new RequestChannelConfig(guild);

        if (!config.isChannelSet()) return;
        if (config.getChannelID() != event.getChannel().getIdLong()) return;

        config.removeChannel();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        final Guild guild = event.getGuild();
        final RequestChannelConfig config = new RequestChannelConfig(guild);

        try {
            if (!config.isChannelSet()) return;
        } catch (NullPointerException ignored) {
            return;
        }

        if (config.getChannelID() != event.getChannel().getIdLong()) return;

        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final User user = event.getAuthor();

        Message eventMessage = event.getMessage();
        String message = eventMessage.getContentRaw();

        if (!message.startsWith(new GuildConfig(guild).getPrefix()) && !user.isBot() && !event.isWebhookMessage()) {
            if (!Config.getBoolean(ENV.MESSAGE_CONTENT_INTENT_ENABLED)) {
                event.getMessage().reply(user.getAsMention()).setEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.DedicatedChannelMessages.DEDICATED_CHANNEL_NO_CONTENT_INTENT)
                        .build()
                ).queue();
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                return;
            }

            if (!memberVoiceState.inAudioChannel()) {
                event.getMessage().reply(user.getAsMention()).setEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
                                .build())
                        .queue();
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                return;
            }

            if (selfVoiceState.inAudioChannel()) {
                if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                    event.getMessage().reply(user.getAsMention()).setEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                                    .build())
                            .queue();
                    event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                    return;
                }
            } else {
                if (TogglesConfig.getConfig(guild).getToggle(Toggles.RESTRICTED_VOICE_CHANNELS)) {
                    final var restrictedChannelsConfig = new RestrictedChannelsConfig(guild);
                    final var localeManager = LocaleManager.getLocaleManager(guild);
                    if (!restrictedChannelsConfig.isRestrictedChannel(memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                        event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.CANT_JOIN_CHANNEL) +
                                        (!restrictedChannelsConfig.getRestrictedChannels(
                                                RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                        ).isEmpty()
                                                ?
                                                localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.RESTRICTED_TO_JOIN, Pair.of("{channels}", restrictedChannelsConfig.restrictedChannelsToString(
                                                        RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                                )))
                                                :
                                                localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NO_VOICE_CHANNEL)
                                        )
                                ).build())
                                .queue();
                        event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                        return;
                    }
                }
            }

            if (!eventMessage.getAttachments().isEmpty()) {
                var audioFile = eventMessage.getAttachments().get(0);
                new PlayCommand().playLocalAudio(guild, event.getChannel().asGuildMessageChannel(), eventMessage, event.getMember(), audioFile);

                event.getMessage().delete().queueAfter(2, TimeUnit.SECONDS, null, new ErrorHandler()
                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                );

                return;
            }

            boolean addToBeginning = false;
            boolean shuffled = false;
            final var split = message.split(" ");

            if (split.length == 1) {
                message = (GeneralUtils.isUrl(message) ? "" : SpotifySourceManager.SEARCH_PREFIX)  + message;
            } else {
                switch (split[split.length-1].toLowerCase()) {
                    case "-n", "-next" -> {
                        String msgNoFlags = message.replaceAll("\\s-(n|next)$", "");
                        message = GeneralUtils.isUrl(msgNoFlags) ? msgNoFlags : SpotifySourceManager.SEARCH_PREFIX + msgNoFlags;
                        addToBeginning = true;
                    }
                    case "-s", "-shuffle" -> {
                        var msgNoFlags = message.replaceAll("\\s-(s|shuffle)$", "");

                        if (!GeneralUtils.isUrl(msgNoFlags)) {
                            event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.MUST_PROVIDE_VALID_PLAYLIST).build())
                                    .queue();
                            event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                                return;
                        }

                        if (!SourcePlaylistPatterns.isPlaylistLink(msgNoFlags)) {
                            event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.MUST_PROVIDE_VALID_PLAYLIST).build())
                                    .queue();
                            event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                            return;
                        }

                        message = msgNoFlags;
                        shuffled = true;
                    }
                    default -> message = GeneralUtils.isUrl(message) ? "" : SpotifySourceManager.SEARCH_PREFIX + message;
                }
            }

            if (addToBeginning)
                RobertifyAudioManager.getInstance()
                        .loadAndPlayFromDedicatedChannel(event.getChannel().asGuildMessageChannel(), message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(), null, true);
            else if (shuffled) {
                RobertifyAudioManager.getInstance()
                        .loadAndPlayFromDedicatedChannelShuffled(event.getChannel().asGuildMessageChannel(), message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(), null, false);
            } else {
                RobertifyAudioManager.getInstance()
                        .loadAndPlayFromDedicatedChannel(event.getChannel().asGuildMessageChannel(), message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(), null, false);
            }
        }

        if (event.getAuthor().isBot()) {
            if (!event.getMessage().isEphemeral())
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS, null, new ErrorHandler()
                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                );
        } else
            event.getMessage().delete().queueAfter(2, TimeUnit.SECONDS, null, new ErrorHandler()
                    .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
            );
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getButton().getId().startsWith(RequestChannelCommand.ButtonID.IDENTIFIER.toString()))
            return;

        final var guild = event.getGuild();
        final RequestChannelConfig config = new RequestChannelConfig(guild);

        if (!config.isChannelSet()) return;
        if (event.getChannel().asGuildMessageChannel().getIdLong() != config.getChannelID()) return;

        final String id = event.getButton().getId();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final Member member = event.getMember();

        if (!selfVoiceState.inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!memberVoiceState.inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                    .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (id.equals(RequestChannelCommand.ButtonID.REWIND.toString())) {
            if (!djCheck(new RewindSlashCommand(), guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            EmbedBuilder rewindEmbed = new RewindCommand().handleRewind(member.getUser(), selfVoiceState, 0, true);
            event.replyEmbeds(rewindEmbed.build())
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.PLAY_AND_PAUSE.toString())) {
            if (!djCheck(new PauseSlashCommand(), guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            EmbedBuilder playPauseEmbed = new PauseCommand().handlePauseEvent(event.getGuild(), selfVoiceState, memberVoiceState);
            event.replyEmbeds(playPauseEmbed.build())
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.END.toString())) {
            if (!djCheck(new SkipSlashCommand(), guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            MessageEmbed skipEmbed = new SkipCommand().handleSkip(selfVoiceState, memberVoiceState);
            event.replyEmbeds(skipEmbed)
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.LOOP.toString())) {
            final LoopSlashCommand loopCommand = new LoopSlashCommand();
            if (!djCheck(loopCommand, guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            final var scheduler = musicManager.getScheduler();
            final var info = musicManager.getPlayer().getPlayingTrack().getInfo();
            final EmbedBuilder loopEmbed;
            if (scheduler.isRepeating()) {
                scheduler.setRepeating(false);
                scheduler.setPlaylistRepeating(true);
                loopEmbed = RobertifyEmbedUtils.embedMessage(guild,
                        RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_START
                );

                new LogUtils(guild).sendLog(
                        LogType.QUEUE_LOOP, RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_LOG,
                        Pair.of("{user}", member.getAsMention()),
                        Pair.of("{status}", "looped")
                );
            } else if (scheduler.isPlaylistRepeating()) {
                scheduler.setPlaylistRepeating(false);
                scheduler.setRepeating(false);
                loopEmbed = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_STOP);

                new LogUtils(guild).sendLog(
                        LogType.QUEUE_LOOP, RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_LOG,
                        Pair.of("{user}", member.getAsMention()),
                        Pair.of("{status}", "unlooped")
                );
            } else {
                scheduler.setRepeating(true);
                scheduler.setPlaylistRepeating(false);
                loopEmbed = RobertifyEmbedUtils.embedMessage(
                        guild,
                        RobertifyLocaleMessage.LoopMessages.LOOP_START,
                        Pair.of("{title}", info.title)
                );

                new LogUtils(guild).sendLog(
                        LogType.TRACK_LOOP, RobertifyLocaleMessage.LoopMessages.LOOP_LOG,
                        Pair.of("{user}", member.getAsMention()),
                        Pair.of("{status}", "looped"),
                        Pair.of("{title}", info.title),
                        Pair.of("{author}", info.author)

                );
            }

            event.replyEmbeds(loopEmbed.build())
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.SHUFFLE.toString())) {
            if (!djCheck(new ShuffleSlashCommand(), guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            EmbedBuilder shuffleEmbed = new ShuffleCommand().handleShuffle(event.getGuild(), member.getUser());
            event.replyEmbeds(shuffleEmbed.build())
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.DISCONNECT.toString())) {
            if (!djCheck(new DisconnectSlashCommand(), guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            EmbedBuilder disconnectEmbed = new DisconnectCommand().handleDisconnect(event.getGuild(), event.getUser());
            event.replyEmbeds(disconnectEmbed.build())
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.STOP.toString())) {
            if (!djCheck(new StopCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            EmbedBuilder stopEmbed = new StopCommand().handleStop(event.getMember());
            event.replyEmbeds(stopEmbed.build())
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.PREVIOUS.toString())) {
            if (!GeneralUtils.checkPremium(guild, event))
                return;

            if (!djCheck(new PreviousTrackCommand(), guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            EmbedBuilder previousEmbed = new PreviousTrackCommand().handlePrevious(event.getGuild(), memberVoiceState);
            event.replyEmbeds(previousEmbed.build())
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(RequestChannelCommand.ButtonID.FAVOURITE.toString())) {
            if (!GeneralUtils.checkPremium(guild, event))
                return;

            if (!djCheck(new FavouriteTracksCommand(), guild, member)) {
                event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            event.replyEmbeds(new FavouriteTracksCommand().handleAdd(event.getGuild(), event.getMember()))
                    .setEphemeral(true)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getComponentId().startsWith(RequestChannelConfig.ChannelConfig.Field.FILTERS.getId()))
            return;

        final var guild = event.getGuild();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var filters = audioPlayer.getFilters();
        final var member = event.getMember();
        final var selfMember = guild.getSelfMember();
        final var localeManager = LocaleManager.getLocaleManager(guild);
        final var eventSelectedOptions = event.getSelectedOptions();
        final var selectedOptions = eventSelectedOptions != null ? eventSelectedOptions.stream().map(SelectOption::getValue).toList() : List.of();

        if (!GeneralUtils.checkPremium(guild, event))
            return;

        if (!selfMember.getVoiceState().inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED)).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        if (!memberVoiceState.inAudioChannel()) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfMember.getVoiceState().getChannel())) {
            event.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL)).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final List<MessageEmbed> embedsToSend = new ArrayList<>();

        if (selectedOptions.contains(RequestChannelConfig.ChannelConfig.Field.FILTERS.getId() + ":8d")) {
            if (filters.getRotation() == null) {
                filters.setRotation(new Rotation()
                        .setFrequency(0.05F)).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS)), Pair.of("{filter}", "8D"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "8D"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS))));
            }
        } else {
            if (filters.getRotation() != null) {
                filters.setRotation(null).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS)), Pair.of("{filter}", localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.EIGHT_D)))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.EIGHT_D)), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))));
            }
        }

        if (selectedOptions.contains(RequestChannelConfig.ChannelConfig.Field.FILTERS.getId() + ":karaoke")) {
            if (filters.getKaraoke() == null) {
                filters.setKaraoke(new Karaoke()).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS)), Pair.of("{filter}", "Karaoke"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Karaoke"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS))));
            }
        } else {
            if (filters.getKaraoke() != null) {
                filters.setKaraoke(null).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS)), Pair.of("{filter}", "Karaoke"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Karaoke"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))));
            }
        }

        if (selectedOptions.contains(RequestChannelConfig.ChannelConfig.Field.FILTERS.getId() + ":nightcore")) {
            if (filters.getTimescale() == null) {
                filters.setTimescale(new Timescale()
                        .setPitch(1.5F)
                ).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS)), Pair.of("{filter}", "Nightcore"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Nightcore"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS))));

            }
        } else {
            if (filters.getTimescale() != null) {
                filters.setTimescale(null).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS)), Pair.of("{filter}", "Nightcore"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Nightcore"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))));
            }
        }

        if (selectedOptions.contains(RequestChannelConfig.ChannelConfig.Field.FILTERS.getId() + ":tremolo")) {
            if (filters.getTremolo() == null) {
                filters.setTremolo(new Tremolo()).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS)), Pair.of("{filter}", "Tremolo"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS))));
            }
        } else {
            if (filters.getTremolo() != null) {
                filters.setTremolo(null).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS)), Pair.of("{filter}", "Tremolo"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))));

            }
        }

        if (selectedOptions.contains(RequestChannelConfig.ChannelConfig.Field.FILTERS.getId() + ":vibrato")) {
            if (filters.getVibrato() == null) {
                filters.setVibrato(new Vibrato()).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS)), Pair.of("{filter}", "Vibrato"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.ON_STATUS))));
            }
        } else {
            if (filters.getVibrato() != null) {
                filters.setVibrato(null).commit();
                embedsToSend.add(RobertifyEmbedUtils.embedMessage(
                        guild,
                        localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_MESSAGE, Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS)), Pair.of("{filter}", "Vibrato"))
                ).build());
                new LogUtils(guild).sendLog(LogType.FILTER_TOGGLE, event.getUser().getAsMention() + " " + localeManager.getMessage(RobertifyLocaleMessage.FilterMessages.FILTER_TOGGLE_LOG_MESSAGE, Pair.of("{filter}", "Tremolo"), Pair.of("{status}", localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.OFF_STATUS))));
            }
        }

        event.replyEmbeds(embedsToSend)
                .setEphemeral(true)
                .queue();
    }

    private boolean djCheck(AbstractSlashCommand command, Guild guild, Member user) {
        final var toggles = TogglesConfig.getConfig(guild);
        if (toggles.isDJToggleSet(command)) {
            if (toggles.getDJToggle(command)) {
                return GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_DJ);
            }
        }
        return true;
    }
}
