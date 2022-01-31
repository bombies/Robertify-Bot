package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class DisconnectCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfState = self.getVoiceState();
        final Message msg = ctx.getMessage();
        final var guild = ctx.getGuild();

        EmbedBuilder eb;

        if (!selfState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "I'm already not in a voice channel!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be in the same voice channel as me for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfState.getChannel())) {
            eb = RobertifyEmbedUtils.embedMessage(guild, "You must be in the same voice channel as me to use this command!" + "\n\nI am currently in: " + selfState.getChannel().getAsMention());
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        msg.replyEmbeds(handleDisconnect(ctx.getGuild()).build())
                .queue();
    }

    public EmbedBuilder handleDisconnect(Guild guild) {
        var musicManager = RobertifyAudioManager.getInstance().getLavaLinkMusicManager(guild);

        musicManager.leave();

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
