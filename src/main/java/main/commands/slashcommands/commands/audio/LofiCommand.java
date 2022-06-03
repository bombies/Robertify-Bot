package main.commands.slashcommands.commands.audio;

import lombok.Getter;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Toggles;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import main.utils.resume.ResumeUtils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

public class LofiCommand implements ICommand {
    @Getter
    private final static List<Long> lofiEnabledGuilds = new ArrayList<>();
    @Getter
    private final static List<Long> announceLofiMode = new ArrayList<>();

    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();

        try {
            msg.replyEmbeds(handleLofi(guild, ctx.getMember()))
                    .queue();
        } catch (IllegalArgumentException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LofiMessages.LOFI_ENABLING).build())
                    .queue(botMsg -> {
                        lofiEnabledGuilds.add(guild.getIdLong());
                        announceLofiMode.add(guild.getIdLong());
                        RobertifyAudioManager.getInstance()
                                .loadAndPlayFromDedicatedChannel(
                                        ctx.getChannel(),
                                        "https://www.youtube.com/watch?v=5qap5aO4i9A&ab_channel=LofiGirl",
                                        guild.getSelfMember().getVoiceState(),
                                        ctx.getMember().getVoiceState(),
                                        botMsg,
                                        false
                                );
                        ResumeUtils.getInstance().saveInfo(guild, guild.getSelfMember().getVoiceState().getChannel());
                    });
        }
    }

    public MessageEmbed handleLofi(Guild guild, Member member) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var queue = musicManager.getScheduler().queue;

        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED).build();


        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                    .build();
        } else if (!selfVoiceState.inVoiceChannel()) {
            if (new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                final var localeManager = LocaleManager.getLocaleManager(guild);
                if (!restrictedChannelsConfig.isRestrictedChannel(guild.getIdLong(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                    return RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.CANT_JOIN_CHANNEL) +
                            (!restrictedChannelsConfig.getRestrictedChannels(
                                    guild.getIdLong(),
                                    RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                            ).isEmpty()
                                    ?
                                    localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.RESTRICTED_TO_JOIN, Pair.of("{channels}", restrictedChannelsConfig.restrictedChannelsToString(
                                            guild.getIdLong(),
                                            RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                    )))
                                    :
                                    localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.NO_VOICE_CHANNEL)
                            )
                    ).build();
                }
            }
        }

        if (lofiEnabledGuilds.contains(guild.getIdLong())) {
            lofiEnabledGuilds.remove(guild.getIdLong());
            musicManager.getScheduler().nextTrack(null);

            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.LofiMessages.LOFI_DISABLED).build();
        } else {
            queue.clear();
            musicManager.getScheduler().getPastQueue().clear();
            audioPlayer.stopTrack();

            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
                new DedicatedChannelConfig().updateMessage(guild);

            return null;
        }
    }

    @Override
    public String getName() {
        return "lofi";
    }

    @Override
    public String getHelp(String prefix) {
        return """
                Looking for some music to study or relax to? Use this command!

                Upon execution, the bot will clear the current queue and indefinitely play Lo-Fi songs for you.\s

                If you wish to stop this you can always run the `stop` command or disconnect the bot through whatever means. You can also run this command again.""";
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
