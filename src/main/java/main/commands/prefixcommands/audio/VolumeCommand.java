package main.commands.prefixcommands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import main.utils.locale.RobertifyLocaleMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import javax.script.ScriptException;
import java.util.List;

@Deprecated
public class VolumeCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        if (ctx.getArgs().isEmpty()) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide what volume you'd like to set the bot to").build())
                    .queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(ctx.getArgs().get(0))) {
            msg.replyEmbeds(RobertifyEmbedUtils.embedMessage(guild, "You must provide an integer as the volume").build())
                    .queue();
            return;
        }

        final int volume = Integer.parseInt(ctx.getArgs().get(0));
        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = ctx.getSelfMember().getVoiceState();

        msg.replyEmbeds(handleVolumeChange(selfVoiceState, memberVoiceState, volume).build())
                .queue();
    }

    public EmbedBuilder handleVolumeChange(GuildVoiceState selfVoiceState, GuildVoiceState memberVoiceState, int volume) {
        final var guild = selfVoiceState.getGuild();

        if (!selfVoiceState.inAudioChannel())
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.VOICE_CHANNEL_NEEDED);

        if (memberVoiceState.inAudioChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.GeneralMessages.SAME_VOICE_CHANNEL_LOC, Pair.of("{channel}", selfVoiceState.getChannel().getAsMention()));

        if (volume < 0 || volume > 100)
            return RobertifyEmbedUtils.embedMessage(guild, RobertifyLocaleMessage.VolumeMessages.INVALID_VOLUME);

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(memberVoiceState.getGuild());
        var audioPlayer = musicManager.getPlayer();

        audioPlayer.getFilters().setVolume((float)volume/100).commit();

        if (new RequestChannelConfig(guild).isChannelSet())
            new RequestChannelConfig(guild).updateMessage();

        new LogUtils(guild).sendLog(LogType.VOLUME_CHANGE,
                RobertifyLocaleMessage.VolumeMessages.VOLUME_CHANGED_LOG,
                Pair.of("{user}", memberVoiceState.getMember().getAsMention()),
                Pair.of("{volume}", String.valueOf(volume))
        );
        return RobertifyEmbedUtils.embedMessage(guild,
                RobertifyLocaleMessage.VolumeMessages.VOLUME_CHANGED,
                Pair.of("{volume}", String.valueOf(volume))
        );
    }

    @Override
    public String getName() {
        return "volume";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n\n" +
                "Control the volume of the bot\n\n" +
                "**__Usages__**\n`" +
                prefix + "volume <0-100>`";
    }

    @Override
    public List<String> getAliases() {
        return List.of("v", "vol");
    }

    @Override
    public boolean isPremiumCommand() {
        return true;
    }
}
