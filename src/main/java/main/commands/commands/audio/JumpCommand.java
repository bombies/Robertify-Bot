package main.commands.commands.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.PlayerManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.GeneralUtils;
import main.utils.database.BotUtils;
import main.utils.database.ServerUtils;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

import javax.script.ScriptException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class JumpCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member self = ctx.getSelfMember();
        GuildVoiceState selfVoiceState = self.getVoiceState();

        EmbedBuilder eb;

        BotUtils botUtils = new BotUtils();
        if (!botUtils.isAnnouncementChannelSet(ctx.getGuild().getIdLong())) {
            System.out.println("there is no announcement channel set");
            botUtils.createConnection();
            botUtils.setAnnouncementChannel(ctx.getGuild().getIdLong(), ctx.getChannel().getIdLong())
                    .closeConnection();

            eb = EmbedUtils.embedMessage("There was no announcement channel set! Setting to to this channel.\n" +
                    "\n_You can change the announcement channel by using set \"setchannel\" command._");
            ctx.getChannel().sendMessageEmbeds(eb.build()).queue();
        }

        final Member member = ctx.getMember();
        final GuildVoiceState memberVoiceState = member.getVoiceState();

        if (!selfVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.inVoiceChannel()) {
            eb = EmbedUtils.embedMessage("You need to be in a voice channel for this to work");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (!memberVoiceState.getChannel().equals(selfVoiceState.getChannel())) {
            eb = EmbedUtils.embedMessage("You must be in the same voice channel as me to use this command");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(ctx.getGuild());
        AudioPlayer audioPlayer = musicManager.audioPlayer;
        AudioTrack track = audioPlayer.getPlayingTrack();

        if (track == null) {
            eb = EmbedUtils.embedMessage("There is nothing playing!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        }

        if (ctx.getArgs().isEmpty()) {
            eb  = EmbedUtils.embedMessage("You must provide the amount of seconds to jump in the song!");
            msg.replyEmbeds(eb.build()).queue();
            return;
        } else {
            long time;
            if (GeneralUtils.stringIsInt(ctx.getArgs().get(0)))
                time = Long.parseLong(ctx.getArgs().get(0));
            else {
                eb = EmbedUtils.embedMessage("You must provide a valid duration to rewind");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            if (time <= 0) {
                eb = EmbedUtils.embedMessage("The duration cannot be negative or zero!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            time = TimeUnit.SECONDS.toMillis(time);

            if (time > track.getDuration() - time) {
                eb = EmbedUtils.embedMessage("This duration cannot be more than the time left!");
                msg.replyEmbeds(eb.build()).queue();
                return;
            }

            track.setPosition(track.getPosition() + time);
        }

        msg.addReaction("✅").queue();
    }

    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Skips the song by the given number of seconds\n" +
                "\nUsage: `"+ ServerUtils.getPrefix(Long.parseLong(guildID))+"jump <seconds_to_jump>` *(Skips the song to a specific duration)*\n";
    }

    @Override
    public List<String> getAliases() {
        return List.of("j", "ff", "fastforward");
    }
}
