package main.commands.commands.audio;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.CommandContext;
import main.commands.ICommand;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.entities.*;

import javax.script.ScriptException;

public class JoinCommand implements ICommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        final Message msg = ctx.getMessage();
        final Member member = ctx.getMember();
        final Guild guild = ctx.getGuild();
        final GuildVoiceState memberVoiceState = member.getVoiceState();
        final GuildVoiceState selfVoiceState = guild.getSelfMember().getVoiceState();

        msg.replyEmbeds(handleJoin(guild, ctx.getChannel(), memberVoiceState, selfVoiceState))
                .queue();
    }

    public MessageEmbed handleJoin(Guild guild, TextChannel textChannel, GuildVoiceState memberVoiceState, GuildVoiceState selfVoiceState) {
        if (!memberVoiceState.inVoiceChannel()) {
            return EmbedUtils.embedMessage("You must be in a voice channel to use this command")
                    .build();
        }

        VoiceChannel channel = memberVoiceState.getChannel();
        if (selfVoiceState.inVoiceChannel()) {
            guild.moveVoiceMember(selfVoiceState.getMember(), channel)
                    .queue();
            return EmbedUtils.embedMessage("I have moved to " + channel.getAsMention()).build();
        } else {
            RobertifyAudioManager.getInstance()
                    .joinVoiceChannel(textChannel, selfVoiceState, memberVoiceState);
            return EmbedUtils.embedMessage("I have joined " + channel.getAsMention()).build();
        }
    }

    @Override
    public String getName() {
        return "join";
    }

    @Override
    public String getHelp(String guildID) {
        return "Use this command to forcefully move the bot into your voice channel.\n\n" +
                "*NOTE: This command can be made DJ only by using* `toggles dj join`";
    }
}
