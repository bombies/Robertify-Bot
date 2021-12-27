package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.utils.GeneralUtils;
import main.utils.database.sqlite3.BotDB;
import main.utils.json.guildconfig.GuildConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClearQueueCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(ctx.getGuild());
        final ConcurrentLinkedQueue<AudioTrack> queue = musicManager.scheduler.queue;
        final Message msg = ctx.getMessage();
        final GuildVoiceState selfVoiceState = ctx.getGuild().getSelfMember().getVoiceState();

        GeneralUtils.setCustomEmbed("Queue");

        if (queue.isEmpty()) {
            EmbedBuilder eb = EmbedUtils.embedMessage("There is already nothing in the queue.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (selfVoiceState.inVoiceChannel()) {
            if (selfVoiceState.getChannel().getMembers().size() > 2) {
                if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_DJ)) {
                    EmbedBuilder eb = EmbedUtils.embedMessage("You need to be a DJ to use this command when there's other users in the channel!");
                    msg.replyEmbeds(eb.build()).queue();
                    return;
                }
            }
        } else {
            EmbedBuilder eb = EmbedUtils.embedMessage("The bot isn't in a voice channel.");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        queue.clear();

        EmbedBuilder eb = EmbedUtils.embedMessage("The queue was cleared!");
        msg.replyEmbeds(eb.build()).queue();

        GeneralUtils.setDefaultEmbed();
    }

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public String getHelp(String prefix) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Permission Required: `"+Permission.ROBERTIFY_DJ+"`\n\n"+
                "Clear all the queued songs";
    }

    @Override
    public List<String> getAliases() {
        return List.of("c");
    }
}
