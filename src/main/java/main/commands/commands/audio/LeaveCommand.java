package main.commands.commands.audio;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.database.BotUtils;
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

        EmbedBuilder eb;

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting to to this channel.\n" +
                    "\n_You can change the announcement channel by using set \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        if (!selfState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("I'm already not in a voice channel!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
//        musicManager.scheduler.repeating = false;
        musicManager.scheduler.queue.clear();
        musicManager.audioPlayer.stopTrack();

        ctx.getGuild().getAudioManager().closeAudioConnection();
        msg.addReaction("✅").queue();
    }

    @Override
    public String getName() {
        return "leave";
    }

    @Override
    public String getHelp(String guildID) {
        return "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one.";
    }
}
