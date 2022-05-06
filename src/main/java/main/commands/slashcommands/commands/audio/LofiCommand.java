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
import net.dv8tion.jda.api.entities.*;

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
            msg.replyEmbeds(handleLofi(guild, ctx.getMember(), ctx.getChannel()))
                    .queue();
        } catch (IllegalArgumentException e) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "Enabling Lo-Fi mode...").build())
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
                    });
        }
    }

    public MessageEmbed handleLofi(Guild guild, Member member, TextChannel channel) {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.getPlayer();
        final var queue = musicManager.getScheduler().queue;

        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "You need to be in a voice channel for this to work").build();


        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfVoiceState.getChannel().getAsMention())
                    .build();
        } else if (!selfVoiceState.inVoiceChannel()) {
            if (new TogglesConfig().getToggle(guild, Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                if (!restrictedChannelsConfig.isRestrictedChannel(guild.getIdLong(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
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
        }

        if (lofiEnabledGuilds.contains(guild.getIdLong())) {
            lofiEnabledGuilds.remove(guild.getIdLong());
            musicManager.getScheduler().nextTrack(null);

            return RobertifyEmbedUtils.embedMessage(guild, "You have disabled Lo-Fi mode").build();
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
