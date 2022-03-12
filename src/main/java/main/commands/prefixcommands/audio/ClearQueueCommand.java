package main.commands.prefixcommands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.ICommand;
import main.constants.Permission;
import main.utils.GeneralUtils;
import main.utils.RobertifyEmbedUtils;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
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
        final var queue = musicManager.getScheduler().queue;
        final Message msg = ctx.getMessage();
        final Guild guild = ctx.getGuild();
        final GuildVoiceState selfVoiceState = ctx.getGuild().getSelfMember().getVoiceState();

        GeneralUtils.setCustomEmbed(guild, "Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "There is already nothing in the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel()) {
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

        queue.clear();
        new LogUtils().sendLog(guild, LogType.QUEUE_CLEAR, ctx.getAuthor().getAsMention() + " has cleared the queue");

        if (new DedicatedChannelConfig().isChannelSet(guild.getIdLong()))
            new DedicatedChannelConfig().updateMessage(guild);

        EmbedBuilder eb = RobertifyEmbedUtils.embedMessage(guild, "The queue was cleared!");
        msg.replyEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed(guild);
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+GeneralUtils.listToString(getAliases())+"`\n\n"+
                "Clear all the queued songs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("c");
    }
}
