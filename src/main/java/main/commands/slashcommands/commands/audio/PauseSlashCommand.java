package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.PauseCommand;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.jetbrains.annotations.NotNull;

public class PauseSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("pause")
                        .setDescription("Pause the song being currently played!")
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return "Pauses the song currently playing";
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        final GuildVoiceState memberVoiceState = event.getMember().getVoiceState();
        final GuildVoiceState selfVoiceState = event.getGuild().getSelfMember().getVoiceState();

        event.getHook().sendMessageEmbeds(new PauseCommand().handlePauseEvent(event.getGuild(), selfVoiceState, memberVoiceState).build())
                .setEphemeral(false).queue();
     }
}
