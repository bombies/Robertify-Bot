package main.commands.slashcommands.commands.management.dedicatedchannel;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.ICommand;
import main.commands.prefixcommands.audio.*;
import main.commands.slashcommands.commands.audio.*;
import main.constants.Permission;
import main.constants.SourcePlaylistPatterns;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class DedicatedChannelEvents extends ListenerAdapter {

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        final Guild guild = event.getGuild();
        final DedicatedChannelConfig config = new DedicatedChannelConfig(guild);

        if (!config.isChannelSet()) return;
        if (config.getChannelID() != event.getChannel().getIdLong()) return;

        config.removeChannel();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        final Guild guild = event.getGuild();
        final DedicatedChannelConfig config = new DedicatedChannelConfig(guild);

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
            if (!memberVoiceState.inVoiceChannel()) {
                event.getMessage().reply(user.getAsMention()).setEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
                                .build())
                        .queue();
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                return;
            }

            if (selfVoiceState.inVoiceChannel()) {
                if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                    event.getMessage().reply(user.getAsMention()).setEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                                    .build())
                            .queue();
                    event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                    return;
                }
            } else {
                if (new TogglesConfig(guild).getToggle(Toggles.RESTRICTED_VOICE_CHANNELS)) {
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
                new PlayCommand().playLocalAudio(guild, event.getTextChannel(), eventMessage, event.getMember(), audioFile);

                event.getMessage().delete().queueAfter(2, TimeUnit.SECONDS, null, new ErrorHandler()
                        .handle(ErrorResponse.UNKNOWN_MESSAGE, ignored -> {})
                );

                return;
            }

            boolean addToBeginning = false;
            boolean shuffled = false;
            final var split = message.split(" ");

            if (split.length == 1) {
                message = (GeneralUtils.isUrl(message) ? "" : "ytsearch:")  + message;
            } else {
                switch (split[split.length-1].toLowerCase()) {
                    case "-n", "-next" -> {
                        String msgNoFlags = message.replaceAll("\\s-(n|next)$", "");
                        message = GeneralUtils.isUrl(msgNoFlags) ? msgNoFlags : "ytsearch:" + msgNoFlags;
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
                    default -> message = GeneralUtils.isUrl(message) ? "" : "ytsearch:" + message;
                }
            }

            if (addToBeginning)
                RobertifyAudioManager.getInstance()
                        .loadAndPlayFromDedicatedChannel(event.getTextChannel(), message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(), null, true);
            else if (shuffled) {
                RobertifyAudioManager.getInstance()
                        .loadAndPlayFromDedicatedChannelShuffled(event.getTextChannel(), message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(), null, false);
            } else {
                RobertifyAudioManager.getInstance()
                        .loadAndPlayFromDedicatedChannel(event.getTextChannel(), message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(), null, false);
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
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        if (!event.getButton().getId().startsWith(DedicatedChannelCommand.ButtonID.IDENTIFIER.toString()))
            return;

        final var guild = event.getGuild();
        final DedicatedChannelConfig config = new DedicatedChannelConfig(guild);

        if (!config.isChannelSet()) return;
        if (event.getTextChannel().getIdLong() != config.getChannelID()) return;

        final String id = event.getButton().getId();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final Member member = event.getMember();

        if (!selfVoiceState.inVoiceChannel()) {
            event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED).build())
                    .queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                    .build())
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .queue();
            return;
        }

        if (id.equals(DedicatedChannelCommand.ButtonID.REWIND.toString())) {
            if (!djCheck(new RewindSlashCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            EmbedBuilder rewindEmbed = new RewindCommand().handleRewind(member.getUser(), selfVoiceState, 0, true);
            event.reply(member.getAsMention()).addEmbeds(rewindEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.PLAY_AND_PAUSE.toString())) {
            if (!djCheck(new PauseSlashCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            EmbedBuilder playPauseEmbed = new PauseCommand().handlePauseEvent(event.getGuild(), selfVoiceState, memberVoiceState);
            event.reply(member.getAsMention()).addEmbeds(playPauseEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.END.toString())) {
            if (!djCheck(new SkipSlashCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            MessageEmbed skipEmbed = new SkipCommand().handleSkip(selfVoiceState, memberVoiceState);
            event.reply(member.getAsMention()).addEmbeds(skipEmbed)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.LOOP.toString())) {
            final LoopSlashCommand loopCommand = new LoopSlashCommand();
            if (!djCheck(loopCommand, guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            final var scheduler = musicManager.getScheduler();
            final var info = musicManager.getPlayer().getPlayingTrack().getInfo();
            final EmbedBuilder loopEmbed;
            if (scheduler.repeating) {
                scheduler.repeating = false;
                scheduler.playlistRepeating = true;
                loopEmbed = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_START);

                new LogUtils(guild).sendLog(
                        LogType.QUEUE_LOOP, RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_LOG,
                        Pair.of("{user}", member.getAsMention()),
                        Pair.of("{status}", "looped")
                );
            } else if (scheduler.playlistRepeating) {
                scheduler.playlistRepeating = false;
                scheduler.repeating = false;
                loopEmbed = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_STOP);

                new LogUtils(guild).sendLog(
                        LogType.QUEUE_LOOP, RobertifyLocaleMessage.LoopMessages.QUEUE_LOOP_LOG,
                        Pair.of("{user}", member.getAsMention()),
                        Pair.of("{status}", "unlooped")
                );
            } else {
                scheduler.repeating = true;
                scheduler.playlistRepeating = false;
                loopEmbed = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LoopMessages.LOOP_START);

                new LogUtils(guild).sendLog(
                        LogType.TRACK_LOOP, RobertifyLocaleMessage.LoopMessages.LOOP_LOG,
                        Pair.of("{user}", member.getAsMention()),
                        Pair.of("{status}", "looped"),
                        Pair.of("{title}", info.title),
                        Pair.of("{author}", info.author)

                );
            }

            event.reply(member.getAsMention()).addEmbeds(loopEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.SHUFFLE.toString())) {
            if (!djCheck(new ShuffleSlashCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            EmbedBuilder shuffleEmbed = new ShuffleCommand().handleShuffle(event.getGuild(), member.getUser());
            event.reply(member.getAsMention()).addEmbeds(shuffleEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.DISCONNECT.toString())) {
            if (!djCheck(new DisconnectSlashCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            EmbedBuilder disconnectEmbed = new DisconnectCommand().handleDisconnect(event.getGuild(), event.getUser());
            event.reply(member.getAsMention()).addEmbeds(disconnectEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.STOP.toString())) {
            if (!djCheck(new StopCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            EmbedBuilder stopEmbed = new StopCommand().handleStop(event.getMember());
            event.reply(member.getAsMention()).addEmbeds(stopEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.PREVIOUS.toString())) {
            if (!new VoteManager().userVoted(member.getId(), VoteManager.Website.TOP_GG)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                                RobertifyLocaleMessage.PremiumMessages.LOCKED_COMMAND_EMBED_TITLE, RobertifyLocaleMessage.PremiumMessages.LOCKED_COMMAND_EMBED_DESC).build())
                        .addActionRow(
                                Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                                Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List")
                        )
                        .queue();
                return;
            }

            if (!djCheck(new PreviousTrackCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            EmbedBuilder previousEmbed = new PreviousTrackCommand().handlePrevious(event.getGuild(), memberVoiceState);
            event.reply(member.getAsMention()).addEmbeds(previousEmbed.build())
                    .setEphemeral(false)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.FAVOURITE.toString())) {
            if (!new VoteManager().userVoted(member.getId(), VoteManager.Website.TOP_GG)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessageWithTitle(guild,
                                RobertifyLocaleMessage.PremiumMessages.LOCKED_COMMAND_EMBED_TITLE, RobertifyLocaleMessage.PremiumMessages.LOCKED_COMMAND_EMBED_DESC).build())
                        .addActionRow(
                                Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                                Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List")
                        )
                        .queue();
                return;
            }

            if (!djCheck(new FavouriteTracksCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.DJ_BUTTON).build())
                        .queue();
                return;
            }

            event.reply(member.getAsMention()).addEmbeds(new FavouriteTracksCommand().handleAdd(event.getGuild(), event.getMember()))
                    .setEphemeral(false)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        }
    }

    private boolean djCheck(AbstractSlashCommand command, Guild guild, Member user) {
        final var toggles = new TogglesConfig(guild);
        if (toggles.isDJToggleSet(command)) {
            if (toggles.getDJToggle(command)) {
                return GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_DJ);
            }
        }
        return true;
    }
}
