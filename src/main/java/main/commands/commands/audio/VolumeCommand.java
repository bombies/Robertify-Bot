package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
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

//        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_DJ)) {
//            msg.replyEmbeds(EmbedUtils.embedMessage("You must be a DJ to use this command").build())
//                    .queue();
//            return;
//        }

        if (ctx.getArgs().isEmpty()) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide what volume you'd like to set the bot to").build())
                    .queue();
            return;
        }

        if (!GeneralUtils.stringIsInt(ctx.getArgs().get(0))) {
            msg.replyEmbeds(EmbedUtils.embedMessage("You must provide an integer as the volume").build())
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
        if (!selfVoiceState.inVoiceChannel())
            return EmbedUtils.embedMessage("You can't use this command while I'm not in a voice channel");

        if (memberVoiceState.inVoiceChannel() && !memberVoiceState.getChannel().equals(selfVoiceState.getChannel()))
            return EmbedUtils.embedMessage("You must be in the same voice channel as I am to use this command");

        if (volume < 0 || volume > 100)
            return EmbedUtils.embedMessage("You can't set the volume to that value");

        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(memberVoiceState.getGuild());
        var audioPlayer = musicManager.audioPlayer;

        audioPlayer.setVolume(volume);
        return EmbedUtils.embedMessage("🔊  You have set the volume of the bot to **"+volume+"%**");
    }

    @Override
    public String getName() {
        return "volume";
    }

    @Override
    public String getHelp(String guildID) {
        return "control the volume of the bot";
    }

    @Override
    public List<String> getAliases() {
        return List.of("v", "vol");
    }
}
