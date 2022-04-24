package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.NowPlayingCommand;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class NowPlayingSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("nowplaying")
                        .setDescription("See the song that is currently being played")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Displays the song that is currently playing";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        event.getHook().sendMessageEmbeds(new NowPlayingCommand().getNowPlayingEmbed(event.getGuild(), event.getTextChannel(), selfVoiceState, memberVoiceState).build())
                .setEphemeral(false)
                .queue();
    }
}
