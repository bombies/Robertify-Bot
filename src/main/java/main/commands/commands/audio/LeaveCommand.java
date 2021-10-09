package main.commands.commands.audio;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.commands.commands.management.permissions.Permission;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;

public class LeaveCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfState = self.getVoiceState();
        final Message msg = ctx.getMessage();

        EmbedBuilder eb;

        if (!GeneralUtils.hasPerms(ctx.getGuild(), ctx.getAuthor(), Permission.ROBERTIFY_DJ)) {
            eb  = EmbedUtils.embedMessage("You need to be a DJ to use this command!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }



        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting it to this channel.\n" +
                    "\n_You can change the announcement channel by using the \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        if (!selfState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("I'm already not in a voice channel!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in the same voice channel as me for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        musicManager.scheduler.queue.clear();
        musicManager.scheduler.player.stopTrack();

        ctx.getGuild().getAudioManager().closeAudioConnection();
        msg.addReaction("✅").queue();
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Permission Required: `"+Permission.ROBERTIFY_DJ+"`\n\n"+
                "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("dc");
    }
}
