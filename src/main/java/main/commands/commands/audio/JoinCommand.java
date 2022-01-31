package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.constants.Toggles;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import net.dv8tion.jda.api.entities.*;

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
        if (!memberVoiceState.inVoiceChannel()) {
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in a voice channel to use this command")
                    .build();
        }

        VoiceChannel channel = memberVoiceState.getChannel();

        if (new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
            final var restrictedChannelsConfig = new RestrictedChannelsConfig();
            if (!restrictedChannelsConfig.isRestrictedChannel(guild.getIdLong(), channel.getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                return RobertifyEmbedUtils.embedMessage(guild, "I can't join this channel!" +
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
                ).build();
            }
        }

        if (selfVoiceState.inVoiceChannel()) {
            guild.moveVoiceMember(selfVoiceState.getMember(), channel)
                    .queue(success -> RobertifyAudioManager.getInstance().getMusicManager(guild)
                            .getScheduler().scheduleDisconnect(true));
            return RobertifyEmbedUtils.embedMessage(guild, "I have moved to " + channel.getAsMention()).build();
        } else {
            RobertifyAudioManager.getInstance()
                    .joinVoiceChannel(textChannel, selfVoiceState, memberVoiceState);

            RobertifyAudioManager.getInstance().getMusicManager(guild)
                    .getScheduler().scheduleDisconnect(true);
            return RobertifyEmbedUtils.embedMessage(guild, "I have joined " + channel.getAsMention()).build();
        }
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
