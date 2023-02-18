package main.commands.prefixcommands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Toggles;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.script.ScriptException;

public class JoinCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member member = ctx.getMember();
        final Guild guild = ctx.getGuild();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();

        msg.replyEmbeds(handleJoin(guild, ctx.getChannel(), memberVoiceState, selfVoiceState))
                .queue();
    }

    public MessageEmbed handleJoin(Guild guild, TextChannel textChannel, GuildVoiceState memberVoiceState, GuildVoiceState selfVoiceState) {
        if (!memberVoiceState.inAudioChannel()) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED)
                    .build();
        }

        final var channel = memberVoiceState.getChannel();

        if (new TogglesConfig(guild).getToggle(Toggles.RESTRICTED_VOICE_CHANNELS)) {
            final var restrictedChannelsConfig = new RestrictedChannelsConfig(guild);
            final var localeManager = LocaleManager.getLocaleManager(guild);
            if (!restrictedChannelsConfig.isRestrictedChannel(channel.getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                return RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.CANT_JOIN_CHANNEL) +
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
                ).build();
            }
        }

        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var placeholderPair = Pair.of("{channel}", channel.getAsMention());

        if (memberVoiceState.getChannel() == selfVoiceState.getChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JoinMessages.ALREADY_JOINED, placeholderPair).build();
        try {
            RobertifyAudioManager.getInstance()
                    .joinAudioChannel(channel, musicManager);
        } catch (IllegalStateException e) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JoinMessages.CANT_JOIN, placeholderPair)
                    .build();
        } catch (InsufficientPermissionException e) {
            return RobertifyEmbedUtils.embedMessage(channel.getGuild(), RobertifyLocaleMessage.GeneralMessages.INSUFFICIENT_PERMS_TO_JOIN, placeholderPair).build();
        }
        return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.JoinMessages.JOINED, placeholderPair).build();
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getHelp(String prefix) {
        return """
                Use this command to forcefully move the bot into your voice channel.

                *NOTE: This command can be made DJ only by using* `toggles dj join`""";
    }
}
