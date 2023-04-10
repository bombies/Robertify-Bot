package main.commands.prefixcommands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.requestchannel.RequestChannelConfig;
import main.utils.json.logs.LogType;
import main.utils.json.logs.LogUtils;
import net.dv8tion.jda.annotations.ForRemoval;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

@Deprecated @ForRemoval
public class ClearQueueCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final var musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final var queueHandler = musicManager.getScheduler().getQueueHandler();
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();
        final GuildVoiceState selfVoiceState = ctx.getGuild().getSelfMember().getVoiceState();

        if (queueHandler.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There is already nothing in the queueHandler.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (selfVoiceState.inAudioChannel()) {
            if (selfVoiceState.getChannel().getMembers().size() > 2) {
                if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getMember(), Permission.ROBERTIFY_DJ)) {
                    EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "You need to be a DJ to use this command when there's other users in the channel!");
                    msg.replyEmbeds(eb.build()).queue();
                    return;
                }
            }
        } else {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "The bot isn't in a voice channel.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        queueHandler.clear();
        new LogUtils(guild).sendLog(LogType.QUEUE_CLEAR, ctx.getAuthor().getAsMention() + " has cleared the queueHandler");

        if (new RequestChannelConfig(guild).isChannelSet())
            new RequestChannelConfig(guild).updateMessage();

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "The queueHandler was cleared!");
        msg.replyEmbeds(eb.build()).queue();
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+ GeneralUtils.listToString(getAliases())+"`\n\n"+
                "Clear all the queued songs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("c");
    }
}
