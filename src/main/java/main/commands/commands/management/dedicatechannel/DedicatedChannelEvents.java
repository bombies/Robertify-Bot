package main.commands.commands.management.dedicatechannel;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.audio.*;
import main.commands.commands.dev.MongoMigrationCommand;
import main.commands.commands.management.permissions.Permission;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.GeneralUtils;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.Toggles;
import main.utils.json.toggles.TogglesConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class DedicatedChannelEvents extends ListenerAdapter {

    @Override
    public void onTextChannelDelete(@NotNull TextChannelDeleteEvent event) {
        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        if (!config.isChannelSet(guild.getIdLong())) return;
        if (config.getChannelID(guild.getIdLong()) != event.getChannel().getIdLong()) return;

        config.removeChannel(guild.getIdLong());
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        final DedicatedChannelConfig config = new DedicatedChannelConfig();
        final Guild guild = event.getGuild();

        if (!config.isChannelSet(guild.getIdLong())) return;
        if (config.getChannelID(guild.getIdLong()) != event.getChannel().getIdLong()) return;

        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final User user = event.getAuthor();

        if (!user.isBot()) {
            if (MongoMigrationCommand.isMigrating()) {
                event.getMessage().replyEmbeds(EmbedUtils.embedMessage("I am migrating databases at the moment!" +
                                " You are not allowed to use this feature.")
                        .build()).queue();
                return;
            }

            if (!memberVoiceState.inVoiceChannel()) {
                event.getMessage().reply(user.getAsMention()).setEmbeds(EmbedUtils.embedMessage("You must be in a voice channel to use this command")
                                .build())
                        .queue();
                event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                return;
            }

            if (selfVoiceState.inVoiceChannel()) {
                if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
                    event.getMessage().reply(user.getAsMention()).setEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command")
                                    .build())
                            .queue();
                    event.getMessage().delete().queueAfter(10, TimeUnit.SECONDS);
                    return;
                }
            } else {
                if (new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                    final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                    if (!restrictedChannelsConfig.isRestrictedChannel(guild.getIdLong(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                        event.getMessage().replyEmbeds(EmbedUtils.embedMessage("I can't join this channel!" +
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
        }

        String message = event.getMessage().getContentRaw();

        if (!message.startsWith(new GuildConfig().getPrefix(guild.getIdLong())) && !user.isBot()) {
            if (!GeneralUtils.isUrl(message))
                message = "ytsearch:" + message;

            RobertifyAudioManager.getInstance()
                    .loadAndPlayFromDedicatedChannel(message, guild.getSelfMember().getVoiceState(), event.getMember().getVoiceState(),
                            new CommandContext(event, null), null);
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

        final DedicatedChannelConfig config = new DedicatedChannelConfig();

        if (!config.isChannelSet(event.getGuild().getIdLong())) return;
        if (event.getTextChannel().getIdLong() != config.getChannelID(event.getGuild().getIdLong())) return;

        final String id = event.getButton().getId();
        final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(event.getGuild());
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();
        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final User user = event.getUser();

        if (!selfVoiceState.inVoiceChannel()) {
            event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("I must be in a voice channel to do this.").build())
                    .queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command")
                    .build())
                    .queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command")
                            .build())
                    .queue();
            return;
        }

        final var guild = event.getGuild();
        if (id.equals(DedicatedChannelCommand.ButtonID.REWIND.toString())) {
            if (!djCheck(new RewindCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder rewindEmbed = new RewindCommand().handleRewind(selfVoiceState, 0, true);
            event.reply(user.getAsMention()).addEmbeds(rewindEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.PLAY_AND_PAUSE.toString())) {
            if (!djCheck(new PauseCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder playPauseEmbed = new PauseCommand().handlePauseEvent(event.getGuild(), selfVoiceState, memberVoiceState);
            event.reply(user.getAsMention()).addEmbeds(playPauseEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.END.toString())) {
            if (!djCheck(new SkipCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder skipEmbed = new SkipCommand().handleSkip(selfVoiceState, memberVoiceState);
            event.reply(user.getAsMention()).addEmbeds(skipEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.LOOP.toString())) {
            if (!djCheck(new LoopCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder loopEmbed = new LoopCommand().handleRepeat(musicManager);
            event.reply(user.getAsMention()).addEmbeds(loopEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.SHUFFLE.toString())) {
            if (!djCheck(new ShuffleCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder shuffleEmbed = new ShuffleCommand().handleShuffle(event.getGuild());
            event.reply(user.getAsMention()).addEmbeds(shuffleEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.DISCONNECT.toString())) {
            if (!djCheck(new DisconnectCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder disconnectEmbed = new DisconnectCommand().handleDisconnect(event.getGuild());
            event.reply(user.getAsMention()).addEmbeds(disconnectEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.STOP.toString())) {
            if (!djCheck(new StopCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder stopEmbed = new StopCommand().handleStop(musicManager);
            event.reply(user.getAsMention()).addEmbeds(stopEmbed.build())
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        } else if (id.equals(DedicatedChannelCommand.ButtonID.PREVIOUS.toString())) {
            if (!djCheck(new PreviousTrackCommand(), guild, user)) {
                event.reply(user.getAsMention()).addEmbeds(EmbedUtils.embedMessage("You must be a DJ" +
                                " to use this button!").build())
                        .queue();
                return;
            }

            EmbedBuilder previousEmbed = new PreviousTrackCommand().handlePrevious(event.getGuild(), memberVoiceState);
            event.reply(user.getAsMention()).addEmbeds(previousEmbed.build())
                    .setEphemeral(false)
                    .queue(null, new ErrorHandler()
                            .handle(ErrorResponse.UNKNOWN_INTERACTION, ignored -> {}));
        }
    }

    private boolean djCheck(ICommand command, Guild guild, User user) {
        final var toggles = new TogglesConfig();
        if (toggles.isDJToggleSet(guild, command)) {
            if (toggles.getDJToggle(guild, command)) {
                return GeneralUtils.hasPerms(guild, user, Permission.ROBERTIFY_DJ);
            }
        }

        return true;
    }
}
