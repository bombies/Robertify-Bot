package main.commands.commands.audio;

import lombok.Getter;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.Toggles;
import main.utils.json.toggles.TogglesConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

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
        final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        final var audioPlayer = musicManager.audioPlayer;
        final var queue = musicManager.scheduler.queue;

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You need to be in a voice channel for this to work").build()).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command!")
                            .build())
                    .queue();
            return;
        } else if (!selfVoiceState.inVoiceChannel()) {
            if (new TogglesConfig().getToggle(ctx.getGuild(), Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                if (!restrictedChannelsConfig.isRestrictedChannel(ctx.getGuild().getIdLong(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                    msg.replyEmbeds(EmbedUtils.embedMessage("I can't join this channel!" +
                                    (!restrictedChannelsConfig.getRestrictedChannels(
                                            ctx.getGuild().getIdLong(),
                                            RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                    ).isEmpty()
                                            ?
                                            "\n\nI am restricted to only join\n" + restrictedChannelsConfig.restrictedChannelsToString(
                                                    ctx.getGuild().getIdLong(),
                                                    RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL
                                            )
                                            :
                                            "\n\nRestricted voice channels have been toggled **ON**, but there aren't any set!"
                                    )
                            ).build())
                            .queue();
                    return;
                }
            }
        }

        if (lofiEnabledGuilds.contains(guild.getIdLong())) {
            lofiEnabledGuilds.remove(guild.getIdLong());
            musicManager.scheduler.nextTrack();

            msg.replyEmbeds(EmbedUtils.embedMessage("You have disabled Lo-Fi mode").build())
                    .queue();
        } else {
            queue.clear();
            musicManager.scheduler.getPastQueue().clear();
            audioPlayer.stopTrack();

            if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
                new DedicatedChannelConfig().updateMessage(guild);

            msg.replyEmbeds(EmbedUtils.embedMessage("Now enabling Lo-Fi mode...").build())
                            .queue(botMsg -> {
                                lofiEnabledGuilds.add(guild.getIdLong());
                                announceLofiMode.add(guild.getIdLong());
                                RobertifyAudioManager.getInstance()
                                        .loadAndPlayFromDedicatedChannel(
                                                ctx.getChannel(),
                                                "https://www.youtube.com/watch?v=5qap5aO4i9A&ab_channel=LofiGirl",
                                                guild.getSelfMember().getVoiceState(),
                                                ctx.getMember().getVoiceState(),
                                                ctx,
                                                botMsg
                                        );
                            });
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
}
