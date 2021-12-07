package main.commands.commands.audio;

import main.audiohandlers.GuildMusicManager;
import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import main.utils.json.dedicatedchannel.DedicatedChannelConfig;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import javax.script.ScriptException;
import java.util.List;

public class DisconnectCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Member self = ctx.getSelfMember();
        final GuildVoiceState selfState = self.getVoiceState();
        final Message msg = ctx.getMessage();

        EmbedBuilder eb;

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

        msg.replyEmbeds(handleDisconnect(ctx.getGuild(), ctx.getAuthor()).build())
                .queue();
    }

    public EmbedBuilder handleDisconnect(Guild guild, User author) {
//        if (!GeneralUtils.hasPerms(guild, author, Permission.ROBERTIFY_DJ))
//            return EmbedUtils.embedMessage("You need to be a DJ to use this command!");

        GuildMusicManager musicManager = RobertifyAudioManager.getInstance().getMusicManager(guild);
        musicManager.scheduler.queue.clear();
        musicManager.scheduler.player.stopTrack();

        guild.getAudioManager().closeAudioConnection();

        if (new DedicatedChannelConfig().isChannelSet(guild.getId()))
            new DedicatedChannelConfig().updateMessage(guild);

        musicManager.scheduler.repeating = false;
        musicManager.scheduler.playlistRepeating = false;
        musicManager.scheduler.getPastQueue().clear();

        return EmbedUtils.embedMessage("Disconnected!");
    }

    @Override
    public String getName() {
        return "disconnect";
    }

    @Override
    public String getHelp(String guildID) {
        return "Aliases: `"+getAliases().toString().replaceAll("[\\[\\]]", "")+"`\n" +
                "Forces the bot to stop playing music and leave the voice channel" +
                " if already in one.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("dc", "leave");
    }
}
