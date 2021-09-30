package main.commands.commands.audio;

import main.commands.CommandContext;
import main.commands.ICommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;

public class LeaveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfState = self.getVoiceState();
        final Message msg = ctx.getMessage();

        if (!selfState.inVoiceChannel()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("I'm already not in a voice channel!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        ctx.getGuild().getAudioManager().closeAudioConnection();
        msg.addReaction("✅").queue();
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getHelp(String guildID) {
        return null;
    }
}
