package main.commands.slashcommands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Toggles;
import main.main.Listener;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import main.utils.json.guildconfig.GuildConfig;
import main.utils.json.restrictedchannels.RestrictedChannelsConfig;
import main.utils.json.toggles.TogglesConfig;
import main.utils.locale.LocaleManager;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.script.ScriptException;
import java.util.List;

public class ShufflePlayCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final TextChannel channel = ctx.getChannel();
        final List<String> args = ctx.getArgs();
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        final Guild guild = ctx.getGuild();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        if (args.isEmpty()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.MISSING_LINK);
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!GeneralUtils.isUrl(args.get(0))) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.INVALID_LINK).build()).queue();
            return;
        }

        final String url = args.get(0);

        if (!url.contains("deezer.page.link")) {
            if (url.contains("soundcloud.com") && !url.contains("sets")) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.NOT_PLAYLIST, Pair.of("{source}", "SoundCloud")).build()).queue();
                return;
            } else if (url.contains("youtube.com") && !url.contains("playlist") && !url.contains("list")) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.NOT_PLAYLIST, Pair.of("{source}", "YouTube")).build()).queue();
                return;
            } else if (!url.contains("playlist") && !url.contains("album") && !url.contains("soundcloud.com") && !url.contains("youtube.com")) {
                msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.ShufflePlayMessages.MUST_PROVIDE_VALID_PLAYLIST).build()).queue();
                return;
            }
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.USER_VOICE_CHANNEL_NEEDED);
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()))
                            .build())
                    .queue();
            return;
        } else if (!selfVoiceState.inVoiceChannel()) {
            if (new TogglesConfig().getToggle(ctx.getGuild(), Toggles.RESTRICTED_VOICE_CHANNELS)) {
                final var restrictedChannelsConfig = new RestrictedChannelsConfig();
                final var localeManager = LocaleManager.getLocaleManager(guild);
                if (!restrictedChannelsConfig.isRestrictedChannel(ctx.getGuild().getIdLong(), memberVoiceState.getChannel().getIdLong(), RestrictedChannelsConfig.ChannelType.VOICE_CHANNEL)) {
                    msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, localeManager.getMessage(RobertifyLocaleMessage.GeneralMessages.CANT_JOIN_CHANNEL) +
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
                            ).build())
                            .queue();
                    return;
                }
            }
        }

        channel.sendMessageEmbeds(RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.FavouriteTracksMessages.FT_ADDING_TO_QUEUE_2).build()).queue(addingMsg -> {
            RobertifyAudioManager.getInstance()
                    .loadAndPlayShuffled(url, selfVoiceState, memberVoiceState, ctx, addingMsg, false);
        });
    }

    @Override
    public String getName() {
        return "shuffleplay";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n\n" +
                "Looking to play a playlist but shuffled right off the bat? This command does that for you.\n\n"
                + getUsages(prefix);
    }

    @Override
    public String getUsages(String prefix) {
        return "**__Usages__**\n" +
                "`"+prefix+"shuffleplay <playlistlink>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("splay", "spl");
    }
}
