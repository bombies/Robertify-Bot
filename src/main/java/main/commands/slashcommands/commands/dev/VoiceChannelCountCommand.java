package main.commands.slashcommands.commands.dev;

import main.audiohandlers.RobertifyAudioManager;
import main.commands.prefixcommands.CommandContext;
import main.commands.prefixcommands.IDevCommand;
import main.main.Robertify;
import main.utils.RobertifyEmbedUtils;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import java.util.List;

public class VoiceChannelCountCommand extends AbstractSlashCommand implements IDevCommand {
    @Override
    public void handle(CommandContext ctx) throws ScriptException {
        if (!permissionCheck(ctx)) return;

        int vcCount = 0;
        int currentlyPlayingCount = 0;
        int peopleListening = 0;
        for (var guild : Robertify.api.getGuilds()) {
            vcCount += guild.getSelfMember().getVoiceState().inVoiceChannel() ? 1 : 0;
            currentlyPlayingCount += (RobertifyAudioManager.getInstance().getMusicManager(guild)).getPlayer().getPlayingTrack() != null ? 1 : 0;

            if (guild.getSelfMember().getVoiceState().inVoiceChannel())
                peopleListening += guild.getSelfMember().getVoiceState().getChannel().getMembers().size();
        }

        ctx.getMessage().replyEmbeds(RobertifyEmbedUtils.embedMessage(ctx.getGuild(), "🔊 I am currently in **" + vcCount + "** voice channels\n" +
                        "I am currently playing music in **"+currentlyPlayingCount+"** of those channels.\n" +
                        "There is currently **"+peopleListening+"** people listening to music").build())
                .queue();
    }

    @Override
    public String getName() {
        return "voicechannelcount";
    }

    @Override
    public String getHelp(String prefix) {
        return null;
    }

    @Override
    public List<String> getAliases() {
        return List.of("vccount", "vcc");
    }

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("voicechannelcount")
                        .setDescription("Count all the voice channels Robertify is currenly playing music in!")
                        .setDevCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!nameCheck(event)) return;

        int vcCount = 0;
        int currentlyPlayingCount = 0;
        int peopleListening = 0;
        for (var guild : Robertify.api.getGuilds()) {
            vcCount += guild.getSelfMember().getVoiceState().inVoiceChannel() ? 1 : 0;
            currentlyPlayingCount += (RobertifyAudioManager.getInstance().getMusicManager(guild)).getPlayer().getPlayingTrack() != null ? 1 : 0;

            if (guild.getSelfMember().getVoiceState().inVoiceChannel())
                peopleListening += guild.getSelfMember().getVoiceState().getChannel().getMembers().size();
        }

        event.replyEmbeds(RobertifyEmbedUtils.embedMessage(event.getGuild(), "🔊 I am currently in **" + vcCount + "** voice channels\n" +
                        "I am currently playing music in **"+currentlyPlayingCount+"** of those channels.\n" +
                        "There is currently **"+peopleListening+"** people listening to music").build())
                .setEphemeral(true)
                .queue();
    }
}
