package main.commands.slashcommands.commands.management.dedicatedchannel;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.ICommand;
import main.commands.prefixcommands.audio.*;
import main.commands.slashcommands.commands.audio.FavouriteTracksCommand;
import main.commands.slashcommands.commands.audio.PreviousTrackCommand;
import main.commands.slashcommands.commands.audio.StopCommand;
import main.constants.Permission;
import main.constants.SourcePlaylistPatterns;
import main.constants.Toggles;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.votes.VoteManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class DedicatedChannelEvents extends ListenerAdapter {

    @Override
    public void onChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (!event.isFromType(ChannelType.TEXT)) return;

        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        if (!config.isChannelSet(guild.getIdLong())) return;
        if (config.getChannelID(guild.getIdLong()) != event.getChannel().getIdLong()) return;

        config.removeChannel(guild.getIdLong());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        try {
            if (!config.isChannelSet(guild.getIdLong())) return;
        } catch (NullPointerException ignored) {
            return;
        }

        if (config.getChannelID(guild.getIdLong()) != event.getChannel().getIdLong()) return;

        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final User user = event.getAuthor();

        Message eventMessage = event.getMessage();
        String message = eventMessage.getContentRaw();

        if (!message.startsWith(new GuildConfig().getPrefix(guild.getIdLong())) && !user.isBot() && !event.isWebhookMessage()) {
            if (!memberVoiceState.inAudioChannel()) {
                event.getMessage().reply(user.getAsMention()).setEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in a voice channel to do this")
                                .build())
                        .queue();
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                return;
            }

            if (selfVoiceState.inAudioChannel()) {
                if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                    event.getMessage().reply(user.getAsMention()).setEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention())
                                    .build())
                            .queue();
                    event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                    return;
                }
            } else {
                if (new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                    final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                    if (!restrictedChannelsConfig.isRestrictedChannel(guild.getIdLong(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                        event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I can't join this channel!" +
                                        (!restrictedChannelsConfig.getRestrictedChannels(
                                                guild.getIdLong(),
                                                RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                        ).isEmpty()
                                                ?
                                                "\n\nI am restricted to only join\n" + restrictedChannelsConfig.restrictedChannelsToString(
                                                        guild.getIdLong(),
                                                        RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                                )
                                                :
                                                "\n\nRestricted voice channels have been toggled **ON**, but there aren't any set!"
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
                        message = GeneralUtils.isUrl(msgNoFlags) ? "" : "ytsearch:" + msgNoFlags;
                        addToBeginning = true;
                    }
                    case "-s", "-shuffle" -> {
                        var msgNoFlags = message.replaceAll("\\s-(s|shuffle)$", "");

                        if (!GeneralUtils.isUrl(msgNoFlags)) {
                            event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide the URL of an album/playlist for me to shuffle!").build())
                                    .queue();
                            event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                                return;
                        }

                        if (!SourcePlaylistPatterns.isPlaylistLink(msgNoFlags)) {
                            event.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide a valid Spotify/Deezer/YouTube/SoundCloud playlist or album").build())
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
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getButton().getId().startsWith(DedicatedChannelCommand.ButtonID.IDENTIFIER.toString()))
            return;

        final DedicatedChannelConfig config = new DedicatedChannelConfig();

        if (!config.isChannelSet(event.getGuild().getIdLong())) return;
        if (event.getTextChannel().getIdLong() != config.getChannelID(event.getGuild().getIdLong())) return;

        final String id = event.getButton().getId();
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final var guild = event.getGuild();
        final Member member = event.getMember();

        if (!selfVoiceState.inAudioChannel()) {
            event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "I must be in a voice channel to do this.").build())
                    .queue();
            return;
        }

        if (!memberVoiceState.inAudioChannel()) {
            event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention())
                    .build())
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention())
                            .build())
                    .queue();
            return;
        }

        if (id.equals(DedicatedChannelCommand.ButtonID.REWIND.toString())) {
            if (!djCheck(new RewindCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder rewindEmbed = new RewindCommand().handleRewind(member.getUser(), selfVoiceState, 0, true);
            event.reply(member.getAsMention()).addEmbeds(rewindEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.PLAY_AND_PAUSE.toString())) {
            if (!djCheck(new PauseCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder playPauseEmbed = new PauseCommand().handlePauseEvent(event.getGuild(), selfVoiceState, memberVoiceState);
            event.reply(member.getAsMention()).addEmbeds(playPauseEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.END.toString())) {
            if (!djCheck(new SkipCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            MessageEmbed skipEmbed = new SkipCommand().handleSkip(selfVoiceState, memberVoiceState);
            event.reply(member.getAsMention()).addEmbeds(skipEmbed)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.LOOP.toString())) {
            if (!djCheck(new LoopCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder loopEmbed = new LoopCommand().handleRepeat(musicManager, member.getUser());
            event.reply(member.getAsMention()).addEmbeds(loopEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.LOOP + "queue")) {
            if (!djCheck(new LoopCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder loopEmbed = new LoopCommand().handleQueueRepeat(musicManager, member.getUser());
            event.reply(member.getAsMention()).addEmbeds(loopEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.SHUFFLE.toString())) {
            if (!djCheck(new ShuffleCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder shuffleEmbed = new ShuffleCommand().handleShuffle(event.getGuild(), member.getUser());
            event.reply(member.getAsMention()).addEmbeds(shuffleEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.DISCONNECT.toString())) {
            if (!djCheck(new DisconnectCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder disconnectEmbed = new DisconnectCommand().handleDisconnect(event.getGuild(), event.getUser());
            event.reply(member.getAsMention()).addEmbeds(disconnectEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.STOP.toString())) {
            if (!djCheck(new StopCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
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
                                "🔒 Locked Command", """
                                                    Woah there! You must vote before interacting with this command.
                                                    Click on each of the buttons below to vote!

                                                    *Note: Only the first two votes sites are required, the last two are optional!*""").build())
                        .addActionRow(
                                Button.of(ButtonStyle.LINK, "https://top.gg/bot/893558050504466482/vote", "Top.gg"),
                                Button.of(ButtonStyle.LINK, "https://discordbotlist.com/bots/robertify/upvote", "Discord Bot List"),
                                Button.of(ButtonStyle.LINK, "https://discords.com/bots/bot/893558050504466482/vote", "Discords.com")
                        )
                        .queue();
                return;
            }

            if (!djCheck(new PreviousTrackCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder previousEmbed = new PreviousTrackCommand().handlePrevious(event.getGuild(), memberVoiceState);
            event.reply(member.getAsMention()).addEmbeds(previousEmbed.build())
                    .setEphemeral(false)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.FAVOURITE.toString())) {
            if (!djCheck(new FavouriteTracksCommand(), guild, member)) {
                event.reply(member.getAsMention()).addEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            event.reply(member.getAsMention()).addEmbeds(new FavouriteTracksCommand().handleAdd(event.getGuild(), event.getMember()))
                    .setEphemeral(false)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        }
    }

    private boolean djCheck(ICommand command, Guild guild, Member user) {
        final var toggles = new TogglesConfig();
        if (toggles.isDJToggleSet(guild, command)) {
            if (toggles.getDJToggle(guild, command)) {
                return GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_DJ);
            }
        }

        return true;
    }
}
