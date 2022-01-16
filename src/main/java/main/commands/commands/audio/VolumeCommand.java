package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

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

        if (!selfVoiceState.inVoiceChannel())
            return RobertifyEmbedUtils.embedMessage(guild, "You can't use this command while I'm not in a voice channel");

        if (memberVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as I am to use this command");

        if (volume < 0 || volume > 100)
            return RobertifyEmbedUtils.embedMessage(guild, "You can't set the volume to that value");

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(memberVoiceState.getGuild());
        var audioPlayer = musicManager.getPlayer();

        audioPlayer.setVolume(volume);

        if (new DedicatedChannelConfig().isChannelSet(selfVoiceState.getGuild().getIdLong()))
            new DedicatedChannelConfig().updateMessage(selfVoiceState.getGuild());

        return RobertifyEmbedUtils.embedMessage(guild, "🔊  You have set the volume of the bot to **"+volume+"%**");
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
}
