package main.commands.slashcommands.commands.audio;

import main.commands.prefixcommands.audio.RewindCommand;
import main.utils.component.interactions.AbstractSlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;

public class RewindSlashCommand extends AbstractSlashCommand {

    @Override
    protected void buildCommand() {
        setCommand(
                getBuilder()
                        .setName("rewind")
                        .setDescription("Rewind the song by the seconds provided or all the way to the beginning")
                        .addOptions(
                                CommandOption.of(
                                        OptionType.INTEGER,
                                        "seconds",
                                        "Seconds to rewind the song by",
                                        false
                                )
                        )
                        .setPossibleDJCommand()
                        .build()
        );
    }

    @Override
    public String getHelp() {
        return """
                Rewind the song

                Usage: `/rewind` *(Rewinds the song to the beginning)*

                Usage: `/rewind <seconds_to_rewind>` *(Rewinds the song by a specific duration)*
                """;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!checks(event)) return;
        sendRandomMessage(event);

        event.deferReply().queue();

        long time = -1;

        if (!event.getOptions().isEmpty()) {
            time = event.getOption("seconds").getAsLong();
        }

        event.getHook().sendMessageEmbeds(new RewindCommand().handleRewind(event.getUser(), event.getGuild().getSelfMember().getVoiceState(), time, event.getOptions().isEmpty()).build())
                .queue();
    }
}
