package main.commands.prefixcommands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.script.ScriptException;
import java.util.List;

@Deprecated @ForRemoval
public class DisconnectCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfState = self.getVoiceState();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        EmbedBuilder eb;

        if (!selfState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "I'm already not in a voice channel!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inAudioChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in the same voice channel as me for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfState.getChannel().getAsMention());
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        msg.replyEmbeds(handleDisconnect(ctx.getGuild(), ctx.getAuthor()).build())
                .queue();
    }

    public EmbedBuilder handleDisconnect(Guild guild, User disconnecter) {
        var musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);

        musicManager.leave();
        new LogUtils().sendLog(guild, LogType.BOT_DISCONNECTED, disconnecter.getAsMention() + " has disconnected the bot.");

        return RobertifyEmbedUtils.embedMessage(guild, "Disconnected!");
    }

    @Override
    public String getName() {
        return "disconnect";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases()) +"`\n" +
                "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("dc", "leave");
    }
}
